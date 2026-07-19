package dev.luan.sicredi.voting.engine;

import dev.luan.sicredi.voting.domainmodel.VotingSession;
import java.time.Instant;
import java.util.Objects;

public final class VotingSessionPolicy {

  private VotingSessionPolicy() {}

  public static boolean acceptsVote(VotingSession votingSession, Instant instant) {
    Objects.requireNonNull(votingSession, "Voting session must not be null");
    Objects.requireNonNull(instant, "Evaluation timestamp must not be null");
    return votingSession.isOpenAt(instant);
  }
}
