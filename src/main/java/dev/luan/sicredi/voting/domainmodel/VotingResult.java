package dev.luan.sicredi.voting.domainmodel;

import java.time.Instant;
import java.util.Objects;

public record VotingResult(
    VotingSessionId votingSessionId,
    AgendaId agendaId,
    VoteTally tally,
    VotingOutcome outcome,
    Instant closedAt,
    Instant finalizedAt) {

  public VotingResult {
    Objects.requireNonNull(votingSessionId, "Voting session ID must not be null");
    Objects.requireNonNull(agendaId, "Agenda ID must not be null");
    Objects.requireNonNull(tally, "Vote tally must not be null");
    Objects.requireNonNull(outcome, "Voting outcome must not be null");
    Objects.requireNonNull(closedAt, "Voting result closing timestamp must not be null");
    Objects.requireNonNull(finalizedAt, "Voting result finalization timestamp must not be null");

    if (finalizedAt.isBefore(closedAt)) {
      throw new IllegalArgumentException(
          "Voting result finalization timestamp must not be before its closing timestamp");
    }
  }
}
