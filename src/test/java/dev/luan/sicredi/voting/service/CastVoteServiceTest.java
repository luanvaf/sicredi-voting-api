package dev.luan.sicredi.voting.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import dev.luan.sicredi.voting.domainmodel.AgendaId;
import dev.luan.sicredi.voting.domainmodel.AssociateCpf;
import dev.luan.sicredi.voting.domainmodel.AssociateEligibility;
import dev.luan.sicredi.voting.domainmodel.Vote;
import dev.luan.sicredi.voting.domainmodel.VoteChoice;
import dev.luan.sicredi.voting.domainmodel.VotingSession;
import dev.luan.sicredi.voting.service.error.AssociateAlreadyVotedException;
import dev.luan.sicredi.voting.service.error.AssociateNotEligibleException;
import dev.luan.sicredi.voting.service.error.VotingSessionClosedException;
import dev.luan.sicredi.voting.service.error.VotingSessionNotFoundException;
import dev.luan.sicredi.voting.service.port.in.CastVote;
import dev.luan.sicredi.voting.service.port.out.AssociateEligibilityGateway;
import dev.luan.sicredi.voting.service.port.out.DuplicateVoteException;
import dev.luan.sicredi.voting.service.port.out.VoteRepository;
import dev.luan.sicredi.voting.service.port.out.VotingSessionRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CastVoteServiceTest {

  private static final Instant OPENED_AT = Instant.parse("2026-07-19T12:00:00Z");

  private final AgendaId agendaId = AgendaId.newId();
  private final AssociateCpf associateCpf = new AssociateCpf("19839091069");
  private final VotingSession votingSession =
      VotingSession.open(agendaId, OPENED_AT, Duration.ofMinutes(1));
  private final VotingSessionRepository votingSessionRepository =
      mock(VotingSessionRepository.class);
  private final VoteRepository voteRepository = mock(VoteRepository.class);
  private final AssociateEligibilityGateway eligibilityGateway =
      mock(AssociateEligibilityGateway.class);

  @BeforeEach
  void configureRepositories() {
    when(votingSessionRepository.findByAgendaId(agendaId)).thenReturn(Optional.of(votingSession));
    when(votingSessionRepository.acquireForVote(agendaId)).thenReturn(Optional.of(votingSession));
    when(eligibilityGateway.check(associateCpf)).thenReturn(AssociateEligibility.ELIGIBLE);
    when(voteRepository.save(any(Vote.class))).thenAnswer(invocation -> invocation.getArgument(0));
  }

  @Test
  void shouldPersistAnEligibleVoteDuringTheOpenSession() {
    CastVoteService service = serviceWithFixedClock(OPENED_AT.plusSeconds(30));

    Vote vote = service.execute(new CastVote.Command(agendaId, associateCpf, VoteChoice.YES));

    assertThat(vote.votingSessionId()).isEqualTo(votingSession.id());
    assertThat(vote.associateCpf()).isEqualTo(associateCpf);
    assertThat(vote.choice()).isEqualTo(VoteChoice.YES);
    assertThat(vote.castAt()).isEqualTo(OPENED_AT.plusSeconds(30));
  }

  @Test
  void shouldRejectAnUnknownVotingSessionBeforeCheckingEligibility() {
    when(votingSessionRepository.findByAgendaId(agendaId)).thenReturn(Optional.empty());
    CastVoteService service = serviceWithFixedClock(OPENED_AT.plusSeconds(30));

    assertThatThrownBy(
            () -> service.execute(new CastVote.Command(agendaId, associateCpf, VoteChoice.YES)))
        .isInstanceOf(VotingSessionNotFoundException.class);
    verifyNoInteractions(eligibilityGateway);
  }

  @Test
  void shouldRejectAClosedVotingSessionBeforeCheckingEligibility() {
    CastVoteService service = serviceWithFixedClock(votingSession.closesAt());

    assertThatThrownBy(
            () -> service.execute(new CastVote.Command(agendaId, associateCpf, VoteChoice.YES)))
        .isInstanceOf(VotingSessionClosedException.class);
    verifyNoInteractions(eligibilityGateway);
  }

  @Test
  void shouldRejectAnIneligibleAssociate() {
    when(eligibilityGateway.check(associateCpf)).thenReturn(AssociateEligibility.INELIGIBLE);
    CastVoteService service = serviceWithFixedClock(OPENED_AT.plusSeconds(30));

    assertThatThrownBy(
            () -> service.execute(new CastVote.Command(agendaId, associateCpf, VoteChoice.YES)))
        .isInstanceOf(AssociateNotEligibleException.class);
  }

  @Test
  void shouldRecheckTheSessionAfterTheEligibilityCall() {
    Clock clock = mock(Clock.class);
    when(clock.instant()).thenReturn(OPENED_AT.plusSeconds(59), votingSession.closesAt());
    CastVoteService service = serviceWithClock(clock);

    assertThatThrownBy(
            () -> service.execute(new CastVote.Command(agendaId, associateCpf, VoteChoice.YES)))
        .isInstanceOf(VotingSessionClosedException.class);
  }

  @Test
  void shouldTranslateAConcurrentDuplicateVote() {
    when(voteRepository.save(any(Vote.class)))
        .thenThrow(new DuplicateVoteException(new RuntimeException()));
    CastVoteService service = serviceWithFixedClock(OPENED_AT.plusSeconds(30));

    assertThatThrownBy(
            () -> service.execute(new CastVote.Command(agendaId, associateCpf, VoteChoice.YES)))
        .isInstanceOf(AssociateAlreadyVotedException.class);
  }

  private CastVoteService serviceWithFixedClock(Instant instant) {
    return serviceWithClock(Clock.fixed(instant, ZoneOffset.UTC));
  }

  private CastVoteService serviceWithClock(Clock clock) {
    return new CastVoteService(
        votingSessionRepository,
        voteRepository,
        eligibilityGateway,
        new ImmediateTransactionExecutor(),
        clock);
  }
}
