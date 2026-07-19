package dev.luan.sicredi.voting.service.port.in;

import dev.luan.sicredi.voting.domainmodel.Agenda;

public interface CreateAgenda {

  Agenda execute(Command command);

  record Command(String title, String description) {}
}
