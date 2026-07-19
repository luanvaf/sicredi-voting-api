package dev.luan.sicredi.voting.service.error;

import dev.luan.sicredi.voting.domainmodel.AgendaId;

public final class AssociateAlreadyVotedException extends VotingApplicationException {

  public AssociateAlreadyVotedException(AgendaId agendaId, Throwable cause) {
    super(
        "ASSOCIATE_ALREADY_VOTED",
        "The associate has already voted on agenda " + agendaId.value(),
        cause);
  }
}
