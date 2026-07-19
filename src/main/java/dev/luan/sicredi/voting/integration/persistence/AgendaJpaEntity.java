package dev.luan.sicredi.voting.integration.persistence;

import dev.luan.sicredi.voting.domainmodel.Agenda;
import dev.luan.sicredi.voting.domainmodel.AgendaId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "agendas")
class AgendaJpaEntity {

  @Id private UUID id;

  @Column(nullable = false, length = 200)
  private String title;

  @Column(length = 1000)
  private String description;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected AgendaJpaEntity() {}

  private AgendaJpaEntity(UUID id, String title, String description, Instant createdAt) {
    this.id = id;
    this.title = title;
    this.description = description;
    this.createdAt = createdAt;
  }

  static AgendaJpaEntity fromDomain(Agenda agenda) {
    return new AgendaJpaEntity(
        agenda.id().value(), agenda.title(), agenda.description(), agenda.createdAt());
  }

  Agenda toDomain() {
    return new Agenda(new AgendaId(id), title, description, createdAt);
  }
}
