package dev.luan.sicredi.voting.bootstrap;

import java.net.URI;
import java.time.Duration;
import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.eligibility")
public record EligibilityProperties(URI baseUrl, Duration connectTimeout, Duration readTimeout) {

  public EligibilityProperties {
    Objects.requireNonNull(baseUrl, "Eligibility base URL must not be null");
    requirePositive(connectTimeout, "Eligibility connect timeout");
    requirePositive(readTimeout, "Eligibility read timeout");
  }

  private static void requirePositive(Duration duration, String field) {
    Objects.requireNonNull(duration, field + " must not be null");
    if (duration.isZero() || duration.isNegative()) {
      throw new IllegalArgumentException(field + " must be positive");
    }
  }
}
