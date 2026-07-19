package dev.luan.sicredi.voting.service.port.in;

import dev.luan.sicredi.voting.domainmodel.AgendaId;
import dev.luan.sicredi.voting.domainmodel.VotingResult;
import java.util.Optional;

public interface FinalizeVotingSession {

  Optional<VotingResult> execute(AgendaId agendaId);
}
