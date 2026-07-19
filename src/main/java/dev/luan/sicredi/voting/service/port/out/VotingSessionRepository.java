package dev.luan.sicredi.voting.service.port.out;

import dev.luan.sicredi.voting.domainmodel.AgendaId;
import dev.luan.sicredi.voting.domainmodel.VotingSession;
import dev.luan.sicredi.voting.domainmodel.VotingSessionId;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface VotingSessionRepository {

  VotingSession save(VotingSession votingSession);

  Optional<VotingSession> findByAgendaId(AgendaId agendaId);

  Optional<VotingSession> acquireForVote(AgendaId agendaId);

  Optional<VotingSession> acquireForClosure(VotingSessionId votingSessionId);

  List<VotingSession> acquireExpiredForClosure(Instant closingThreshold, int batchSize);
}
