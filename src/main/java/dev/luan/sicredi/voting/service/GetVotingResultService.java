package dev.luan.sicredi.voting.service;

import dev.luan.sicredi.voting.domainmodel.AgendaId;
import dev.luan.sicredi.voting.domainmodel.VotingResult;
import dev.luan.sicredi.voting.service.error.AgendaNotFoundException;
import dev.luan.sicredi.voting.service.error.VotingResultNotAvailableException;
import dev.luan.sicredi.voting.service.port.in.FinalizeVotingSession;
import dev.luan.sicredi.voting.service.port.in.GetVotingResult;
import dev.luan.sicredi.voting.service.port.out.AgendaRepository;
import dev.luan.sicredi.voting.service.port.out.VotingResultRepository;
import java.util.Objects;

public final class GetVotingResultService implements GetVotingResult {

  private final AgendaRepository agendaRepository;
  private final VotingResultRepository votingResultRepository;
  private final FinalizeVotingSession finalizeVotingSession;

  public GetVotingResultService(
      AgendaRepository agendaRepository,
      VotingResultRepository votingResultRepository,
      FinalizeVotingSession finalizeVotingSession) {
    this.agendaRepository =
        Objects.requireNonNull(agendaRepository, "Agenda repository must not be null");
    this.votingResultRepository =
        Objects.requireNonNull(votingResultRepository, "Voting result repository must not be null");
    this.finalizeVotingSession =
        Objects.requireNonNull(
            finalizeVotingSession, "Finalize voting session use case must not be null");
  }

  @Override
  public VotingResult execute(AgendaId agendaId) {
    Objects.requireNonNull(agendaId, "Agenda ID must not be null");
    agendaRepository.findById(agendaId).orElseThrow(() -> new AgendaNotFoundException(agendaId));
    return votingResultRepository
        .findByAgendaId(agendaId)
        .or(() -> finalizeVotingSession.execute(agendaId))
        .orElseThrow(() -> new VotingResultNotAvailableException(agendaId));
  }
}
