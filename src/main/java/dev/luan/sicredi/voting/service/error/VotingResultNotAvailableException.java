package dev.luan.sicredi.voting.service.error;

import dev.luan.sicredi.voting.domainmodel.AgendaId;

public final class VotingResultNotAvailableException extends VotingApplicationException {

  public VotingResultNotAvailableException(AgendaId agendaId) {
    super(
        "VOTING_RESULT_NOT_AVAILABLE",
        "The voting result for agenda " + agendaId.value() + " is not available yet");
  }
}
