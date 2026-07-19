package dev.luan.sicredi.voting.service.port.out;

import dev.luan.sicredi.voting.domainmodel.Agenda;
import dev.luan.sicredi.voting.domainmodel.AgendaId;
import java.util.Optional;

public interface AgendaRepository {

  Agenda save(Agenda agenda);

  Optional<Agenda> findById(AgendaId agendaId);
}
