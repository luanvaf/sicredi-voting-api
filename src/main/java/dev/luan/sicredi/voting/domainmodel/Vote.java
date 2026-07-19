package dev.luan.sicredi.voting.domainmodel;

import java.time.Instant;
import java.util.Objects;

public record Vote(
    VoteId id,
    VotingSessionId votingSessionId,
    AssociateCpf associateCpf,
    VoteChoice choice,
    Instant castAt) {

  public Vote {
    Objects.requireNonNull(id, "Vote ID must not be null");
    Objects.requireNonNull(votingSessionId, "Voting session ID must not be null");
    Objects.requireNonNull(associateCpf, "Associate CPF must not be null");
    Objects.requireNonNull(choice, "Vote choice must not be null");
    Objects.requireNonNull(castAt, "Vote timestamp must not be null");
  }
}
