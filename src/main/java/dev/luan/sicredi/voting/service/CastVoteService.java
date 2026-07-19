package dev.luan.sicredi.voting.service;

import dev.luan.sicredi.voting.domainmodel.AssociateEligibility;
import dev.luan.sicredi.voting.domainmodel.Vote;
import dev.luan.sicredi.voting.domainmodel.VoteId;
import dev.luan.sicredi.voting.domainmodel.VotingSession;
import dev.luan.sicredi.voting.engine.VotingSessionPolicy;
import dev.luan.sicredi.voting.service.error.AgendaNotFoundException;
import dev.luan.sicredi.voting.service.error.AssociateAlreadyVotedException;
import dev.luan.sicredi.voting.service.error.AssociateNotEligibleException;
import dev.luan.sicredi.voting.service.error.VotingSessionClosedException;
import dev.luan.sicredi.voting.service.error.VotingSessionNotFoundException;
import dev.luan.sicredi.voting.service.port.in.CastVote;
import dev.luan.sicredi.voting.service.port.out.AgendaRepository;
import dev.luan.sicredi.voting.service.port.out.AssociateEligibilityGateway;
import dev.luan.sicredi.voting.service.port.out.DuplicateVoteException;
import dev.luan.sicredi.voting.service.port.out.TransactionExecutor;
import dev.luan.sicredi.voting.service.port.out.VoteRepository;
import dev.luan.sicredi.voting.service.port.out.VotingSessionRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

public final class CastVoteService implements CastVote {

  private final AgendaRepository agendaRepository;
  private final VotingSessionRepository votingSessionRepository;
  private final VoteRepository voteRepository;
  private final AssociateEligibilityGateway associateEligibilityGateway;
  private final TransactionExecutor transactionExecutor;
  private final Clock clock;

  public CastVoteService(
      AgendaRepository agendaRepository,
      VotingSessionRepository votingSessionRepository,
      VoteRepository voteRepository,
      AssociateEligibilityGateway associateEligibilityGateway,
      TransactionExecutor transactionExecutor,
      Clock clock) {
    this.agendaRepository =
        Objects.requireNonNull(agendaRepository, "Agenda repository must not be null");
    this.votingSessionRepository =
        Objects.requireNonNull(
            votingSessionRepository, "Voting session repository must not be null");
    this.voteRepository =
        Objects.requireNonNull(voteRepository, "Vote repository must not be null");
    this.associateEligibilityGateway =
        Objects.requireNonNull(
            associateEligibilityGateway, "Associate eligibility gateway must not be null");
    this.transactionExecutor =
        Objects.requireNonNull(transactionExecutor, "Transaction executor must not be null");
    this.clock = Objects.requireNonNull(clock, "Clock must not be null");
  }

  @Override
  public Vote execute(Command command) {
    Objects.requireNonNull(command, "Cast vote command must not be null");
    requireOpenSessionBeforeEligibilityCheck(command);

    AssociateEligibility eligibility = associateEligibilityGateway.check(command.associateCpf());
    if (eligibility != AssociateEligibility.ELIGIBLE) {
      throw new AssociateNotEligibleException(command.associateCpf());
    }

    try {
      return transactionExecutor.required(() -> persistVote(command));
    } catch (DuplicateVoteException exception) {
      throw new AssociateAlreadyVotedException(command.agendaId(), exception);
    }
  }

  private void requireOpenSessionBeforeEligibilityCheck(Command command) {
    VotingSession votingSession =
        votingSessionRepository
            .findByAgendaId(command.agendaId())
            .orElseGet(() -> missingVotingSession(command));
    requireOpen(votingSession, command, Instant.now(clock));
  }

  private VotingSession missingVotingSession(Command command) {
    if (agendaRepository.findById(command.agendaId()).isEmpty()) {
      throw new AgendaNotFoundException(command.agendaId());
    }
    throw new VotingSessionNotFoundException(command.agendaId());
  }

  private Vote persistVote(Command command) {
    VotingSession lockedSession =
        votingSessionRepository
            .acquireForVote(command.agendaId())
            .orElseThrow(() -> new VotingSessionNotFoundException(command.agendaId()));
    Instant castAt = Instant.now(clock);
    requireOpen(lockedSession, command, castAt);

    Vote vote =
        new Vote(
            VoteId.newId(), lockedSession.id(), command.associateCpf(), command.choice(), castAt);
    return voteRepository.save(vote);
  }

  private static void requireOpen(VotingSession votingSession, Command command, Instant instant) {
    if (!VotingSessionPolicy.acceptsVote(votingSession, instant)) {
      throw new VotingSessionClosedException(command.agendaId());
    }
  }
}
