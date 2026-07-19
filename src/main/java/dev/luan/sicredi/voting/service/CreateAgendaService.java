package dev.luan.sicredi.voting.service;

import dev.luan.sicredi.voting.domainmodel.Agenda;
import dev.luan.sicredi.voting.domainmodel.AgendaId;
import dev.luan.sicredi.voting.service.port.in.CreateAgenda;
import dev.luan.sicredi.voting.service.port.out.AgendaRepository;
import dev.luan.sicredi.voting.service.port.out.TransactionExecutor;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

public final class CreateAgendaService implements CreateAgenda {

  private final AgendaRepository agendaRepository;
  private final TransactionExecutor transactionExecutor;
  private final Clock clock;

  public CreateAgendaService(
      AgendaRepository agendaRepository, TransactionExecutor transactionExecutor, Clock clock) {
    this.agendaRepository =
        Objects.requireNonNull(agendaRepository, "Agenda repository must not be null");
    this.transactionExecutor =
        Objects.requireNonNull(transactionExecutor, "Transaction executor must not be null");
    this.clock = Objects.requireNonNull(clock, "Clock must not be null");
  }

  @Override
  public Agenda execute(Command command) {
    Objects.requireNonNull(command, "Create agenda command must not be null");
    Agenda agenda =
        new Agenda(AgendaId.newId(), command.title(), command.description(), Instant.now(clock));

    return transactionExecutor.required(() -> agendaRepository.save(agenda));
  }
}
