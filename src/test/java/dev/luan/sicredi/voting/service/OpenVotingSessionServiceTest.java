package dev.luan.sicredi.voting.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import dev.luan.sicredi.voting.domainmodel.Agenda;
import dev.luan.sicredi.voting.domainmodel.AgendaId;
import dev.luan.sicredi.voting.domainmodel.VotingSession;
import dev.luan.sicredi.voting.engine.VotingSessionDurationPolicy;
import dev.luan.sicredi.voting.service.error.AgendaNotFoundException;
import dev.luan.sicredi.voting.service.error.VotingSessionAlreadyExistsException;
import dev.luan.sicredi.voting.service.port.in.OpenVotingSession;
import dev.luan.sicredi.voting.service.port.out.AgendaRepository;
import dev.luan.sicredi.voting.service.port.out.DuplicateVotingSessionException;
import dev.luan.sicredi.voting.service.port.out.VotingSessionRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OpenVotingSessionServiceTest {

  private static final Instant NOW = Instant.parse("2026-07-19T12:00:00Z");

  private final AgendaId agendaId = AgendaId.newId();
  private final AgendaRepository agendaRepository = mock(AgendaRepository.class);
  private final VotingSessionRepository votingSessionRepository =
      mock(VotingSessionRepository.class);
  private final OpenVotingSessionService service =
      new OpenVotingSessionService(
          agendaRepository,
          votingSessionRepository,
          new ImmediateTransactionExecutor(),
          new VotingSessionDurationPolicy(Duration.ofMinutes(1), Duration.ofHours(24)),
          Clock.fixed(NOW, ZoneOffset.UTC));

  @BeforeEach
  void configureAgenda() {
    when(agendaRepository.findById(agendaId))
        .thenReturn(Optional.of(new Agenda(agendaId, "Annual budget", null, NOW.minusSeconds(1))));
    when(votingSessionRepository.findByAgendaId(agendaId)).thenReturn(Optional.empty());
    when(votingSessionRepository.save(any(VotingSession.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
  }

  @Test
  void shouldOpenAOneMinuteSessionByDefault() {
    VotingSession votingSession =
        service.execute(new OpenVotingSession.Command(agendaId, Optional.empty()));

    assertThat(votingSession.openedAt()).isEqualTo(NOW);
    assertThat(votingSession.closesAt()).isEqualTo(NOW.plusSeconds(60));
  }

  @Test
  void shouldOpenASessionWithTheRequestedDuration() {
    VotingSession votingSession =
        service.execute(
            new OpenVotingSession.Command(agendaId, Optional.of(Duration.ofMinutes(5))));

    assertThat(votingSession.closesAt()).isEqualTo(NOW.plusSeconds(300));
  }

  @Test
  void shouldRejectAnUnknownAgenda() {
    when(agendaRepository.findById(agendaId)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () -> service.execute(new OpenVotingSession.Command(agendaId, Optional.empty())))
        .isInstanceOf(AgendaNotFoundException.class);
  }

  @Test
  void shouldRejectAnAgendaThatAlreadyHasASession() {
    when(votingSessionRepository.findByAgendaId(agendaId))
        .thenReturn(
            Optional.of(VotingSession.open(agendaId, NOW.minusSeconds(10), Duration.ofMinutes(1))));

    assertThatThrownBy(
            () -> service.execute(new OpenVotingSession.Command(agendaId, Optional.empty())))
        .isInstanceOf(VotingSessionAlreadyExistsException.class);
  }

  @Test
  void shouldTranslateAConcurrentUniqueConstraintViolation() {
    when(votingSessionRepository.save(any(VotingSession.class)))
        .thenThrow(new DuplicateVotingSessionException(new RuntimeException()));

    assertThatThrownBy(
            () -> service.execute(new OpenVotingSession.Command(agendaId, Optional.empty())))
        .isInstanceOf(VotingSessionAlreadyExistsException.class);
  }
}
