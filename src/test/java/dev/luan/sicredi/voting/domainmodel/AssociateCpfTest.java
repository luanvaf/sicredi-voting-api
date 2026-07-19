package dev.luan.sicredi.voting.domainmodel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

class AssociateCpfTest {

  @Test
  void shouldAcceptAValidCpf() {
    AssociateCpf cpf = new AssociateCpf("19839091069");

    assertThat(cpf.value()).isEqualTo("19839091069");
    assertThat(cpf.masked()).isEqualTo("***.***.***-69");
    assertThat(cpf).hasToString("***.***.***-69");
  }

  @Test
  void shouldRejectAFormattedCpf() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new AssociateCpf("198.390.910-69"))
        .withMessage("Associate CPF must be a valid unformatted CPF");
  }

  @Test
  void shouldRejectAnInvalidCheckDigit() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new AssociateCpf("19839091068"))
        .withMessage("Associate CPF must be a valid unformatted CPF");
  }

  @Test
  void shouldRejectRepeatedDigits() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new AssociateCpf("11111111111"))
        .withMessage("Associate CPF must be a valid unformatted CPF");
  }
}
