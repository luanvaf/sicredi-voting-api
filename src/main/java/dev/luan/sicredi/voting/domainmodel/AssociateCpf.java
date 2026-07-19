package dev.luan.sicredi.voting.domainmodel;

import java.util.Objects;

public record AssociateCpf(String value) {

  private static final int CPF_LENGTH = 11;

  public AssociateCpf {
    Objects.requireNonNull(value, "Associate CPF must not be null");
    if (!isValid(value)) {
      throw new IllegalArgumentException("Associate CPF must be a valid unformatted CPF");
    }
  }

  public String masked() {
    return "***.***.***-" + value.substring(value.length() - 2);
  }

  @Override
  public String toString() {
    return masked();
  }

  private static boolean isValid(String value) {
    if (!value.matches("\\d{" + CPF_LENGTH + "}")) {
      return false;
    }
    if (value.chars().allMatch(character -> character == value.charAt(0))) {
      return false;
    }

    int firstDigit = calculateCheckDigit(value, 9, 10);
    int secondDigit = calculateCheckDigit(value, 10, 11);

    return firstDigit == Character.digit(value.charAt(9), 10)
        && secondDigit == Character.digit(value.charAt(10), 10);
  }

  private static int calculateCheckDigit(String value, int digitCount, int initialWeight) {
    int sum = 0;
    for (int index = 0; index < digitCount; index++) {
      sum += Character.digit(value.charAt(index), 10) * (initialWeight - index);
    }

    int digit = 11 - (sum % 11);
    return digit >= 10 ? 0 : digit;
  }
}
