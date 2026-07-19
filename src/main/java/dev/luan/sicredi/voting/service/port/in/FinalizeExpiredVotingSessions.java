package dev.luan.sicredi.voting.service.port.in;

public interface FinalizeExpiredVotingSessions {

  int execute(int batchSize);
}
