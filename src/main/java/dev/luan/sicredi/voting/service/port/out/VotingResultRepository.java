package dev.luan.sicredi.voting.service.port.out;

import dev.luan.sicredi.voting.domainmodel.AgendaId;
import dev.luan.sicredi.voting.domainmodel.VotingResult;
import dev.luan.sicredi.voting.domainmodel.VotingSessionId;
import java.util.Optional;

public interface VotingResultRepository {

  VotingResult save(VotingResult votingResult);

  Optional<VotingResult> findByAgendaId(AgendaId agendaId);

  Optional<VotingResult> findByVotingSessionId(VotingSessionId votingSessionId);
}
