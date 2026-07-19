package dev.luan.sicredi.voting.domainmodel;

import java.time.Instant;
import java.util.Objects;

public record Agenda(AgendaId id, String title, String description, Instant createdAt) {

  public static final int MAXIMUM_TITLE_LENGTH = 200;
  public static final int MAXIMUM_DESCRIPTION_LENGTH = 1000;

  public Agenda {
    Objects.requireNonNull(id, "Agenda ID must not be null");
    Objects.requireNonNull(title, "Agenda title must not be null");
    Objects.requireNonNull(createdAt, "Agenda creation timestamp must not be null");

    title = title.strip();
    if (title.isEmpty()) {
      throw new IllegalArgumentException("Agenda title must not be blank");
    }
    if (title.length() > MAXIMUM_TITLE_LENGTH) {
      throw new IllegalArgumentException(
          "Agenda title must not exceed " + MAXIMUM_TITLE_LENGTH + " characters");
    }

    if (description != null) {
      description = description.strip();
      if (description.isEmpty()) {
        description = null;
      } else if (description.length() > MAXIMUM_DESCRIPTION_LENGTH) {
        throw new IllegalArgumentException(
            "Agenda description must not exceed " + MAXIMUM_DESCRIPTION_LENGTH + " characters");
      }
    }
  }
}
