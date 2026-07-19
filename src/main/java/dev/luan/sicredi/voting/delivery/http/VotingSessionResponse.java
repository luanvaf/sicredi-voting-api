package dev.luan.sicredi.voting.delivery.http;

import dev.luan.sicredi.voting.domainmodel.VotingSession;
import java.time.Instant;
import java.util.UUID;

record VotingSessionResponse(
    UUID id, UUID agendaId, Instant openedAt, Instant closesAt, String status) {

  static VotingSessionResponse fromDomain(VotingSession votingSession) {
    return new VotingSessionResponse(
        votingSession.id().value(),
        votingSession.agendaId().value(),
        votingSession.openedAt(),
        votingSession.closesAt(),
        votingSession.isFinalized() ? "CLOSED" : "OPEN");
  }
}
