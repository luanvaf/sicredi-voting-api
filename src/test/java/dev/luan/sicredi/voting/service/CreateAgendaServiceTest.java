package dev.luan.sicredi.voting.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import dev.luan.sicredi.voting.domainmodel.Agenda;
import dev.luan.sicredi.voting.service.port.in.CreateAgenda;
import dev.luan.sicredi.voting.service.port.out.AgendaRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class CreateAgendaServiceTest {

  private static final Instant NOW = Instant.parse("2026-07-19T12:00:00Z");

  @Test
  void shouldCreateAndPersistAnAgenda() {
    AgendaRepository agendaRepository = mock(AgendaRepository.class);
    when(agendaRepository.save(any(Agenda.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    CreateAgendaService service =
        new CreateAgendaService(
            agendaRepository, new ImmediateTransactionExecutor(), Clock.fixed(NOW, ZoneOffset.UTC));

    Agenda agenda =
        service.execute(new CreateAgenda.Command("Annual budget", "Approve next year's budget"));

    assertThat(agenda.id()).isNotNull();
    assertThat(agenda.title()).isEqualTo("Annual budget");
    assertThat(agenda.description()).isEqualTo("Approve next year's budget");
    assertThat(agenda.createdAt()).isEqualTo(NOW);
  }
}
