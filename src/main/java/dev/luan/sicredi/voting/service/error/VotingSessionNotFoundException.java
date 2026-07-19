package dev.luan.sicredi.voting.service.error;

import dev.luan.sicredi.voting.domainmodel.AgendaId;

public final class VotingSessionNotFoundException extends VotingApplicationException {

  public VotingSessionNotFoundException(AgendaId agendaId) {
    super(
        "VOTING_SESSION_NOT_FOUND",
        "Agenda " + agendaId.value() + " does not have a voting session");
  }
}
