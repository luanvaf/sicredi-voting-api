package dev.luan.sicredi.voting.integration.persistence;

import dev.luan.sicredi.voting.domainmodel.AgendaId;
import dev.luan.sicredi.voting.domainmodel.VotingResult;
import dev.luan.sicredi.voting.domainmodel.VotingSessionId;
import dev.luan.sicredi.voting.service.error.PersistenceUnavailableException;
import dev.luan.sicredi.voting.service.port.out.VotingResultRepository;
import java.util.Objects;
import java.util.Optional;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Repository;

@Repository
public class PostgresVotingResultRepository implements VotingResultRepository {

  private final SpringDataVotingResultRepository repository;

  public PostgresVotingResultRepository(SpringDataVotingResultRepository repository) {
    this.repository =
        Objects.requireNonNull(repository, "Spring Data voting result repository must not be null");
  }

  @Override
  public VotingResult save(VotingResult votingResult) {
    Objects.requireNonNull(votingResult, "Voting result must not be null");
    try {
      return repository.saveAndFlush(VotingResultJpaEntity.fromDomain(votingResult)).toDomain();
    } catch (DataAccessException exception) {
      throw new PersistenceUnavailableException(exception);
    }
  }

  @Override
  public Optional<VotingResult> findByAgendaId(AgendaId agendaId) {
    Objects.requireNonNull(agendaId, "Agenda ID must not be null");
    try {
      return repository.findByAgendaId(agendaId.value()).map(VotingResultJpaEntity::toDomain);
    } catch (DataAccessException exception) {
      throw new PersistenceUnavailableException(exception);
    }
  }

  @Override
  public Optional<VotingResult> findByVotingSessionId(VotingSessionId votingSessionId) {
    Objects.requireNonNull(votingSessionId, "Voting session ID must not be null");
    try {
      return repository.findById(votingSessionId.value()).map(VotingResultJpaEntity::toDomain);
    } catch (DataAccessException exception) {
      throw new PersistenceUnavailableException(exception);
    }
  }
}
