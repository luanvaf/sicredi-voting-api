package dev.luan.sicredi.voting.service;

import dev.luan.sicredi.voting.service.port.in.PublishPendingResultEvents;
import dev.luan.sicredi.voting.service.port.out.ResultEventPublicationException;
import dev.luan.sicredi.voting.service.port.out.ResultEventPublisher;
import dev.luan.sicredi.voting.service.port.out.TransactionExecutor;
import dev.luan.sicredi.voting.service.port.out.VotingResultOutbox;
import dev.luan.sicredi.voting.service.port.out.VotingResultOutbox.ClaimedResultEvent;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public final class PublishPendingResultEventsService implements PublishPendingResultEvents {

  private static final String UNEXPECTED_FAILURE_CODE = "UNEXPECTED_PUBLICATION_FAILURE";

  private final VotingResultOutbox outbox;
  private final ResultEventPublisher publisher;
  private final TransactionExecutor transactionExecutor;
  private final Clock clock;
  private final Duration leaseDuration;
  private final Duration initialRetryDelay;
  private final Duration maximumRetryDelay;

  public PublishPendingResultEventsService(
      VotingResultOutbox outbox,
      ResultEventPublisher publisher,
      TransactionExecutor transactionExecutor,
      Clock clock,
      Duration leaseDuration,
      Duration initialRetryDelay,
      Duration maximumRetryDelay) {
    this.outbox = Objects.requireNonNull(outbox, "Voting result outbox must not be null");
    this.publisher = Objects.requireNonNull(publisher, "Result event publisher must not be null");
    this.transactionExecutor =
        Objects.requireNonNull(transactionExecutor, "Transaction executor must not be null");
    this.clock = Objects.requireNonNull(clock, "Clock must not be null");
    this.leaseDuration = requirePositive(leaseDuration, "Outbox lease duration");
    this.initialRetryDelay = requirePositive(initialRetryDelay, "Initial retry delay");
    this.maximumRetryDelay = requirePositive(maximumRetryDelay, "Maximum retry delay");
    if (initialRetryDelay.compareTo(maximumRetryDelay) > 0) {
      throw new IllegalArgumentException("Initial retry delay must not exceed maximum retry delay");
    }
  }

  @Override
  public int execute(int batchSize) {
    if (batchSize < 1) {
      throw new IllegalArgumentException("Publication batch size must be positive");
    }

    Instant claimedAt = Instant.now(clock);
    List<ClaimedResultEvent> claimedEvents =
        transactionExecutor.required(
            () -> outbox.claimPending(claimedAt, batchSize, leaseDuration));

    int publishedEvents = 0;
    for (ClaimedResultEvent claimedEvent : claimedEvents) {
      ClaimedResultEvent renewedEvent = renewLease(claimedEvent);
      if (renewedEvent != null && publish(renewedEvent)) {
        publishedEvents++;
      }
    }
    return publishedEvents;
  }

  private ClaimedResultEvent renewLease(ClaimedResultEvent event) {
    Instant newLeaseUntil = Instant.now(clock).plus(leaseDuration);
    boolean renewed = transactionExecutor.required(() -> outbox.renewLease(event, newLeaseUntil));
    return renewed ? event.withLeaseUntil(newLeaseUntil) : null;
  }

  private boolean publish(ClaimedResultEvent event) {
    try {
      publisher.publish(event);
    } catch (ResultEventPublicationException exception) {
      scheduleRetry(event, exception.safeErrorCode());
      return false;
    } catch (RuntimeException exception) {
      scheduleRetry(event, UNEXPECTED_FAILURE_CODE);
      return false;
    }

    Instant publishedAt = Instant.now(clock);

    // A broker acknowledgement necessarily precedes this database update. A process crash in
    // between can cause a duplicate delivery, which is an accepted at-least-once trade-off.
    return transactionExecutor.required(() -> outbox.markPublished(event, publishedAt));
  }

  private void scheduleRetry(ClaimedResultEvent event, String safeErrorCode) {
    Instant failedAt = Instant.now(clock);
    Instant nextAttemptAt = failedAt.plus(retryDelay(event.attemptNumber()));
    transactionExecutor.required(() -> outbox.scheduleRetry(event, nextAttemptAt, safeErrorCode));
  }

  private Duration retryDelay(int attemptNumber) {
    Duration delay = initialRetryDelay;
    for (int attempt = 1;
        attempt < attemptNumber && delay.compareTo(maximumRetryDelay) < 0;
        attempt++) {
      if (delay.compareTo(maximumRetryDelay.dividedBy(2)) > 0) {
        return maximumRetryDelay;
      }
      delay = delay.multipliedBy(2);
    }
    return delay.compareTo(maximumRetryDelay) > 0 ? maximumRetryDelay : delay;
  }

  private static Duration requirePositive(Duration value, String fieldName) {
    Objects.requireNonNull(value, fieldName + " must not be null");
    if (value.isZero() || value.isNegative()) {
      throw new IllegalArgumentException(fieldName + " must be positive");
    }
    return value;
  }
}
