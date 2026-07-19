package dev.luan.sicredi.voting.integration.messaging;

import dev.luan.sicredi.voting.domainmodel.VotingResultFinalizedEvent;
import dev.luan.sicredi.voting.service.error.PersistenceUnavailableException;
import dev.luan.sicredi.voting.service.port.out.VotingResultOutbox;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

public final class PostgresVotingResultOutbox implements VotingResultOutbox {

  static final String EVENT_TYPE = "voting-result.finalized.v1";

  private static final Pattern SAFE_ERROR_CODE = Pattern.compile("[A-Z][A-Z0-9_]{0,99}");

  private static final String APPEND_SQL =
      """
      INSERT INTO voting_result_outbox (
          event_id,
          voting_session_id,
          agenda_id,
          event_type,
          payload,
          occurred_at,
          next_attempt_at
      )
      VALUES (?, ?, ?, ?, ?::jsonb, ?, ?)
      """;

  private static final String CLAIM_SQL =
      """
      WITH candidates AS (
          SELECT event_id
          FROM voting_result_outbox
          WHERE published_at IS NULL
            AND next_attempt_at <= ?
          ORDER BY next_attempt_at, occurred_at, event_id
          LIMIT ?
          FOR UPDATE SKIP LOCKED
      )
      UPDATE voting_result_outbox AS outbox
      SET next_attempt_at = ?,
          attempts = outbox.attempts + 1
      FROM candidates
      WHERE outbox.event_id = candidates.event_id
      RETURNING
          outbox.event_id,
          outbox.agenda_id,
          outbox.event_type,
          outbox.payload::text AS payload,
          outbox.attempts,
          outbox.next_attempt_at
      """;

  private static final String RENEW_LEASE_SQL =
      """
      UPDATE voting_result_outbox
      SET next_attempt_at = ?
      WHERE event_id = ?
        AND published_at IS NULL
        AND next_attempt_at = ?
      """;

  private static final String MARK_PUBLISHED_SQL =
      """
      UPDATE voting_result_outbox
      SET published_at = ?,
          last_error_code = NULL
      WHERE event_id = ?
        AND published_at IS NULL
        AND next_attempt_at = ?
      """;

  private static final String SCHEDULE_RETRY_SQL =
      """
      UPDATE voting_result_outbox
      SET next_attempt_at = ?,
          last_error_code = ?
      WHERE event_id = ?
        AND published_at IS NULL
        AND next_attempt_at = ?
      """;

  private final JdbcTemplate jdbcTemplate;
  private final ObjectMapper objectMapper;

  public PostgresVotingResultOutbox(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
    this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "JDBC template must not be null");
    this.objectMapper = Objects.requireNonNull(objectMapper, "Object mapper must not be null");
  }

  @Override
  public void append(VotingResultFinalizedEvent event) {
    Objects.requireNonNull(event, "Voting result event must not be null");
    try {
      jdbcTemplate.update(
          APPEND_SQL,
          event.eventId(),
          event.votingSessionId().value(),
          event.agendaId().value(),
          EVENT_TYPE,
          serialize(event),
          Timestamp.from(event.occurredAt()),
          Timestamp.from(event.occurredAt()));
    } catch (DataAccessException exception) {
      throw new PersistenceUnavailableException(exception);
    }
  }

  @Override
  public List<ClaimedResultEvent> claimPending(
      Instant claimedAt, int batchSize, Duration leaseDuration) {
    Objects.requireNonNull(claimedAt, "Claim timestamp must not be null");
    requirePositive(batchSize, "Outbox claim batch size");
    requirePositive(leaseDuration, "Outbox lease duration");
    Instant leaseUntil = claimedAt.plus(leaseDuration);

    try {
      return jdbcTemplate.query(
          CLAIM_SQL,
          (resultSet, rowNumber) ->
              new ClaimedResultEvent(
                  resultSet.getObject("event_id", java.util.UUID.class),
                  resultSet.getObject("agenda_id", java.util.UUID.class),
                  resultSet.getString("event_type"),
                  resultSet.getString("payload"),
                  resultSet.getInt("attempts"),
                  resultSet.getTimestamp("next_attempt_at").toInstant()),
          Timestamp.from(claimedAt),
          batchSize,
          Timestamp.from(leaseUntil));
    } catch (DataAccessException exception) {
      throw new PersistenceUnavailableException(exception);
    }
  }

  @Override
  public boolean renewLease(ClaimedResultEvent event, Instant newLeaseUntil) {
    Objects.requireNonNull(event, "Claimed result event must not be null");
    Objects.requireNonNull(newLeaseUntil, "New lease timestamp must not be null");
    try {
      return jdbcTemplate.update(
              RENEW_LEASE_SQL,
              Timestamp.from(newLeaseUntil),
              event.eventId(),
              Timestamp.from(event.leaseUntil()))
          == 1;
    } catch (DataAccessException exception) {
      throw new PersistenceUnavailableException(exception);
    }
  }

  @Override
  public boolean markPublished(ClaimedResultEvent event, Instant publishedAt) {
    Objects.requireNonNull(event, "Claimed result event must not be null");
    Objects.requireNonNull(publishedAt, "Publication timestamp must not be null");
    try {
      return jdbcTemplate.update(
              MARK_PUBLISHED_SQL,
              Timestamp.from(publishedAt),
              event.eventId(),
              Timestamp.from(event.leaseUntil()))
          == 1;
    } catch (DataAccessException exception) {
      throw new PersistenceUnavailableException(exception);
    }
  }

  @Override
  public boolean scheduleRetry(
      ClaimedResultEvent event, Instant nextAttemptAt, String safeErrorCode) {
    Objects.requireNonNull(event, "Claimed result event must not be null");
    Objects.requireNonNull(nextAttemptAt, "Next attempt timestamp must not be null");
    if (safeErrorCode == null || !SAFE_ERROR_CODE.matcher(safeErrorCode).matches()) {
      throw new IllegalArgumentException("Publication error code must use the safe code format");
    }
    try {
      return jdbcTemplate.update(
              SCHEDULE_RETRY_SQL,
              Timestamp.from(nextAttemptAt),
              safeErrorCode,
              event.eventId(),
              Timestamp.from(event.leaseUntil()))
          == 1;
    } catch (DataAccessException exception) {
      throw new PersistenceUnavailableException(exception);
    }
  }

  private String serialize(VotingResultFinalizedEvent event) {
    ObjectNode payload = objectMapper.createObjectNode();
    payload.put("eventId", event.eventId().toString());
    payload.put("eventType", EVENT_TYPE);
    payload.put("votingSessionId", event.votingSessionId().value().toString());
    payload.put("agendaId", event.agendaId().value().toString());
    payload.put("yesVotes", event.tally().yesVotes());
    payload.put("noVotes", event.tally().noVotes());
    payload.put("outcome", event.outcome().name());
    payload.put("closedAt", event.closedAt().toString());
    payload.put("finalizedAt", event.finalizedAt().toString());
    payload.put("occurredAt", event.occurredAt().toString());
    return payload.toString();
  }

  private static void requirePositive(int value, String fieldName) {
    if (value < 1) {
      throw new IllegalArgumentException(fieldName + " must be positive");
    }
  }

  private static void requirePositive(Duration value, String fieldName) {
    Objects.requireNonNull(value, fieldName + " must not be null");
    if (value.isZero() || value.isNegative()) {
      throw new IllegalArgumentException(fieldName + " must be positive");
    }
  }
}
