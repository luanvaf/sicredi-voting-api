package dev.luan.sicredi.voting.domainmodel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class VotingSessionTest {

  private static final Instant OPENED_AT = Instant.parse("2026-07-19T12:00:00Z");

  @Test
  void shouldAcceptVotesFromOpeningUntilBeforeClosing() {
    VotingSession session = VotingSession.open(AgendaId.newId(), OPENED_AT, Duration.ofMinutes(1));

    assertThat(session.isOpenAt(OPENED_AT)).isTrue();
    assertThat(session.isOpenAt(OPENED_AT.plusSeconds(59))).isTrue();
    assertThat(session.isOpenAt(OPENED_AT.plusSeconds(60))).isFalse();
  }

  @Test
  void shouldNotAcceptVotesBeforeOpening() {
    VotingSession session = VotingSession.open(AgendaId.newId(), OPENED_AT, Duration.ofMinutes(1));

    assertThat(session.isOpenAt(OPENED_AT.minusNanos(1))).isFalse();
  }

  @Test
  void shouldReportExpirationAtAndAfterClosing() {
    VotingSession session = VotingSession.open(AgendaId.newId(), OPENED_AT, Duration.ofMinutes(1));

    assertThat(session.isExpiredAt(OPENED_AT.plusSeconds(59))).isFalse();
    assertThat(session.isExpiredAt(OPENED_AT.plusSeconds(60))).isTrue();
    assertThat(session.isExpiredAt(OPENED_AT.plusSeconds(61))).isTrue();
  }

  @Test
  void shouldRejectANonIncreasingClosingTimestamp() {
    assertThatIllegalArgumentException()
        .isThrownBy(
            () ->
                new VotingSession(
                    VotingSessionId.newId(), AgendaId.newId(), OPENED_AT, OPENED_AT, null))
        .withMessage("Voting session closing timestamp must be after its opening timestamp");
  }

  @Test
  void shouldRejectAFinalizationBeforeClosing() {
    assertThatIllegalArgumentException()
        .isThrownBy(
            () ->
                new VotingSession(
                    VotingSessionId.newId(),
                    AgendaId.newId(),
                    OPENED_AT,
                    OPENED_AT.plusSeconds(60),
                    OPENED_AT.plusSeconds(59)))
        .withMessage(
            "Voting session finalization timestamp must not be before its closing timestamp");
  }

  @Test
  void shouldRejectZeroAndNegativeDurations() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> VotingSession.open(AgendaId.newId(), OPENED_AT, Duration.ZERO))
        .withMessage("Voting session duration must be positive");
    assertThatIllegalArgumentException()
        .isThrownBy(() -> VotingSession.open(AgendaId.newId(), OPENED_AT, Duration.ofSeconds(-1)))
        .withMessage("Voting session duration must be positive");
  }

  @Test
  void shouldRejectFinalizationBeforeClosing() {
    VotingSession session = VotingSession.open(AgendaId.newId(), OPENED_AT, Duration.ofMinutes(1));

    assertThatIllegalArgumentException()
        .isThrownBy(() -> session.finalizeAt(OPENED_AT.plusSeconds(59)))
        .withMessage("Voting session cannot be finalized before its closing timestamp");
  }

  @Test
  void shouldFinalizeIdempotently() {
    VotingSession session = VotingSession.open(AgendaId.newId(), OPENED_AT, Duration.ofMinutes(1));
    Instant finalizedAt = OPENED_AT.plusSeconds(61);

    VotingSession finalized = session.finalizeAt(finalizedAt);

    assertThat(finalized.isFinalized()).isTrue();
    assertThat(finalized.finalizedAt()).isEqualTo(finalizedAt);
    assertThat(finalized.finalizeAt(finalizedAt.plusSeconds(1))).isSameAs(finalized);
  }
}
