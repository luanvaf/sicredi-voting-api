package dev.luan.sicredi.voting.service.port.out;

public final class DuplicateVotingSessionException extends RuntimeException {

  public DuplicateVotingSessionException(Throwable cause) {
    super("The agenda already has a voting session", cause);
  }
}
