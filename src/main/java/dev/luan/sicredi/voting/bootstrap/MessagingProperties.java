package dev.luan.sicredi.voting.bootstrap;

import java.time.Duration;
import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.messaging")
public record MessagingProperties(
    String resultExchange,
    String resultRoutingKey,
    String resultQueue,
    Duration relayPollDelay,
    int relayBatchSize,
    Duration claimLease,
    Duration initialRetryDelay,
    Duration maximumRetryDelay,
    Duration confirmTimeout) {

  public MessagingProperties {
    resultExchange = requireText(resultExchange, "Result exchange");
    resultRoutingKey = requireText(resultRoutingKey, "Result routing key");
    resultQueue = requireText(resultQueue, "Result queue");
    requirePositive(relayPollDelay, "Relay poll delay");
    if (relayBatchSize < 1) {
      throw new IllegalArgumentException("Relay batch size must be positive");
    }
    requirePositive(claimLease, "Outbox claim lease");
    requirePositive(initialRetryDelay, "Initial retry delay");
    requirePositive(maximumRetryDelay, "Maximum retry delay");
    requirePositive(confirmTimeout, "Publisher confirm timeout");
    if (initialRetryDelay.compareTo(maximumRetryDelay) > 0) {
      throw new IllegalArgumentException("Initial retry delay must not exceed maximum retry delay");
    }
    if (claimLease.compareTo(confirmTimeout) <= 0) {
      throw new IllegalArgumentException(
          "Outbox claim lease must exceed publisher confirm timeout");
    }
  }

  private static String requireText(String value, String field) {
    Objects.requireNonNull(value, field + " must not be null");
    if (value.isBlank()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
    return value;
  }

  private static void requirePositive(Duration value, String field) {
    Objects.requireNonNull(value, field + " must not be null");
    if (value.isZero() || value.isNegative()) {
      throw new IllegalArgumentException(field + " must be positive");
    }
  }
}
