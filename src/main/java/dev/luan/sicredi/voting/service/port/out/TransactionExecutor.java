package dev.luan.sicredi.voting.service.port.out;

import java.util.function.Supplier;

public interface TransactionExecutor {

  <T> T required(Supplier<T> operation);
}
