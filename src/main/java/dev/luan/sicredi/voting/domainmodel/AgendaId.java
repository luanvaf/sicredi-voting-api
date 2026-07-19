package dev.luan.sicredi.voting.domainmodel;

import java.util.Objects;
import java.util.UUID;

public record AgendaId(UUID value) {

  public AgendaId {
    Objects.requireNonNull(value, "Agenda ID must not be null");
  }

  public static AgendaId newId() {
    return new AgendaId(UUID.randomUUID());
  }

  public static AgendaId from(String value) {
    return new AgendaId(UUID.fromString(value));
  }
}
