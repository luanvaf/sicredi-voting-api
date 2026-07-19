package dev.luan.sicredi.voting.engine;

import dev.luan.sicredi.voting.domainmodel.AgendaId;
import dev.luan.sicredi.voting.domainmodel.VoteTally;
import dev.luan.sicredi.voting.domainmodel.VotingOutcome;
import dev.luan.sicredi.voting.domainmodel.VotingResult;
import dev.luan.sicredi.voting.domainmodel.VotingSessionId;
import java.time.Instant;
import java.util.Objects;

public final class VotingResultCalculator {

  private VotingResultCalculator() {}

  public static VotingResult calculate(
      VotingSessionId votingSessionId,
      AgendaId agendaId,
      VoteTally tally,
      Instant closedAt,
      Instant finalizedAt) {
    Objects.requireNonNull(tally, "Vote tally must not be null");
    return new VotingResult(
        votingSessionId, agendaId, tally, determineOutcome(tally), closedAt, finalizedAt);
  }

  public static VotingOutcome determineOutcome(VoteTally tally) {
    Objects.requireNonNull(tally, "Vote tally must not be null");
    return switch (Long.compare(tally.yesVotes(), tally.noVotes())) {
      case 1 -> VotingOutcome.APPROVED;
      case -1 -> VotingOutcome.REJECTED;
      default -> VotingOutcome.TIED;
    };
  }
}
