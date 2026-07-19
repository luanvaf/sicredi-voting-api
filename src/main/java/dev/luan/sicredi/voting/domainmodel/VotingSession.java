package dev.luan.sicredi.voting.domainmodel;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

public record VotingSession(
    VotingSessionId id,
    AgendaId agendaId,
    Instant openedAt,
    Instant closesAt,
    Instant finalizedAt) {

  public VotingSession {
    Objects.requireNonNull(id, "Voting session ID must not be null");
    Objects.requireNonNull(agendaId, "Agenda ID must not be null");
    Objects.requireNonNull(openedAt, "Voting session opening timestamp must not be null");
    Objects.requireNonNull(closesAt, "Voting session closing timestamp must not be null");

    if (!closesAt.isAfter(openedAt)) {
      throw new IllegalArgumentException(
          "Voting session closing timestamp must be after its opening timestamp");
    }
    if (finalizedAt != null && finalizedAt.isBefore(closesAt)) {
      throw new IllegalArgumentException(
          "Voting session finalization timestamp must not be before its closing timestamp");
    }
  }

  public static VotingSession open(AgendaId agendaId, Instant openedAt, Duration duration) {
    Objects.requireNonNull(duration, "Voting session duration must not be null");
    if (duration.isZero() || duration.isNegative()) {
      throw new IllegalArgumentException("Voting session duration must be positive");
    }
    return new VotingSession(
        VotingSessionId.newId(), agendaId, openedAt, openedAt.plus(duration), null);
  }

  public boolean isOpenAt(Instant instant) {
    Objects.requireNonNull(instant, "Evaluation timestamp must not be null");
    return finalizedAt == null && !instant.isBefore(openedAt) && instant.isBefore(closesAt);
  }

  public boolean isExpiredAt(Instant instant) {
    Objects.requireNonNull(instant, "Evaluation timestamp must not be null");
    return !instant.isBefore(closesAt);
  }

  public boolean isFinalized() {
    return finalizedAt != null;
  }

  public VotingSession finalizeAt(Instant instant) {
    Objects.requireNonNull(instant, "Finalization timestamp must not be null");
    if (instant.isBefore(closesAt)) {
      throw new IllegalArgumentException(
          "Voting session cannot be finalized before its closing timestamp");
    }
    if (finalizedAt != null) {
      return this;
    }
    return new VotingSession(id, agendaId, openedAt, closesAt, instant);
  }
}
