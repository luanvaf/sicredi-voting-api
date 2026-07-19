package dev.luan.sicredi.voting.integration.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import dev.luan.sicredi.voting.domainmodel.AgendaId;
import dev.luan.sicredi.voting.domainmodel.VoteTally;
import dev.luan.sicredi.voting.domainmodel.VotingOutcome;
import dev.luan.sicredi.voting.domainmodel.VotingResultFinalizedEvent;
import dev.luan.sicredi.voting.domainmodel.VotingSessionId;
import dev.luan.sicredi.voting.service.port.out.VotingResultOutbox.ClaimedResultEvent;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.ObjectMapper;

@Testcontainers
class PostgresVotingResultOutboxIntegrationTest {

  private static final Instant OCCURRED_AT = Instant.parse("2026-07-19T12:00:00Z");

  @Container
  private static final PostgreSQLContainer POSTGRES =
      new PostgreSQLContainer(DockerImageName.parse("postgres:17-alpine"));

  private JdbcTemplate jdbcTemplate;
  private PostgresVotingResultOutbox outbox;

  @BeforeEach
  void setUp() {
    DriverManagerDataSource dataSource =
        new DriverManagerDataSource(
            POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    jdbcTemplate = new JdbcTemplate(dataSource);
    jdbcTemplate.execute("DROP TABLE IF EXISTS voting_result_outbox");
    jdbcTemplate.execute(
        """
        CREATE TABLE voting_result_outbox (
            event_id UUID PRIMARY KEY,
            voting_session_id UUID NOT NULL,
            agenda_id UUID NOT NULL,
            event_type VARCHAR(100) NOT NULL,
            payload JSONB NOT NULL,
            occurred_at TIMESTAMPTZ NOT NULL,
            attempts INTEGER NOT NULL DEFAULT 0,
            next_attempt_at TIMESTAMPTZ NOT NULL,
            published_at TIMESTAMPTZ,
            last_error_code VARCHAR(100)
        )
        """);
    outbox = new PostgresVotingResultOutbox(jdbcTemplate, new ObjectMapper());
  }

  @Test
  void shouldClaimWithALeaseAndMarkTheConfirmedEventAsPublished() {
    VotingResultFinalizedEvent event = event();
    outbox.append(event);

    List<ClaimedResultEvent> claimedEvents =
        outbox.claimPending(OCCURRED_AT, 10, Duration.ofSeconds(30));

    assertThat(claimedEvents).hasSize(1);
    ClaimedResultEvent claimedEvent = claimedEvents.getFirst();
    assertThat(claimedEvent.eventId()).isEqualTo(event.eventId());
    assertThat(claimedEvent.agendaId()).isEqualTo(event.agendaId().value());
    assertThat(claimedEvent.attemptNumber()).isOne();
    assertThat(claimedEvent.leaseUntil()).isEqualTo(OCCURRED_AT.plusSeconds(30));
    assertThat(claimedEvent.payload())
        .contains("\"outcome\": \"APPROVED\"")
        .doesNotContainIgnoringCase("cpf");
    assertThat(outbox.claimPending(OCCURRED_AT, 10, Duration.ofSeconds(30))).isEmpty();

    Instant renewedLease = OCCURRED_AT.plusSeconds(45);
    assertThat(outbox.renewLease(claimedEvent, renewedLease)).isTrue();
    ClaimedResultEvent renewedEvent = claimedEvent.withLeaseUntil(renewedLease);
    assertThat(outbox.markPublished(renewedEvent, OCCURRED_AT.plusSeconds(1))).isTrue();
    assertThat(outbox.claimPending(OCCURRED_AT.plusSeconds(60), 10, Duration.ofSeconds(30)))
        .isEmpty();
  }

  @Test
  void shouldFenceAStaleWorkerAfterTheLeaseExpires() {
    VotingResultFinalizedEvent event = event();
    outbox.append(event);
    ClaimedResultEvent firstClaim =
        outbox.claimPending(OCCURRED_AT, 1, Duration.ofSeconds(5)).getFirst();

    ClaimedResultEvent secondClaim =
        outbox.claimPending(OCCURRED_AT.plusSeconds(6), 1, Duration.ofSeconds(5)).getFirst();

    assertThat(secondClaim.attemptNumber()).isEqualTo(2);
    assertThat(outbox.markPublished(firstClaim, OCCURRED_AT.plusSeconds(7))).isFalse();
    assertThat(
            outbox.scheduleRetry(firstClaim, OCCURRED_AT.plusSeconds(20), "BROKER_CONFIRM_TIMEOUT"))
        .isFalse();
    assertThat(outbox.scheduleRetry(secondClaim, OCCURRED_AT.plusSeconds(20), "BROKER_NACK"))
        .isTrue();
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT last_error_code FROM voting_result_outbox WHERE event_id = ?",
                String.class,
                event.eventId()))
        .isEqualTo("BROKER_NACK");
  }

  private static VotingResultFinalizedEvent event() {
    return new VotingResultFinalizedEvent(
        UUID.randomUUID(),
        VotingSessionId.newId(),
        AgendaId.newId(),
        new VoteTally(7, 4),
        VotingOutcome.APPROVED,
        OCCURRED_AT.minusSeconds(60),
        OCCURRED_AT,
        OCCURRED_AT);
  }
}
