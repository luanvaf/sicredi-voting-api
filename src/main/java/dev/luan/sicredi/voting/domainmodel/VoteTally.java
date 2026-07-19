package dev.luan.sicredi.voting.domainmodel;

public record VoteTally(long yesVotes, long noVotes) {

  public VoteTally {
    if (yesVotes < 0 || noVotes < 0) {
      throw new IllegalArgumentException("Vote counts must not be negative");
    }
    Math.addExact(yesVotes, noVotes);
  }

  public long totalVotes() {
    return Math.addExact(yesVotes, noVotes);
  }
}
