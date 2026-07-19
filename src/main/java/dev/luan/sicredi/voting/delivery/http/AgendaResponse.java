package dev.luan.sicredi.voting.delivery.http;

import dev.luan.sicredi.voting.domainmodel.Agenda;
import java.time.Instant;
import java.util.UUID;

record AgendaResponse(UUID id, String title, String description, Instant createdAt) {

  static AgendaResponse fromDomain(Agenda agenda) {
    return new AgendaResponse(
        agenda.id().value(), agenda.title(), agenda.description(), agenda.createdAt());
  }
}
