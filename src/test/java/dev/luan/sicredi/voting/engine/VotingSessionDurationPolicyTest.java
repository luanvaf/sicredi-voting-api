package dev.luan.sicredi.voting.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class VotingSessionDurationPolicyTest {

  private final VotingSessionDurationPolicy policy =
      new VotingSessionDurationPolicy(Duration.ofMinutes(1), Duration.ofHours(24));

  @Test
  void shouldUseTheDefaultDurationWhenNoneIsRequested() {
    assertThat(policy.resolve(Optional.empty())).isEqualTo(Duration.ofMinutes(1));
  }

  @Test
  void shouldUseAValidRequestedDuration() {
    assertThat(policy.resolve(Optional.of(Duration.ofMinutes(5)))).isEqualTo(Duration.ofMinutes(5));
  }

  @Test
  void shouldRejectADurationBelowOneSecond() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> policy.resolve(Optional.of(Duration.ofMillis(999))))
        .withMessage("Voting session duration must be between 1 and 86400 seconds");
  }

  @Test
  void shouldRejectADurationAboveTheMaximum() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> policy.resolve(Optional.of(Duration.ofHours(25))))
        .withMessage("Voting session duration must be between 1 and 86400 seconds");
  }
}
