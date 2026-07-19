package dev.luan.sicredi.voting.domainmodel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class VoteTallyTest {

  @Test
  void shouldCalculateTheTotalVoteCount() {
    assertThat(new VoteTally(7, 4).totalVotes()).isEqualTo(11);
  }

  @Test
  void shouldRejectNegativeVoteCounts() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new VoteTally(-1, 0))
        .withMessage("Vote counts must not be negative");
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new VoteTally(0, -1))
        .withMessage("Vote counts must not be negative");
  }

  @Test
  void shouldRejectAnOverflowingTotal() {
    assertThatThrownBy(() -> new VoteTally(Long.MAX_VALUE, 1))
        .isInstanceOf(ArithmeticException.class);
  }
}
