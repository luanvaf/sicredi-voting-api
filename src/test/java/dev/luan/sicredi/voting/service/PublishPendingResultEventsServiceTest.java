package dev.luan.sicredi.voting.service;

import static dev.luan.sicredi.voting.service.port.out.ResultEventPublicationException.FailureCode.BROKER_NACK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.luan.sicredi.voting.service.port.out.ResultEventPublicationException;
import dev.luan.sicredi.voting.service.port.out.ResultEventPublisher;
import dev.luan.sicredi.voting.service.port.out.TransactionExecutor;
import dev.luan.sicredi.voting.service.port.out.VotingResultOutbox;
import dev.luan.sicredi.voting.service.port.out.VotingResultOutbox.ClaimedResultEvent;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

class PublishPendingResultEventsServiceTest {

  private static final Instant NOW = Instant.parse("2026-07-19T12:00:00Z");
  private static final Duration LEASE_DURATION = Duration.ofSeconds(30);
  private static final Duration INITIAL_RETRY_DELAY = Duration.ofSeconds(2);
  private static final Duration MAXIMUM_RETRY_DELAY = Duration.ofSeconds(30);

  private final VotingResultOutbox outbox = mock(VotingResultOutbox.class);
  private final ResultEventPublisher publisher = mock(ResultEventPublisher.class);
  private final TrackingTransactionExecutor transactionExecutor = new TrackingTransactionExecutor();
  private final PublishPendingResultEventsService service =
      new PublishPendingResultEventsService(
          outbox,
          publisher,
          transactionExecutor,
          Clock.fixed(NOW, ZoneOffset.UTC),
          LEASE_DURATION,
          INITIAL_RETRY_DELAY,
          MAXIMUM_RETRY_DELAY);

  @Test
  void shouldPublishOutsideTheDatabaseTransactionAndMarkOnlyAfterConfirmation() {
    ClaimedResultEvent event = claimedEvent(1);
    when(outbox.claimPending(NOW, 10, LEASE_DURATION)).thenReturn(List.of(event));
    when(outbox.renewLease(event, NOW.plus(LEASE_DURATION))).thenReturn(true);
    when(outbox.markPublished(event, NOW)).thenReturn(true);
    doAnswer(
            invocation -> {
              assertThat(transactionExecutor.isInTransaction()).isFalse();
              return null;
            })
        .when(publisher)
        .publish(event);

    int publishedEvents = service.execute(10);

    assertThat(publishedEvents).isOne();
    verify(publisher).publish(event);
    verify(outbox).markPublished(event, NOW);
  }

  @Test
  void shouldScheduleAnExponentialRetryUsingOnlyTheSafeFailureCode() {
    ClaimedResultEvent event = claimedEvent(3);
    when(outbox.claimPending(NOW, 10, LEASE_DURATION)).thenReturn(List.of(event));
    when(outbox.renewLease(event, NOW.plus(LEASE_DURATION))).thenReturn(true);
    doThrow(new ResultEventPublicationException(BROKER_NACK)).when(publisher).publish(event);

    int publishedEvents = service.execute(10);

    assertThat(publishedEvents).isZero();
    verify(outbox).scheduleRetry(event, NOW.plusSeconds(8), "BROKER_NACK");
    verify(outbox, never()).markPublished(event, NOW);
  }

  @Test
  void shouldCapTheExponentialRetryDelay() {
    ClaimedResultEvent event = claimedEvent(20);
    when(outbox.claimPending(NOW, 1, LEASE_DURATION)).thenReturn(List.of(event));
    when(outbox.renewLease(event, NOW.plus(LEASE_DURATION))).thenReturn(true);
    doThrow(new IllegalStateException("Potentially sensitive infrastructure detail"))
        .when(publisher)
        .publish(event);

    service.execute(1);

    verify(outbox)
        .scheduleRetry(event, NOW.plus(MAXIMUM_RETRY_DELAY), "UNEXPECTED_PUBLICATION_FAILURE");
  }

  @Test
  void shouldNotPublishWhenTheClaimLeaseWasLost() {
    ClaimedResultEvent event = claimedEvent(1);
    when(outbox.claimPending(NOW, 1, LEASE_DURATION)).thenReturn(List.of(event));
    when(outbox.renewLease(event, NOW.plus(LEASE_DURATION))).thenReturn(false);

    int publishedEvents = service.execute(1);

    assertThat(publishedEvents).isZero();
    verify(publisher, never()).publish(event);
  }

  @Test
  void shouldRejectANonPositiveBatchSize() {
    assertThatThrownBy(() -> service.execute(0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Publication batch size must be positive");
  }

  private static ClaimedResultEvent claimedEvent(int attemptNumber) {
    return new ClaimedResultEvent(
        UUID.randomUUID(),
        UUID.randomUUID(),
        "voting-result.finalized.v1",
        "{\"outcome\":\"APPROVED\"}",
        attemptNumber,
        NOW.plus(LEASE_DURATION));
  }

  private static final class TrackingTransactionExecutor implements TransactionExecutor {

    private boolean inTransaction;

    @Override
    public <T> T required(Supplier<T> operation) {
      assertThat(inTransaction).isFalse();
      inTransaction = true;
      try {
        return operation.get();
      } finally {
        inTransaction = false;
      }
    }

    boolean isInTransaction() {
      return inTransaction;
    }
  }
}
