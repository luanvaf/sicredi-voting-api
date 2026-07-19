package dev.luan.sicredi.voting.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.time.Duration;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class RuntimePropertiesTest {

  private static final URI ELIGIBILITY_BASE_URL = URI.create("https://eligibility.example.test");

  @Test
  void shouldAcceptValidRuntimeProperties() {
    EligibilityProperties eligibility =
        new EligibilityProperties(
            ELIGIBILITY_BASE_URL, Duration.ofSeconds(2), Duration.ofSeconds(3));
    VotingProperties voting =
        new VotingProperties(
            Duration.ofMinutes(1), Duration.ofHours(24), Duration.ofSeconds(1), 100);
    MessagingProperties messaging = validMessagingProperties();

    assertThat(eligibility.baseUrl()).isEqualTo(ELIGIBILITY_BASE_URL);
    assertThat(voting.closureBatchSize()).isEqualTo(100);
    assertThat(messaging.resultExchange()).isEqualTo("voting.results");
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("invalidRuntimeProperties")
  void shouldRejectInvalidRuntimeProperties(
      String scenario,
      Class<? extends Throwable> exceptionType,
      String message,
      Executable constructor) {
    assertThatThrownBy(constructor::execute).isInstanceOf(exceptionType).hasMessage(message);
  }

  private static Stream<Arguments> invalidRuntimeProperties() {
    return Stream.of(
        invalid(
            "missing eligibility URL",
            NullPointerException.class,
            "Eligibility base URL must not be null",
            () -> new EligibilityProperties(null, Duration.ofSeconds(2), Duration.ofSeconds(3))),
        invalid(
            "missing eligibility timeout",
            NullPointerException.class,
            "Eligibility connect timeout must not be null",
            () -> new EligibilityProperties(ELIGIBILITY_BASE_URL, null, Duration.ofSeconds(3))),
        invalid(
            "zero eligibility timeout",
            IllegalArgumentException.class,
            "Eligibility connect timeout must be positive",
            () ->
                new EligibilityProperties(
                    ELIGIBILITY_BASE_URL, Duration.ZERO, Duration.ofSeconds(3))),
        invalid(
            "negative eligibility timeout",
            IllegalArgumentException.class,
            "Eligibility read timeout must be positive",
            () ->
                new EligibilityProperties(
                    ELIGIBILITY_BASE_URL, Duration.ofSeconds(2), Duration.ofSeconds(-1))),
        invalid(
            "missing voting duration",
            NullPointerException.class,
            "Default session duration must not be null",
            () -> new VotingProperties(null, Duration.ofHours(24), Duration.ofSeconds(1), 100)),
        invalid(
            "zero voting duration",
            IllegalArgumentException.class,
            "Default session duration must be positive",
            () ->
                new VotingProperties(
                    Duration.ZERO, Duration.ofHours(24), Duration.ofSeconds(1), 100)),
        invalid(
            "negative voting duration",
            IllegalArgumentException.class,
            "Maximum session duration must be positive",
            () ->
                new VotingProperties(
                    Duration.ofMinutes(1), Duration.ofSeconds(-1), Duration.ofSeconds(1), 100)),
        invalid(
            "default duration above maximum",
            IllegalArgumentException.class,
            "Default session duration must not exceed maximum session duration",
            () ->
                new VotingProperties(
                    Duration.ofHours(2), Duration.ofHours(1), Duration.ofSeconds(1), 100)),
        invalid(
            "invalid closure batch",
            IllegalArgumentException.class,
            "Closure batch size must be positive",
            () ->
                new VotingProperties(
                    Duration.ofMinutes(1), Duration.ofHours(24), Duration.ofSeconds(1), 0)),
        invalid(
            "missing exchange",
            NullPointerException.class,
            "Result exchange must not be null",
            () ->
                new MessagingProperties(
                    null,
                    "voting.session.closed",
                    "voting.results.platform",
                    Duration.ofSeconds(1),
                    20,
                    Duration.ofSeconds(30),
                    Duration.ofSeconds(2),
                    Duration.ofMinutes(5),
                    Duration.ofSeconds(5))),
        invalid(
            "blank routing key",
            IllegalArgumentException.class,
            "Result routing key must not be blank",
            () ->
                new MessagingProperties(
                    "voting.results",
                    " ",
                    "voting.results.platform",
                    Duration.ofSeconds(1),
                    20,
                    Duration.ofSeconds(30),
                    Duration.ofSeconds(2),
                    Duration.ofMinutes(5),
                    Duration.ofSeconds(5))),
        invalid(
            "invalid relay batch",
            IllegalArgumentException.class,
            "Relay batch size must be positive",
            () ->
                new MessagingProperties(
                    "voting.results",
                    "voting.session.closed",
                    "voting.results.platform",
                    Duration.ofSeconds(1),
                    0,
                    Duration.ofSeconds(30),
                    Duration.ofSeconds(2),
                    Duration.ofMinutes(5),
                    Duration.ofSeconds(5))),
        invalid(
            "zero relay delay",
            IllegalArgumentException.class,
            "Relay poll delay must be positive",
            () ->
                new MessagingProperties(
                    "voting.results",
                    "voting.session.closed",
                    "voting.results.platform",
                    Duration.ZERO,
                    20,
                    Duration.ofSeconds(30),
                    Duration.ofSeconds(2),
                    Duration.ofMinutes(5),
                    Duration.ofSeconds(5))),
        invalid(
            "negative retry delay",
            IllegalArgumentException.class,
            "Initial retry delay must be positive",
            () ->
                new MessagingProperties(
                    "voting.results",
                    "voting.session.closed",
                    "voting.results.platform",
                    Duration.ofSeconds(1),
                    20,
                    Duration.ofSeconds(30),
                    Duration.ofSeconds(-1),
                    Duration.ofMinutes(5),
                    Duration.ofSeconds(5))),
        invalid(
            "missing claim lease",
            NullPointerException.class,
            "Outbox claim lease must not be null",
            () ->
                new MessagingProperties(
                    "voting.results",
                    "voting.session.closed",
                    "voting.results.platform",
                    Duration.ofSeconds(1),
                    20,
                    null,
                    Duration.ofSeconds(2),
                    Duration.ofMinutes(5),
                    Duration.ofSeconds(5))),
        invalid(
            "initial retry above maximum",
            IllegalArgumentException.class,
            "Initial retry delay must not exceed maximum retry delay",
            () ->
                new MessagingProperties(
                    "voting.results",
                    "voting.session.closed",
                    "voting.results.platform",
                    Duration.ofSeconds(1),
                    20,
                    Duration.ofSeconds(30),
                    Duration.ofMinutes(2),
                    Duration.ofMinutes(1),
                    Duration.ofSeconds(5))),
        invalid(
            "claim lease equal to confirmation timeout",
            IllegalArgumentException.class,
            "Outbox claim lease must exceed publisher confirm timeout",
            () ->
                new MessagingProperties(
                    "voting.results",
                    "voting.session.closed",
                    "voting.results.platform",
                    Duration.ofSeconds(1),
                    20,
                    Duration.ofSeconds(5),
                    Duration.ofSeconds(2),
                    Duration.ofMinutes(5),
                    Duration.ofSeconds(5))));
  }

  private static MessagingProperties validMessagingProperties() {
    return new MessagingProperties(
        "voting.results",
        "voting.session.closed",
        "voting.results.platform",
        Duration.ofSeconds(1),
        20,
        Duration.ofSeconds(30),
        Duration.ofSeconds(2),
        Duration.ofMinutes(5),
        Duration.ofSeconds(5));
  }

  private static Arguments invalid(
      String scenario,
      Class<? extends Throwable> exceptionType,
      String message,
      Executable constructor) {
    return Arguments.of(scenario, exceptionType, message, constructor);
  }
}
