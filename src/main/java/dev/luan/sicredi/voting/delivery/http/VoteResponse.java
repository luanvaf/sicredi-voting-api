package dev.luan.sicredi.voting.delivery.http;

import dev.luan.sicredi.voting.domainmodel.Vote;
import dev.luan.sicredi.voting.domainmodel.VoteChoice;
import java.time.Instant;
import java.util.UUID;

record VoteResponse(
    UUID id, UUID agendaId, UUID votingSessionId, VoteChoice choice, Instant castAt) {

  static VoteResponse fromDomain(UUID agendaId, Vote vote) {
    return new VoteResponse(
        vote.id().value(), agendaId, vote.votingSessionId().value(), vote.choice(), vote.castAt());
  }
}
