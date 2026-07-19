package dev.luan.sicredi.voting.engine;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

public record VotingSessionDurationPolicy(Duration defaultDuration, Duration maximumDuration) {

  private static final Duration MINIMUM_DURATION = Duration.ofSeconds(1);

  public VotingSessionDurationPolicy {
    Objects.requireNonNull(defaultDuration, "Default session duration must not be null");
    Objects.requireNonNull(maximumDuration, "Maximum session duration must not be null");
    requireInRange(defaultDuration, maximumDuration);
  }

  public Duration resolve(Optional<Duration> requestedDuration) {
    Objects.requireNonNull(requestedDuration, "Requested session duration must not be null");
    Duration resolvedDuration = requestedDuration.orElse(defaultDuration);
    requireInRange(resolvedDuration, maximumDuration);
    return resolvedDuration;
  }

  private static void requireInRange(Duration duration, Duration maximumDuration) {
    if (duration.compareTo(MINIMUM_DURATION) < 0 || duration.compareTo(maximumDuration) > 0) {
      throw new IllegalArgumentException(
          "Voting session duration must be between "
              + MINIMUM_DURATION.toSeconds()
              + " and "
              + maximumDuration.toSeconds()
              + " seconds");
    }
  }
}
