package dev.luan.sicredi.voting.domainmodel;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class IdentifierTest {

  private static final String VALUE = "ad2ad611-43b2-4746-b421-5b09ac2e70d0";

  @Test
  void shouldParseAgendaIdentifiers() {
    assertThat(AgendaId.from(VALUE).value()).isEqualTo(UUID.fromString(VALUE));
  }

  @Test
  void shouldParseVotingSessionIdentifiers() {
    assertThat(VotingSessionId.from(VALUE).value()).isEqualTo(UUID.fromString(VALUE));
  }
}
