package dev.luan.sicredi.voting.service;

import dev.luan.sicredi.voting.service.port.out.TransactionExecutor;
import java.util.function.Supplier;

final class ImmediateTransactionExecutor implements TransactionExecutor {

  @Override
  public <T> T required(Supplier<T> operation) {
    return operation.get();
  }
}
