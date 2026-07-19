package dev.luan.sicredi.voting.acceptance;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import dev.luan.sicredi.voting.bootstrap.VotingApplication;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Testcontainers
@ExtendWith(OutputCaptureExtension.class)
@SpringBootTest(
    classes = VotingApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
      "app.voting.closure-poll-delay=PT0.1S",
      "app.messaging.relay-poll-delay=PT0.1S",
      "app.messaging.confirm-timeout=PT3S",
      "app.messaging.claim-lease=PT10S"
    })
class VotingApiIntegrationTest {

  private static final String ELIGIBLE_CPF = "93541134780";
  private static final String INELIGIBLE_CPF = "11144477735";
  private static final String RESULT_QUEUE = "voting.results.platform";

  private static final WireMockServer ELIGIBILITY_PROVIDER =
      new WireMockServer(WireMockConfiguration.options().dynamicPort());

  static {
    ELIGIBILITY_PROVIDER.start();
  }

  @Container
  static final PostgreSQLContainer POSTGRES =
      new PostgreSQLContainer(DockerImageName.parse("postgres:17-alpine"));

  @Container
  static final RabbitMQContainer RABBITMQ =
      new RabbitMQContainer(DockerImageName.parse("rabbitmq:4.1-management-alpine"));

  @DynamicPropertySource
  static void configureDependencies(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
    registry.add(
        "spring.rabbitmq.addresses",
        () -> "amqp://guest:guest@" + RABBITMQ.getHost() + ":" + RABBITMQ.getAmqpPort());
    registry.add("app.eligibility.base-url", ELIGIBILITY_PROVIDER::baseUrl);
  }

  @LocalServerPort private int serverPort;

  @Autowired private ObjectMapper objectMapper;

  @Autowired private RabbitTemplate rabbitTemplate;

  private HttpClient httpClient;

  @BeforeEach
  void configureClientAndEligibilityProvider() {
    httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    ELIGIBILITY_PROVIDER.resetAll();
    ELIGIBILITY_PROVIDER.stubFor(
        get(urlEqualTo("/users/" + ELIGIBLE_CPF))
            .willReturn(okJson("{\"status\":\"ABLE_TO_VOTE\"}")));
    ELIGIBILITY_PROVIDER.stubFor(
        get(urlEqualTo("/users/" + INELIGIBLE_CPF))
            .willReturn(okJson("{\"status\":\"UNABLE_TO_VOTE\"}")));
  }

  @AfterAll
  static void stopEligibilityProvider() {
    ELIGIBILITY_PROVIDER.stop();
  }

  @Test
  void shouldCompleteTheVotingLifecycleAndPublishItsResult(CapturedOutput output) throws Exception {
    UUID agendaId = createAgenda("Annual budget");
    HttpResponse<String> sessionResponse = openSession(agendaId, 5);
    assertThat(sessionResponse.statusCode()).isEqualTo(201);

    HttpResponse<String> voteResponse = castVote(agendaId, ELIGIBLE_CPF, "YES");
    assertThat(voteResponse.statusCode()).isEqualTo(201);

    HttpResponse<String> duplicateResponse = castVote(agendaId, ELIGIBLE_CPF, "NO");
    assertThat(duplicateResponse.statusCode()).isEqualTo(409);
    assertThat(errorCode(duplicateResponse)).isEqualTo("ASSOCIATE_ALREADY_VOTED");

    HttpResponse<String> ineligibleResponse = castVote(agendaId, INELIGIBLE_CPF, "NO");
    assertThat(ineligibleResponse.statusCode()).isEqualTo(422);
    assertThat(errorCode(ineligibleResponse)).isEqualTo("ASSOCIATE_NOT_ELIGIBLE");

    HttpResponse<String> resultResponse =
        await()
            .atMost(Duration.ofSeconds(15))
            .pollInterval(Duration.ofMillis(200))
            .until(
                () -> getApi("/api/v1/agendas/" + agendaId + "/result"),
                response -> response.statusCode() == 200);
    JsonNode result = objectMapper.readTree(resultResponse.body());
    assertThat(result.get("yesVotes").asLong()).isOne();
    assertThat(result.get("noVotes").asLong()).isZero();
    assertThat(result.get("totalVotes").asLong()).isOne();
    assertThat(result.get("outcome").asText()).isEqualTo("APPROVED");

    Message publishedMessage =
        await()
            .atMost(Duration.ofSeconds(10))
            .pollInterval(Duration.ofMillis(200))
            .until(() -> rabbitTemplate.receive(RESULT_QUEUE, 100), java.util.Objects::nonNull);
    String eventPayload = new String(publishedMessage.getBody(), StandardCharsets.UTF_8);
    JsonNode event = objectMapper.readTree(eventPayload);
    assertThat(event.get("agendaId").asText()).isEqualTo(agendaId.toString());
    assertThat(event.get("outcome").asText()).isEqualTo("APPROVED");
    assertThat(eventPayload).doesNotContain(ELIGIBLE_CPF, INELIGIBLE_CPF);
    assertThat(publishedMessage.getMessageProperties().getMessageId())
        .isEqualTo(event.get("eventId").asText());
    assertThat(publishedMessage.getMessageProperties().getCorrelationId())
        .isEqualTo(agendaId.toString());
    assertThat(output.getAll()).doesNotContain(ELIGIBLE_CPF, INELIGIBLE_CPF);
  }

  @Test
  void shouldAcceptExactlyOneOfManyConcurrentDuplicateVotes() throws Exception {
    UUID agendaId = createAgenda("Concurrent voting");
    assertThat(openSession(agendaId, 86_400).statusCode()).isEqualTo(201);

    int contenders = 20;
    CountDownLatch ready = new CountDownLatch(contenders);
    CountDownLatch start = new CountDownLatch(1);
    List<CompletableFuture<Integer>> responses = new ArrayList<>(contenders);

    try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
      for (int index = 0; index < contenders; index++) {
        responses.add(
            CompletableFuture.supplyAsync(
                () -> {
                  ready.countDown();
                  awaitStart(start);
                  try {
                    return castVote(agendaId, ELIGIBLE_CPF, "YES").statusCode();
                  } catch (Exception exception) {
                    throw new IllegalStateException(exception);
                  }
                },
                executor));
      }
      assertThat(ready.await(5, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
      start.countDown();
      CompletableFuture.allOf(responses.toArray(CompletableFuture[]::new)).get();
    }

    List<Integer> statuses = responses.stream().map(CompletableFuture::join).toList();
    assertThat(statuses).filteredOn(status -> status == 201).hasSize(1);
    assertThat(statuses).filteredOn(status -> status == 409).hasSize(19);
  }

  private UUID createAgenda(String title) throws Exception {
    HttpResponse<String> response =
        post(
            "/api/v1/agendas",
            "{\"title\":\"" + title + "\",\"description\":\"Integration test\"}");
    assertThat(response.statusCode()).isEqualTo(201);
    return UUID.fromString(objectMapper.readTree(response.body()).get("id").asText());
  }

  private HttpResponse<String> openSession(UUID agendaId, long durationSeconds) throws Exception {
    return post(
        "/api/v1/agendas/" + agendaId + "/voting-sessions",
        "{\"durationSeconds\":" + durationSeconds + "}");
  }

  private HttpResponse<String> castVote(UUID agendaId, String associateCpf, String choice)
      throws Exception {
    return post(
        "/api/v1/agendas/" + agendaId + "/votes",
        "{\"associateCpf\":\"" + associateCpf + "\",\"choice\":\"" + choice + "\"}");
  }

  private String errorCode(HttpResponse<String> response) throws Exception {
    return objectMapper.readTree(response.body()).get("code").asText();
  }

  private HttpResponse<String> post(String path, String json) throws Exception {
    HttpRequest request =
        HttpRequest.newBuilder(uri(path))
            .timeout(Duration.ofSeconds(20))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .build();
    return httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
  }

  private HttpResponse<String> getApi(String path) throws Exception {
    HttpRequest request =
        HttpRequest.newBuilder(uri(path)).timeout(Duration.ofSeconds(10)).GET().build();
    return httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
  }

  private URI uri(String path) {
    return URI.create("http://127.0.0.1:" + serverPort + path);
  }

  private static void awaitStart(CountDownLatch start) {
    try {
      start.await();
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(exception);
    }
  }
}
