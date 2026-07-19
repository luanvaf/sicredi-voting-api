package dev.luan.sicredi.voting.service.error;

public final class EligibilityServiceUnavailableException extends VotingApplicationException {

  public EligibilityServiceUnavailableException(Throwable cause) {
    super(
        "ELIGIBILITY_SERVICE_UNAVAILABLE",
        "The associate eligibility provider is unavailable",
        cause);
  }
}
