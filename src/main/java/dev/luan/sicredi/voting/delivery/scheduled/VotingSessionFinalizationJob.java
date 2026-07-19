package dev.luan.sicredi.voting.delivery.scheduled;

import dev.luan.sicredi.voting.service.error.VotingApplicationException;
import dev.luan.sicredi.voting.service.port.in.FinalizeExpiredVotingSessions;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class VotingSessionFinalizationJob {

  private static final Logger LOGGER = LoggerFactory.getLogger(VotingSessionFinalizationJob.class);

  private final FinalizeExpiredVotingSessions finalizeExpiredVotingSessions;
  private final int batchSize;

  public VotingSessionFinalizationJob(
      FinalizeExpiredVotingSessions finalizeExpiredVotingSessions,
      @Value("${app.voting.closure-batch-size}") int batchSize) {
    this.finalizeExpiredVotingSessions =
        Objects.requireNonNull(
            finalizeExpiredVotingSessions,
            "Finalize expired voting sessions use case must not be null");
    if (batchSize < 1) {
      throw new IllegalArgumentException("Closure batch size must be positive");
    }
    this.batchSize = batchSize;
  }

  @Scheduled(fixedDelayString = "${app.voting.closure-poll-delay}")
  public void finalizeExpiredSessions() {
    try {
      int finalizedSessions = finalizeExpiredVotingSessions.execute(batchSize);
      if (finalizedSessions > 0) {
        LOGGER.info("Finalized {} expired voting sessions", finalizedSessions);
      }
    } catch (VotingApplicationException exception) {
      LOGGER.error(
          "Voting session finalization failed with code {} and type {}",
          exception.errorCode(),
          exception.getClass().getSimpleName());
    } catch (RuntimeException exception) {
      LOGGER.error(
          "Voting session finalization failed with type {}", exception.getClass().getSimpleName());
    }
  }
}
