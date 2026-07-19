package dev.luan.sicredi.voting.bootstrap;

import java.time.Duration;
import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.voting")
public record VotingProperties(
    Duration defaultSessionDuration,
    Duration maximumSessionDuration,
    Duration closurePollDelay,
    int closureBatchSize) {

  public VotingProperties {
    requirePositive(defaultSessionDuration, "Default session duration");
    requirePositive(maximumSessionDuration, "Maximum session duration");
    requirePositive(closurePollDelay, "Closure poll delay");
    if (defaultSessionDuration.compareTo(maximumSessionDuration) > 0) {
      throw new IllegalArgumentException(
          "Default session duration must not exceed maximum session duration");
    }
    if (closureBatchSize < 1) {
      throw new IllegalArgumentException("Closure batch size must be positive");
    }
  }

  private static void requirePositive(Duration value, String field) {
    Objects.requireNonNull(value, field + " must not be null");
    if (value.isZero() || value.isNegative()) {
      throw new IllegalArgumentException(field + " must be positive");
    }
  }
}
