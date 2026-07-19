package dev.luan.sicredi.voting.integration.persistence;

import dev.luan.sicredi.voting.domainmodel.Agenda;
import dev.luan.sicredi.voting.domainmodel.AgendaId;
import dev.luan.sicredi.voting.service.error.PersistenceUnavailableException;
import dev.luan.sicredi.voting.service.port.out.AgendaRepository;
import java.util.Objects;
import java.util.Optional;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Repository;

@Repository
public class PostgresAgendaRepository implements AgendaRepository {

  private final SpringDataAgendaRepository repository;

  public PostgresAgendaRepository(SpringDataAgendaRepository repository) {
    this.repository =
        Objects.requireNonNull(repository, "Spring Data agenda repository must not be null");
  }

  @Override
  public Agenda save(Agenda agenda) {
    try {
      return repository.saveAndFlush(AgendaJpaEntity.fromDomain(agenda)).toDomain();
    } catch (DataAccessException exception) {
      throw new PersistenceUnavailableException(exception);
    }
  }

  @Override
  public Optional<Agenda> findById(AgendaId agendaId) {
    try {
      return repository.findById(agendaId.value()).map(AgendaJpaEntity::toDomain);
    } catch (DataAccessException exception) {
      throw new PersistenceUnavailableException(exception);
    }
  }
}
