package dev.luan.sicredi.voting.service.error;

import dev.luan.sicredi.voting.domainmodel.AgendaId;

public final class VotingSessionClosedException extends VotingApplicationException {

  public VotingSessionClosedException(AgendaId agendaId) {
    super(
        "VOTING_SESSION_CLOSED",
        "The voting session for agenda " + agendaId.value() + " is closed");
  }
}
