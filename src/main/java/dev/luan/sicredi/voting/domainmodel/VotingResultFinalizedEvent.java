package dev.luan.sicredi.voting.domainmodel;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record VotingResultFinalizedEvent(
    UUID eventId,
    VotingSessionId votingSessionId,
    AgendaId agendaId,
    VoteTally tally,
    VotingOutcome outcome,
    Instant closedAt,
    Instant finalizedAt,
    Instant occurredAt) {

  public VotingResultFinalizedEvent {
    Objects.requireNonNull(eventId, "Event ID must not be null");
    Objects.requireNonNull(votingSessionId, "Voting session ID must not be null");
    Objects.requireNonNull(agendaId, "Agenda ID must not be null");
    Objects.requireNonNull(tally, "Vote tally must not be null");
    Objects.requireNonNull(outcome, "Voting outcome must not be null");
    Objects.requireNonNull(closedAt, "Closing timestamp must not be null");
    Objects.requireNonNull(finalizedAt, "Finalization timestamp must not be null");
    Objects.requireNonNull(occurredAt, "Occurrence timestamp must not be null");
  }

  public static VotingResultFinalizedEvent from(VotingResult result) {
    Objects.requireNonNull(result, "Voting result must not be null");
    return new VotingResultFinalizedEvent(
        UUID.randomUUID(),
        result.votingSessionId(),
        result.agendaId(),
        result.tally(),
        result.outcome(),
        result.closedAt(),
        result.finalizedAt(),
        result.finalizedAt());
  }
}
