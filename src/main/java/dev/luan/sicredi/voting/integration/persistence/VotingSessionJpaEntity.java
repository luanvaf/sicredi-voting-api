package dev.luan.sicredi.voting.integration.persistence;

import dev.luan.sicredi.voting.domainmodel.AgendaId;
import dev.luan.sicredi.voting.domainmodel.VotingSession;
import dev.luan.sicredi.voting.domainmodel.VotingSessionId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "voting_sessions")
class VotingSessionJpaEntity {

  @Id private UUID id;

  @Column(name = "agenda_id", nullable = false, unique = true)
  private UUID agendaId;

  @Column(name = "opened_at", nullable = false)
  private Instant openedAt;

  @Column(name = "closes_at", nullable = false)
  private Instant closesAt;

  @Column(name = "finalized_at")
  private Instant finalizedAt;

  protected VotingSessionJpaEntity() {}

  private VotingSessionJpaEntity(
      UUID id, UUID agendaId, Instant openedAt, Instant closesAt, Instant finalizedAt) {
    this.id = id;
    this.agendaId = agendaId;
    this.openedAt = openedAt;
    this.closesAt = closesAt;
    this.finalizedAt = finalizedAt;
  }

  static VotingSessionJpaEntity fromDomain(VotingSession votingSession) {
    return new VotingSessionJpaEntity(
        votingSession.id().value(),
        votingSession.agendaId().value(),
        votingSession.openedAt(),
        votingSession.closesAt(),
        votingSession.finalizedAt());
  }

  VotingSession toDomain() {
    return new VotingSession(
        new VotingSessionId(id), new AgendaId(agendaId), openedAt, closesAt, finalizedAt);
  }
}
