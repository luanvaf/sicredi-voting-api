package dev.luan.sicredi.voting.service;

import dev.luan.sicredi.voting.domainmodel.VotingSession;
import dev.luan.sicredi.voting.engine.VotingSessionDurationPolicy;
import dev.luan.sicredi.voting.service.error.AgendaNotFoundException;
import dev.luan.sicredi.voting.service.error.VotingSessionAlreadyExistsException;
import dev.luan.sicredi.voting.service.port.in.OpenVotingSession;
import dev.luan.sicredi.voting.service.port.out.AgendaRepository;
import dev.luan.sicredi.voting.service.port.out.DuplicateVotingSessionException;
import dev.luan.sicredi.voting.service.port.out.TransactionExecutor;
import dev.luan.sicredi.voting.service.port.out.VotingSessionRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

public final class OpenVotingSessionService implements OpenVotingSession {

  private final AgendaRepository agendaRepository;
  private final VotingSessionRepository votingSessionRepository;
  private final TransactionExecutor transactionExecutor;
  private final VotingSessionDurationPolicy durationPolicy;
  private final Clock clock;

  public OpenVotingSessionService(
      AgendaRepository agendaRepository,
      VotingSessionRepository votingSessionRepository,
      TransactionExecutor transactionExecutor,
      VotingSessionDurationPolicy durationPolicy,
      Clock clock) {
    this.agendaRepository =
        Objects.requireNonNull(agendaRepository, "Agenda repository must not be null");
    this.votingSessionRepository =
        Objects.requireNonNull(
            votingSessionRepository, "Voting session repository must not be null");
    this.transactionExecutor =
        Objects.requireNonNull(transactionExecutor, "Transaction executor must not be null");
    this.durationPolicy =
        Objects.requireNonNull(durationPolicy, "Duration policy must not be null");
    this.clock = Objects.requireNonNull(clock, "Clock must not be null");
  }

  @Override
  public VotingSession execute(Command command) {
    Objects.requireNonNull(command, "Open voting session command must not be null");
    Duration duration = durationPolicy.resolve(command.requestedDuration());

    try {
      return transactionExecutor.required(() -> open(command, duration));
    } catch (DuplicateVotingSessionException exception) {
      throw new VotingSessionAlreadyExistsException(command.agendaId());
    }
  }

  private VotingSession open(Command command, Duration duration) {
    agendaRepository
        .findById(command.agendaId())
        .orElseThrow(() -> new AgendaNotFoundException(command.agendaId()));
    votingSessionRepository
        .findByAgendaId(command.agendaId())
        .ifPresent(
            ignored -> {
              throw new VotingSessionAlreadyExistsException(command.agendaId());
            });

    VotingSession votingSession =
        VotingSession.open(command.agendaId(), Instant.now(clock), duration);
    return votingSessionRepository.save(votingSession);
  }
}
