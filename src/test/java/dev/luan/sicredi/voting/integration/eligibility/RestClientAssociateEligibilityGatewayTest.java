package dev.luan.sicredi.voting.integration.eligibility;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import dev.luan.sicredi.voting.domainmodel.AssociateCpf;
import dev.luan.sicredi.voting.domainmodel.AssociateEligibility;
import dev.luan.sicredi.voting.service.error.AssociateNotFoundException;
import dev.luan.sicredi.voting.service.error.EligibilityServiceUnavailableException;
import java.net.http.HttpClient;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

class RestClientAssociateEligibilityGatewayTest {

  private static final String CPF = "19839091069";
  private static final AssociateCpf ASSOCIATE_CPF = new AssociateCpf(CPF);

  @RegisterExtension
  static final WireMockExtension WIREMOCK = WireMockExtension.newInstance().build();

  @ParameterizedTest
  @CsvSource({
    "ABLE_TO_VOTE, ELIGIBLE",
    "'ABLE TO VOTE', ELIGIBLE",
    "UNABLE_TO_VOTE, INELIGIBLE",
    "'UNABLE TO VOTE', INELIGIBLE"
  })
  void shouldMapProviderStatuses(String providerStatus, AssociateEligibility expectedEligibility) {
    WIREMOCK.stubFor(
        get(urlEqualTo("/users/" + CPF))
            .willReturn(okJson("{\"status\":\"" + providerStatus + "\"}")));

    AssociateEligibility eligibility = gateway(WIREMOCK.baseUrl()).check(ASSOCIATE_CPF);

    assertThat(eligibility).isEqualTo(expectedEligibility);
    WIREMOCK.verify(getRequestedFor(urlEqualTo("/users/" + CPF)));
  }

  @Test
  void shouldTranslateNotFoundResponse() {
    WIREMOCK.stubFor(
        get(urlEqualTo("/users/" + CPF))
            .willReturn(
                aResponse()
                    .withStatus(404)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"error\":\"Associate not found\"}")));

    assertThatThrownBy(() -> gateway(WIREMOCK.baseUrl()).check(ASSOCIATE_CPF))
        .isInstanceOf(AssociateNotFoundException.class)
        .hasMessageContaining(ASSOCIATE_CPF.masked())
        .hasMessageNotContaining(CPF);
  }

  @Test
  void shouldTranslateServerErrors() {
    WIREMOCK.stubFor(get(urlEqualTo("/users/" + CPF)).willReturn(serverError()));

    assertUnavailable(() -> gateway(WIREMOCK.baseUrl()).check(ASSOCIATE_CPF));
  }

  @ParameterizedTest
  @ValueSource(strings = {"{", "{}", "{\"status\":\"UNKNOWN\"}"})
  void shouldTranslateMalformedProviderResponses(String responseBody) {
    WIREMOCK.stubFor(
        get(urlEqualTo("/users/" + CPF))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(responseBody)));

    assertUnavailable(() -> gateway(WIREMOCK.baseUrl()).check(ASSOCIATE_CPF));
  }

  @Test
  void shouldTranslateReadTimeouts() {
    WIREMOCK.stubFor(
        get(urlEqualTo("/users/" + CPF))
            .willReturn(okJson("{\"status\":\"ABLE_TO_VOTE\"}").withFixedDelay(250)));

    assertUnavailable(
        () -> gateway(WIREMOCK.baseUrl(), Duration.ofMillis(50)).check(ASSOCIATE_CPF));
  }

  @Test
  void shouldTranslateConnectionErrors() {
    assertUnavailable(() -> gateway("http://127.0.0.1:1").check(ASSOCIATE_CPF));
  }

  private static RestClientAssociateEligibilityGateway gateway(String baseUrl) {
    return gateway(baseUrl, Duration.ofSeconds(1));
  }

  private static RestClientAssociateEligibilityGateway gateway(
      String baseUrl, Duration readTimeout) {
    HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(200)).build();
    JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
    requestFactory.setReadTimeout(readTimeout);

    RestClient restClient =
        RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory).build();
    return new RestClientAssociateEligibilityGateway(restClient);
  }

  private static void assertUnavailable(Runnable invocation) {
    assertThatThrownBy(invocation::run)
        .isInstanceOf(EligibilityServiceUnavailableException.class)
        .hasMessage("The associate eligibility provider is unavailable");
  }
}
