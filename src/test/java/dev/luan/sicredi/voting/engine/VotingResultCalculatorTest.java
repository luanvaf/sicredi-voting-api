package dev.luan.sicredi.voting.engine;

import static org.assertj.core.api.Assertions.assertThat;

import dev.luan.sicredi.voting.domainmodel.VoteTally;
import dev.luan.sicredi.voting.domainmodel.VotingOutcome;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class VotingResultCalculatorTest {

  @ParameterizedTest
  @CsvSource({"2, 1, APPROVED", "1, 2, REJECTED", "1, 1, TIED", "0, 0, TIED"})
  void shouldDetermineTheOutcome(long yesVotes, long noVotes, VotingOutcome expectedOutcome) {
    VoteTally tally = new VoteTally(yesVotes, noVotes);

    assertThat(VotingResultCalculator.determineOutcome(tally)).isEqualTo(expectedOutcome);
  }
}
