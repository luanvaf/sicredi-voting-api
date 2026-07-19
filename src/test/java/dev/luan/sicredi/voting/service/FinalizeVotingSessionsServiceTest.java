package dev.luan.sicredi.voting.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import dev.luan.sicredi.voting.domainmodel.AgendaId;
import dev.luan.sicredi.voting.domainmodel.VoteTally;
import dev.luan.sicredi.voting.domainmodel.VotingOutcome;
import dev.luan.sicredi.voting.domainmodel.VotingResult;
import dev.luan.sicredi.voting.domainmodel.VotingResultFinalizedEvent;
import dev.luan.sicredi.voting.domainmodel.VotingSession;
import dev.luan.sicredi.voting.domainmodel.VotingSessionId;
import dev.luan.sicredi.voting.service.port.out.VoteRepository;
import dev.luan.sicredi.voting.service.port.out.VotingResultOutbox;
import dev.luan.sicredi.voting.service.port.out.VotingResultRepository;
import dev.luan.sicredi.voting.service.port.out.VotingSessionRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class FinalizeVotingSessionsServiceTest {

  private static final Instant NOW = Instant.parse("2026-07-19T12:00:00Z");

  private final VotingSessionRepository votingSessionRepository =
      mock(VotingSessionRepository.class);
  private final VoteRepository voteRepository = mock(VoteRepository.class);
  private final VotingResultRepository votingResultRepository = mock(VotingResultRepository.class);
  private final VotingResultOutbox votingResultOutbox = mock(VotingResultOutbox.class);
  private final FinalizeVotingSessionsService service =
      new FinalizeVotingSessionsService(
          votingSessionRepository,
          voteRepository,
          votingResultRepository,
          votingResultOutbox,
          new ImmediateTransactionExecutor(),
          Clock.fixed(NOW, ZoneOffset.UTC));

  @Test
  void shouldFinalizeAnExpiredSessionAndAppendItsEvent() {
    VotingSession session =
        new VotingSession(
            VotingSessionId.newId(),
            AgendaId.newId(),
            NOW.minusSeconds(120),
            NOW.minusSeconds(60),
            null);
    VoteTally tally = new VoteTally(7, 4);
    when(votingSessionRepository.acquireExpiredForClosure(NOW, 100)).thenReturn(List.of(session));
    when(voteRepository.tally(session.id())).thenReturn(tally);
    when(votingResultRepository.save(any(VotingResult.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(votingSessionRepository.save(any(VotingSession.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    int finalizedSessions = service.execute(100);

    assertThat(finalizedSessions).isOne();

    ArgumentCaptor<VotingResult> resultCaptor = ArgumentCaptor.forClass(VotingResult.class);
    verify(votingResultRepository).save(resultCaptor.capture());
    VotingResult result = resultCaptor.getValue();
    assertThat(result.tally()).isEqualTo(tally);
    assertThat(result.outcome()).isEqualTo(VotingOutcome.APPROVED);
    assertThat(result.closedAt()).isEqualTo(session.closesAt());
    assertThat(result.finalizedAt()).isEqualTo(NOW);

    ArgumentCaptor<VotingResultFinalizedEvent> eventCaptor =
        ArgumentCaptor.forClass(VotingResultFinalizedEvent.class);
    verify(votingResultOutbox).append(eventCaptor.capture());
    assertThat(eventCaptor.getValue())
        .extracting(
            VotingResultFinalizedEvent::votingSessionId,
            VotingResultFinalizedEvent::agendaId,
            VotingResultFinalizedEvent::tally,
            VotingResultFinalizedEvent::outcome,
            VotingResultFinalizedEvent::occurredAt)
        .containsExactly(session.id(), session.agendaId(), tally, VotingOutcome.APPROVED, NOW);

    ArgumentCaptor<VotingSession> sessionCaptor = ArgumentCaptor.forClass(VotingSession.class);
    verify(votingSessionRepository).save(sessionCaptor.capture());
    assertThat(sessionCaptor.getValue().finalizedAt()).isEqualTo(NOW);
  }

  @Test
  void shouldFinalizeAnExpiredSessionByAgendaId() {
    VotingSession session =
        new VotingSession(
            VotingSessionId.newId(),
            AgendaId.newId(),
            NOW.minusSeconds(120),
            NOW.minusSeconds(60),
            null);
    when(votingSessionRepository.acquireForClosure(session.agendaId()))
        .thenReturn(Optional.of(session));
    when(voteRepository.tally(session.id())).thenReturn(new VoteTally(1, 0));
    when(votingResultRepository.save(any(VotingResult.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(votingSessionRepository.save(any(VotingSession.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    Optional<VotingResult> result = service.execute(session.agendaId());

    assertThat(result).isPresent();
    assertThat(result.orElseThrow().outcome()).isEqualTo(VotingOutcome.APPROVED);
    verify(votingResultOutbox).append(any(VotingResultFinalizedEvent.class));
  }

  @Test
  void shouldNotFinalizeAnOpenSessionOnDemand() {
    VotingSession session =
        new VotingSession(
            VotingSessionId.newId(),
            AgendaId.newId(),
            NOW.minusSeconds(30),
            NOW.plusSeconds(30),
            null);
    when(votingSessionRepository.acquireForClosure(session.agendaId()))
        .thenReturn(Optional.of(session));

    assertThat(service.execute(session.agendaId())).isEmpty();
    verifyNoInteractions(voteRepository, votingResultRepository, votingResultOutbox);
  }

  @Test
  void shouldRejectANonPositiveBatchSize() {
    assertThatThrownBy(() -> service.execute(0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Finalization batch size must be positive");
  }
}
