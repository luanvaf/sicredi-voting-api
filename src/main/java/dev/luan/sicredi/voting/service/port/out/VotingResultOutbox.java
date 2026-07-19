package dev.luan.sicredi.voting.service.port.out;

import dev.luan.sicredi.voting.domainmodel.VotingResultFinalizedEvent;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public interface VotingResultOutbox {

  void append(VotingResultFinalizedEvent event);

  List<ClaimedResultEvent> claimPending(Instant claimedAt, int batchSize, Duration leaseDuration);

  boolean renewLease(ClaimedResultEvent event, Instant newLeaseUntil);

  boolean markPublished(ClaimedResultEvent event, Instant publishedAt);

  boolean scheduleRetry(ClaimedResultEvent event, Instant nextAttemptAt, String safeErrorCode);

  record ClaimedResultEvent(
      UUID eventId,
      UUID agendaId,
      String eventType,
      String payload,
      int attemptNumber,
      Instant leaseUntil) {

    public ClaimedResultEvent {
      Objects.requireNonNull(eventId, "Event ID must not be null");
      Objects.requireNonNull(agendaId, "Agenda ID must not be null");
      eventType = requireText(eventType, "Event type");
      payload = requireText(payload, "Event payload");
      Objects.requireNonNull(leaseUntil, "Event lease timestamp must not be null");
      if (attemptNumber < 1) {
        throw new IllegalArgumentException("Event attempt number must be positive");
      }
    }

    public ClaimedResultEvent withLeaseUntil(Instant newLeaseUntil) {
      return new ClaimedResultEvent(
          eventId, agendaId, eventType, payload, attemptNumber, newLeaseUntil);
    }

    private static String requireText(String value, String fieldName) {
      Objects.requireNonNull(value, fieldName + " must not be null");
      if (value.isBlank()) {
        throw new IllegalArgumentException(fieldName + " must not be blank");
      }
      return value;
    }
  }
}
