package dev.luan.sicredi.voting.service.port.in;

public interface PublishPendingResultEvents {

  int execute(int batchSize);
}
