package dev.luan.sicredi.voting.service.error;

public final class PersistenceUnavailableException extends VotingApplicationException {

  public PersistenceUnavailableException(Throwable cause) {
    super("PERSISTENCE_UNAVAILABLE", "Voting persistence is unavailable", cause);
  }
}
