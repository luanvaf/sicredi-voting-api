package dev.luan.sicredi.voting.service.port.in;

import dev.luan.sicredi.voting.domainmodel.AgendaId;
import dev.luan.sicredi.voting.domainmodel.AssociateCpf;
import dev.luan.sicredi.voting.domainmodel.Vote;
import dev.luan.sicredi.voting.domainmodel.VoteChoice;
import java.util.Objects;

public interface CastVote {

  Vote execute(Command command);

  record Command(AgendaId agendaId, AssociateCpf associateCpf, VoteChoice choice) {

    public Command {
      Objects.requireNonNull(agendaId, "Agenda ID must not be null");
      Objects.requireNonNull(associateCpf, "Associate CPF must not be null");
      Objects.requireNonNull(choice, "Vote choice must not be null");
    }
  }
}
