package dev.luan.sicredi.voting.service.port.out;

public final class DuplicateVoteException extends RuntimeException {

  public DuplicateVoteException() {
    super("The associate already has a vote in this voting session");
  }

  public DuplicateVoteException(Throwable cause) {
    super("The associate already has a vote in this voting session", cause);
  }
}
