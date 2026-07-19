package dev.luan.sicredi.voting.service.port.in;

import dev.luan.sicredi.voting.domainmodel.AgendaId;
import dev.luan.sicredi.voting.domainmodel.VotingSession;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

public interface OpenVotingSession {

  VotingSession execute(Command command);

  record Command(AgendaId agendaId, Optional<Duration> requestedDuration) {

    public Command {
      Objects.requireNonNull(agendaId, "Agenda ID must not be null");
      Objects.requireNonNull(requestedDuration, "Requested duration must not be null");
    }
  }
}
