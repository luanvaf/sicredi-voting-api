package dev.luan.sicredi.voting.service.error;

import dev.luan.sicredi.voting.domainmodel.AgendaId;

public final class VotingSessionAlreadyExistsException extends VotingApplicationException {

  public VotingSessionAlreadyExistsException(AgendaId agendaId) {
    super(
        "VOTING_SESSION_ALREADY_EXISTS",
        "Agenda " + agendaId.value() + " already has a voting session");
  }
}
