package dev.luan.sicredi.voting.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import dev.luan.sicredi.voting.domainmodel.Agenda;
import dev.luan.sicredi.voting.domainmodel.AgendaId;
import dev.luan.sicredi.voting.domainmodel.VoteTally;
import dev.luan.sicredi.voting.domainmodel.VotingOutcome;
import dev.luan.sicredi.voting.domainmodel.VotingResult;
import dev.luan.sicredi.voting.domainmodel.VotingSessionId;
import dev.luan.sicredi.voting.service.error.AgendaNotFoundException;
import dev.luan.sicredi.voting.service.error.VotingResultNotAvailableException;
import dev.luan.sicredi.voting.service.port.in.FinalizeVotingSession;
import dev.luan.sicredi.voting.service.port.out.AgendaRepository;
import dev.luan.sicredi.voting.service.port.out.VotingResultRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class GetVotingResultServiceTest {

  private static final Instant NOW = Instant.parse("2026-07-19T12:00:00Z");

  private final AgendaId agendaId = AgendaId.newId();
  private final AgendaRepository agendaRepository = mock(AgendaRepository.class);
  private final VotingResultRepository votingResultRepository = mock(VotingResultRepository.class);
  private final FinalizeVotingSession finalizeVotingSession = mock(FinalizeVotingSession.class);
  private final GetVotingResultService service =
      new GetVotingResultService(agendaRepository, votingResultRepository, finalizeVotingSession);

  @Test
  void shouldReturnTheFinalizedResult() {
    VotingResult result =
        new VotingResult(
            VotingSessionId.newId(),
            agendaId,
            new VoteTally(3, 1),
            VotingOutcome.APPROVED,
            NOW.minusSeconds(1),
            NOW);
    when(agendaRepository.findById(agendaId))
        .thenReturn(Optional.of(new Agenda(agendaId, "Budget", null, NOW.minusSeconds(10))));
    when(votingResultRepository.findByAgendaId(agendaId)).thenReturn(Optional.of(result));

    assertThat(service.execute(agendaId)).isEqualTo(result);
    verifyNoInteractions(finalizeVotingSession);
  }

  @Test
  void shouldFinalizeAnExpiredSessionOnDemand() {
    VotingResult result =
        new VotingResult(
            VotingSessionId.newId(),
            agendaId,
            new VoteTally(2, 2),
            VotingOutcome.TIED,
            NOW.minusSeconds(1),
            NOW);
    when(agendaRepository.findById(agendaId))
        .thenReturn(Optional.of(new Agenda(agendaId, "Budget", null, NOW.minusSeconds(10))));
    when(votingResultRepository.findByAgendaId(agendaId)).thenReturn(Optional.empty());
    when(finalizeVotingSession.execute(agendaId)).thenReturn(Optional.of(result));

    assertThat(service.execute(agendaId)).isEqualTo(result);
  }

  @Test
  void shouldRejectAnUnknownAgenda() {
    when(agendaRepository.findById(agendaId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.execute(agendaId)).isInstanceOf(AgendaNotFoundException.class);
  }

  @Test
  void shouldRejectAResultThatIsNotFinalizedYet() {
    when(agendaRepository.findById(agendaId))
        .thenReturn(Optional.of(new Agenda(agendaId, "Budget", null, NOW.minusSeconds(10))));
    when(votingResultRepository.findByAgendaId(agendaId)).thenReturn(Optional.empty());
    when(finalizeVotingSession.execute(agendaId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.execute(agendaId))
        .isInstanceOf(VotingResultNotAvailableException.class);
  }
}
