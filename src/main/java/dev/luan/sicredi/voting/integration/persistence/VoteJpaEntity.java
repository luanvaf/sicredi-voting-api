package dev.luan.sicredi.voting.integration.persistence;

import dev.luan.sicredi.voting.domainmodel.AssociateCpf;
import dev.luan.sicredi.voting.domainmodel.Vote;
import dev.luan.sicredi.voting.domainmodel.VoteChoice;
import dev.luan.sicredi.voting.domainmodel.VoteId;
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
@Table(name = "votes")
class VoteJpaEntity {

  @Id private UUID id;

  @Column(name = "voting_session_id", nullable = false)
  private UUID votingSessionId;

  @Column(name = "associate_cpf", nullable = false, length = 11)
  private String associateCpf;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 3)
  private VoteChoice choice;

  @Column(name = "cast_at", nullable = false)
  private Instant castAt;

  protected VoteJpaEntity() {}

  private VoteJpaEntity(
      UUID id, UUID votingSessionId, String associateCpf, VoteChoice choice, Instant castAt) {
    this.id = id;
    this.votingSessionId = votingSessionId;
    this.associateCpf = associateCpf;
    this.choice = choice;
    this.castAt = castAt;
  }

  static VoteJpaEntity fromDomain(Vote vote) {
    return new VoteJpaEntity(
        vote.id().value(),
        vote.votingSessionId().value(),
        vote.associateCpf().value(),
        vote.choice(),
        vote.castAt());
  }

  Vote toDomain() {
    return new Vote(
        new VoteId(id),
        new VotingSessionId(votingSessionId),
        new AssociateCpf(associateCpf),
        choice,
        castAt);
  }
}
