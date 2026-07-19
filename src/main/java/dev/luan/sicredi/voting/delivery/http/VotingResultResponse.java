package dev.luan.sicredi.voting.delivery.http;

import dev.luan.sicredi.voting.domainmodel.VotingOutcome;
import dev.luan.sicredi.voting.domainmodel.VotingResult;
import java.time.Instant;
import java.util.UUID;

record VotingResultResponse(
    UUID agendaId,
    UUID votingSessionId,
    long yesVotes,
    long noVotes,
    long totalVotes,
    VotingOutcome outcome,
    Instant closedAt,
    Instant finalizedAt) {

  static VotingResultResponse fromDomain(VotingResult result) {
    return new VotingResultResponse(
        result.agendaId().value(),
        result.votingSessionId().value(),
        result.tally().yesVotes(),
        result.tally().noVotes(),
        result.tally().totalVotes(),
        result.outcome(),
        result.closedAt(),
        result.finalizedAt());
  }
}
