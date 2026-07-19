package dev.luan.sicredi.voting.service.port.in;

import dev.luan.sicredi.voting.domainmodel.AgendaId;
import dev.luan.sicredi.voting.domainmodel.VotingResult;

public interface GetVotingResult {

  VotingResult execute(AgendaId agendaId);
}
