package dev.luan.sicredi.voting.delivery.scheduled;

import dev.luan.sicredi.voting.service.error.VotingApplicationException;
import dev.luan.sicredi.voting.service.port.in.PublishPendingResultEvents;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class VotingResultOutboxRelayJob {

  private static final Logger LOGGER = LoggerFactory.getLogger(VotingResultOutboxRelayJob.class);

  private final PublishPendingResultEvents publishPendingResultEvents;
  private final int batchSize;

  public VotingResultOutboxRelayJob(
      PublishPendingResultEvents publishPendingResultEvents,
      @Value("${app.messaging.relay-batch-size}") int batchSize) {
    this.publishPendingResultEvents =
        Objects.requireNonNull(
            publishPendingResultEvents, "Publish pending result events use case must not be null");
    if (batchSize < 1) {
      throw new IllegalArgumentException("Relay batch size must be positive");
    }
    this.batchSize = batchSize;
  }

  @Scheduled(fixedDelayString = "${app.messaging.relay-poll-delay}")
  public void publishPendingEvents() {
    try {
      int publishedEvents = publishPendingResultEvents.execute(batchSize);
      if (publishedEvents > 0) {
        LOGGER.info("Published {} voting result events", publishedEvents);
      }
    } catch (VotingApplicationException exception) {
      LOGGER.error(
          "Voting result relay failed with code {} and type {}",
          exception.errorCode(),
          exception.getClass().getSimpleName());
    } catch (RuntimeException exception) {
      LOGGER.error("Voting result relay failed with type {}", exception.getClass().getSimpleName());
    }
  }
}
