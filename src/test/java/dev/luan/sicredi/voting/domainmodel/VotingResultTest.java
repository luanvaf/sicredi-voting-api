package dev.luan.sicredi.voting.domainmodel;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class VotingResultTest {

  @Test
  void shouldRejectFinalizationBeforeTheSessionClosingTimestamp() {
    Instant closedAt = Instant.parse("2026-07-19T12:00:00Z");

    assertThatIllegalArgumentException()
        .isThrownBy(
            () ->
                new VotingResult(
                    VotingSessionId.newId(),
                    AgendaId.newId(),
                    new VoteTally(1, 0),
                    VotingOutcome.APPROVED,
                    closedAt,
                    closedAt.minusNanos(1)))
        .withMessage(
            "Voting result finalization timestamp must not be before its closing timestamp");
  }
}
