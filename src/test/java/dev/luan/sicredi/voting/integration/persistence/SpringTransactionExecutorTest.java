package dev.luan.sicredi.voting.integration.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import dev.luan.sicredi.voting.service.error.PersistenceUnavailableException;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

class SpringTransactionExecutorTest {

  private final PlatformTransactionManager transactionManager =
      mock(PlatformTransactionManager.class);

  @Test
  void shouldExecuteAndCommitTheOperation() {
    TransactionStatus status = new SimpleTransactionStatus();
    when(transactionManager.getTransaction(any(TransactionDefinition.class))).thenReturn(status);
    SpringTransactionExecutor executor = new SpringTransactionExecutor(transactionManager);

    String result = executor.required(() -> "committed");

    assertThat(result).isEqualTo("committed");
    verify(transactionManager).commit(status);
  }

  @Test
  void shouldTranslateTransactionInfrastructureFailures() {
    CannotCreateTransactionException failure =
        new CannotCreateTransactionException("database unavailable");
    when(transactionManager.getTransaction(any(TransactionDefinition.class))).thenThrow(failure);
    SpringTransactionExecutor executor = new SpringTransactionExecutor(transactionManager);
    @SuppressWarnings("unchecked")
    Supplier<String> operation = mock(Supplier.class);

    assertThatThrownBy(() -> executor.required(operation))
        .isInstanceOf(PersistenceUnavailableException.class)
        .hasMessage("Voting persistence is unavailable")
        .hasCause(failure);
    verifyNoInteractions(operation);
  }

  @Test
  void shouldPreserveOperationFailures() {
    TransactionStatus status = new SimpleTransactionStatus();
    when(transactionManager.getTransaction(any(TransactionDefinition.class))).thenReturn(status);
    SpringTransactionExecutor executor = new SpringTransactionExecutor(transactionManager);
    IllegalStateException failure = new IllegalStateException("business failure");

    assertThatThrownBy(
            () ->
                executor.required(
                    () -> {
                      throw failure;
                    }))
        .isSameAs(failure);
    verify(transactionManager).rollback(status);
  }

  @Test
  void shouldRequireDependenciesAndOperations() {
    assertThatNullPointerException()
        .isThrownBy(() -> new SpringTransactionExecutor(null))
        .withMessage("Transaction manager must not be null");

    SpringTransactionExecutor executor = new SpringTransactionExecutor(transactionManager);
    assertThatNullPointerException()
        .isThrownBy(() -> executor.required(null))
        .withMessage("Transactional operation must not be null");
  }
}
