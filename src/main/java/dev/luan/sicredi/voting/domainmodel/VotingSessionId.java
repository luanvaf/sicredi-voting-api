package dev.luan.sicredi.voting.domainmodel;

import java.util.Objects;
import java.util.UUID;

public record VotingSessionId(UUID value) {

  public VotingSessionId {
    Objects.requireNonNull(value, "Voting session ID must not be null");
  }

  public static VotingSessionId newId() {
    return new VotingSessionId(UUID.randomUUID());
  }

  public static VotingSessionId from(String value) {
    return new VotingSessionId(UUID.fromString(value));
  }
}
