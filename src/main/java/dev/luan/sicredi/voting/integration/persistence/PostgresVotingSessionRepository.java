package dev.luan.sicredi.voting.integration.persistence;

import dev.luan.sicredi.voting.domainmodel.AgendaId;
import dev.luan.sicredi.voting.domainmodel.VotingSession;
import dev.luan.sicredi.voting.domainmodel.VotingSessionId;
import dev.luan.sicredi.voting.service.error.PersistenceUnavailableException;
import dev.luan.sicredi.voting.service.port.out.DuplicateVotingSessionException;
import dev.luan.sicredi.voting.service.port.out.VotingSessionRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceException;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

@Repository
public class PostgresVotingSessionRepository implements VotingSessionRepository {

  private static final String FIND_BY_AGENDA_ID =
      "select session from VotingSessionJpaEntity session " + "where session.agendaId = :agendaId";
  private static final String FIND_EXPIRED_FOR_CLOSURE =
      """
            SELECT *
            FROM voting_sessions
            WHERE finalized_at IS NULL
              AND closes_at <= :closingThreshold
            ORDER BY closes_at, id
            LIMIT :batchSize
            FOR UPDATE SKIP LOCKED
            """;

  private final SpringDataVotingSessionRepository repository;
  private final EntityManager entityManager;

  public PostgresVotingSessionRepository(
      SpringDataVotingSessionRepository repository, EntityManager entityManager) {
    this.repository =
        Objects.requireNonNull(
            repository, "Spring Data voting session repository must not be null");
    this.entityManager = Objects.requireNonNull(entityManager, "Entity manager must not be null");
  }

  @Override
  public VotingSession save(VotingSession votingSession) {
    try {
      return repository.saveAndFlush(VotingSessionJpaEntity.fromDomain(votingSession)).toDomain();
    } catch (DataIntegrityViolationException exception) {
      throw new DuplicateVotingSessionException(exception);
    } catch (DataAccessException exception) {
      throw new PersistenceUnavailableException(exception);
    }
  }

  @Override
  public Optional<VotingSession> findByAgendaId(AgendaId agendaId) {
    try {
      return repository.findByAgendaId(agendaId.value()).map(VotingSessionJpaEntity::toDomain);
    } catch (DataAccessException exception) {
      throw new PersistenceUnavailableException(exception);
    }
  }

  @Override
  public Optional<VotingSession> acquireForVote(AgendaId agendaId) {
    try {
      return entityManager
          .createQuery(FIND_BY_AGENDA_ID, VotingSessionJpaEntity.class)
          .setParameter("agendaId", agendaId.value())
          .setLockMode(LockModeType.PESSIMISTIC_READ)
          .getResultStream()
          .findFirst()
          .map(VotingSessionJpaEntity::toDomain);
    } catch (PersistenceException exception) {
      throw new PersistenceUnavailableException(exception);
    }
  }

  @Override
  public Optional<VotingSession> acquireForClosure(VotingSessionId votingSessionId) {
    try {
      return Optional.ofNullable(
              entityManager.find(
                  VotingSessionJpaEntity.class,
                  votingSessionId.value(),
                  LockModeType.PESSIMISTIC_WRITE))
          .map(VotingSessionJpaEntity::toDomain);
    } catch (PersistenceException exception) {
      throw new PersistenceUnavailableException(exception);
    }
  }

  @Override
  public List<VotingSession> acquireExpiredForClosure(Instant closingThreshold, int batchSize) {
    if (batchSize < 1) {
      throw new IllegalArgumentException("Closure batch size must be positive");
    }

    try {
      @SuppressWarnings("unchecked")
      List<VotingSessionJpaEntity> entities =
          entityManager
              .createNativeQuery(FIND_EXPIRED_FOR_CLOSURE, VotingSessionJpaEntity.class)
              .setParameter("closingThreshold", closingThreshold)
              .setParameter("batchSize", batchSize)
              .getResultList();
      return entities.stream().map(VotingSessionJpaEntity::toDomain).toList();
    } catch (PersistenceException exception) {
      throw new PersistenceUnavailableException(exception);
    }
  }
}
