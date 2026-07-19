package dev.luan.sicredi.voting.integration.persistence;

import dev.luan.sicredi.voting.service.port.out.TransactionExecutor;
import java.util.Objects;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class SpringTransactionExecutor implements TransactionExecutor {

  private final TransactionTemplate transactionTemplate;

  public SpringTransactionExecutor(PlatformTransactionManager transactionManager) {
    Objects.requireNonNull(transactionManager, "Transaction manager must not be null");
    this.transactionTemplate = new TransactionTemplate(transactionManager);
  }

  @Override
  public <T> T required(Supplier<T> operation) {
    Objects.requireNonNull(operation, "Transactional operation must not be null");
    return transactionTemplate.execute(status -> operation.get());
  }
}
