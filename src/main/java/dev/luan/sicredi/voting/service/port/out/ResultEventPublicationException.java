package dev.luan.sicredi.voting.service.port.out;

import java.util.Objects;

public final class ResultEventPublicationException extends RuntimeException {

  private final FailureCode failureCode;

  public ResultEventPublicationException(FailureCode failureCode) {
    super(messageFor(failureCode));
    this.failureCode = Objects.requireNonNull(failureCode, "Failure code must not be null");
  }

  public ResultEventPublicationException(FailureCode failureCode, Throwable cause) {
    super(messageFor(failureCode), cause);
    this.failureCode = Objects.requireNonNull(failureCode, "Failure code must not be null");
  }

  public String safeErrorCode() {
    return failureCode.name();
  }

  public enum FailureCode {
    BROKER_UNAVAILABLE,
    BROKER_NACK,
    BROKER_CONFIRM_TIMEOUT,
    UNROUTABLE_MESSAGE,
    PUBLICATION_INTERRUPTED
  }

  private static String messageFor(FailureCode failureCode) {
    Objects.requireNonNull(failureCode, "Failure code must not be null");
    return "Result event publication failed: " + failureCode.name();
  }
}
