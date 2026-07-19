package dev.luan.sicredi.voting.service.error;

import dev.luan.sicredi.voting.domainmodel.AgendaId;

public final class AgendaNotFoundException extends VotingApplicationException {

  public AgendaNotFoundException(AgendaId agendaId) {
    super("AGENDA_NOT_FOUND", "Agenda " + agendaId.value() + " was not found");
  }
}
