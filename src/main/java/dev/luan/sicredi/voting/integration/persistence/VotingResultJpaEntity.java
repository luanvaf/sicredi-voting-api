package dev.luan.sicredi.voting.integration.persistence;

import dev.luan.sicredi.voting.domainmodel.AgendaId;
import dev.luan.sicredi.voting.domainmodel.VoteTally;
import dev.luan.sicredi.voting.domainmodel.VotingOutcome;
import dev.luan.sicredi.voting.domainmodel.VotingResult;
import dev.luan.sicredi.voting.domainmodel.VotingSessionId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "voting_results")
class VotingResultJpaEntity {

  @Id
  @Column(name = "voting_session_id")
  private UUID votingSessionId;

  @Column(name = "agenda_id", nullable = false, unique = true)
  private UUID agendaId;

  @Column(name = "yes_votes", nullable = false)
  private long yesVotes;

  @Column(name = "no_votes", nullable = false)
  private long noVotes;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 8)
  private VotingOutcome outcome;

  @Column(name = "closed_at", nullable = false)
  private Instant closedAt;

  @Column(name = "finalized_at", nullable = false)
  private Instant finalizedAt;

  protected VotingResultJpaEntity() {}

  private VotingResultJpaEntity(
      UUID votingSessionId,
      UUID agendaId,
      long yesVotes,
      long noVotes,
      VotingOutcome outcome,
      Instant closedAt,
      Instant finalizedAt) {
    this.votingSessionId = votingSessionId;
    this.agendaId = agendaId;
    this.yesVotes = yesVotes;
    this.noVotes = noVotes;
    this.outcome = outcome;
    this.closedAt = closedAt;
    this.finalizedAt = finalizedAt;
  }

  static VotingResultJpaEntity fromDomain(VotingResult result) {
    return new VotingResultJpaEntity(
        result.votingSessionId().value(),
        result.agendaId().value(),
        result.tally().yesVotes(),
        result.tally().noVotes(),
        result.outcome(),
        result.closedAt(),
        result.finalizedAt());
  }

  VotingResult toDomain() {
    return new VotingResult(
        new VotingSessionId(votingSessionId),
        new AgendaId(agendaId),
        new VoteTally(yesVotes, noVotes),
        outcome,
        closedAt,
        finalizedAt);
  }
}
