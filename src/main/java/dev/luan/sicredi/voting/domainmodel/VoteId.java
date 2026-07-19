package dev.luan.sicredi.voting.domainmodel;

import java.util.Objects;
import java.util.UUID;

public record VoteId(UUID value) {

  public VoteId {
    Objects.requireNonNull(value, "Vote ID must not be null");
  }

  public static VoteId newId() {
    return new VoteId(UUID.randomUUID());
  }
}
