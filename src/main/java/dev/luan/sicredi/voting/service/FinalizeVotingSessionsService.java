package dev.luan.sicredi.voting.service;

import dev.luan.sicredi.voting.domainmodel.AgendaId;
import dev.luan.sicredi.voting.domainmodel.VoteTally;
import dev.luan.sicredi.voting.domainmodel.VotingResult;
import dev.luan.sicredi.voting.domainmodel.VotingResultFinalizedEvent;
import dev.luan.sicredi.voting.domainmodel.VotingSession;
import dev.luan.sicredi.voting.engine.VotingResultCalculator;
import dev.luan.sicredi.voting.service.port.in.FinalizeExpiredVotingSessions;
import dev.luan.sicredi.voting.service.port.in.FinalizeVotingSession;
import dev.luan.sicredi.voting.service.port.out.TransactionExecutor;
import dev.luan.sicredi.voting.service.port.out.VoteRepository;
import dev.luan.sicredi.voting.service.port.out.VotingResultOutbox;
import dev.luan.sicredi.voting.service.port.out.VotingResultRepository;
import dev.luan.sicredi.voting.service.port.out.VotingSessionRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class FinalizeVotingSessionsService
    implements FinalizeExpiredVotingSessions, FinalizeVotingSession {

  private final VotingSessionRepository votingSessionRepository;
  private final VoteRepository voteRepository;
  private final VotingResultRepository votingResultRepository;
  private final VotingResultOutbox votingResultOutbox;
  private final TransactionExecutor transactionExecutor;
  private final Clock clock;

  public FinalizeVotingSessionsService(
      VotingSessionRepository votingSessionRepository,
      VoteRepository voteRepository,
      VotingResultRepository votingResultRepository,
      VotingResultOutbox votingResultOutbox,
      TransactionExecutor transactionExecutor,
      Clock clock) {
    this.votingSessionRepository =
        Objects.requireNonNull(
            votingSessionRepository, "Voting session repository must not be null");
    this.voteRepository =
        Objects.requireNonNull(voteRepository, "Vote repository must not be null");
    this.votingResultRepository =
        Objects.requireNonNull(votingResultRepository, "Voting result repository must not be null");
    this.votingResultOutbox =
        Objects.requireNonNull(votingResultOutbox, "Voting result outbox must not be null");
    this.transactionExecutor =
        Objects.requireNonNull(transactionExecutor, "Transaction executor must not be null");
    this.clock = Objects.requireNonNull(clock, "Clock must not be null");
  }

  @Override
  public int execute(int batchSize) {
    if (batchSize < 1) {
      throw new IllegalArgumentException("Finalization batch size must be positive");
    }
    Instant closingThreshold = Instant.now(clock);
    return transactionExecutor.required(() -> finalizeBatch(closingThreshold, batchSize));
  }

  @Override
  public Optional<VotingResult> execute(AgendaId agendaId) {
    Objects.requireNonNull(agendaId, "Agenda ID must not be null");
    Instant closingThreshold = Instant.now(clock);
    return transactionExecutor.required(() -> finalizeByAgendaId(agendaId, closingThreshold));
  }

  private int finalizeBatch(Instant closingThreshold, int batchSize) {
    List<VotingSession> expiredSessions =
        votingSessionRepository.acquireExpiredForClosure(closingThreshold, batchSize);
    expiredSessions.forEach(session -> finalizeSession(session, closingThreshold));
    return expiredSessions.size();
  }

  private Optional<VotingResult> finalizeByAgendaId(AgendaId agendaId, Instant closingThreshold) {
    Optional<VotingSession> session = votingSessionRepository.acquireForClosure(agendaId);
    if (session.isEmpty()) {
      return Optional.empty();
    }
    VotingSession lockedSession = session.orElseThrow();
    if (!lockedSession.isExpiredAt(closingThreshold)) {
      return Optional.empty();
    }
    if (lockedSession.isFinalized()) {
      return votingResultRepository.findByVotingSessionId(lockedSession.id());
    }
    return Optional.of(finalizeSession(lockedSession, closingThreshold));
  }

  private VotingResult finalizeSession(VotingSession session, Instant finalizedAt) {
    VoteTally tally = voteRepository.tally(session.id());
    VotingResult result =
        VotingResultCalculator.calculate(
            session.id(), session.agendaId(), tally, session.closesAt(), finalizedAt);

    votingResultRepository.save(result);
    votingResultOutbox.append(VotingResultFinalizedEvent.from(result));
    votingSessionRepository.save(session.finalizeAt(finalizedAt));
    return result;
  }
}
