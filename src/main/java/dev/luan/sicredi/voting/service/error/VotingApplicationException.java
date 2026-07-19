package dev.luan.sicredi.voting.service.error;

import java.util.Objects;

public abstract class VotingApplicationException extends RuntimeException {

  private final String errorCode;

  protected VotingApplicationException(String errorCode, String message) {
    super(message);
    this.errorCode = Objects.requireNonNull(errorCode, "Error code must not be null");
  }

  protected VotingApplicationException(String errorCode, String message, Throwable cause) {
    super(message, cause);
    this.errorCode = Objects.requireNonNull(errorCode, "Error code must not be null");
  }

  public String errorCode() {
    return errorCode;
  }
}
