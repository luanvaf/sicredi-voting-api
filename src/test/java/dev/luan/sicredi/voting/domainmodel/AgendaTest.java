package dev.luan.sicredi.voting.domainmodel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class AgendaTest {

  @Test
  void shouldNormalizeTextFields() {
    Agenda agenda =
        new Agenda(
            AgendaId.newId(),
            "  Annual budget  ",
            "  Approve next year's budget  ",
            Instant.parse("2026-07-19T12:00:00Z"));

    assertThat(agenda.title()).isEqualTo("Annual budget");
    assertThat(agenda.description()).isEqualTo("Approve next year's budget");
  }

  @Test
  void shouldNormalizeABlankDescriptionToNull() {
    Agenda agenda =
        new Agenda(AgendaId.newId(), "Annual budget", "   ", Instant.parse("2026-07-19T12:00:00Z"));

    assertThat(agenda.description()).isNull();
  }

  @Test
  void shouldRejectABlankTitle() {
    assertThatIllegalArgumentException()
        .isThrownBy(
            () -> new Agenda(AgendaId.newId(), "   ", null, Instant.parse("2026-07-19T12:00:00Z")))
        .withMessage("Agenda title must not be blank");
  }
}
