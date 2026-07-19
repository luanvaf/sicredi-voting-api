package dev.luan.sicredi.voting.integration.persistence;

import dev.luan.sicredi.voting.domainmodel.Vote;
import dev.luan.sicredi.voting.domainmodel.VoteTally;
import dev.luan.sicredi.voting.domainmodel.VotingSessionId;
import dev.luan.sicredi.voting.service.error.PersistenceUnavailableException;
import dev.luan.sicredi.voting.service.port.out.DuplicateVoteException;
import dev.luan.sicredi.voting.service.port.out.VoteRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import java.util.Objects;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

@Repository
public class PostgresVoteRepository implements VoteRepository {

  private static final String TALLY_VOTES =
      """
            SELECT
                COUNT(*) FILTER (WHERE choice = 'YES') AS yes_votes,
                COUNT(*) FILTER (WHERE choice = 'NO') AS no_votes
            FROM votes
            WHERE voting_session_id = :votingSessionId
            """;

  private final SpringDataVoteRepository repository;
  private final EntityManager entityManager;

  public PostgresVoteRepository(SpringDataVoteRepository repository, EntityManager entityManager) {
    this.repository =
        Objects.requireNonNull(repository, "Spring Data vote repository must not be null");
    this.entityManager = Objects.requireNonNull(entityManager, "Entity manager must not be null");
  }

  @Override
  public Vote save(Vote vote) {
    try {
      return repository.saveAndFlush(VoteJpaEntity.fromDomain(vote)).toDomain();
    } catch (DataIntegrityViolationException exception) {
      throw new DuplicateVoteException(exception);
    } catch (DataAccessException exception) {
      throw new PersistenceUnavailableException(exception);
    }
  }

  @Override
  public VoteTally tally(VotingSessionId votingSessionId) {
    try {
      Object[] result =
          (Object[])
              entityManager
                  .createNativeQuery(TALLY_VOTES)
                  .setParameter("votingSessionId", votingSessionId.value())
                  .getSingleResult();
      return new VoteTally(((Number) result[0]).longValue(), ((Number) result[1]).longValue());
    } catch (PersistenceException exception) {
      throw new PersistenceUnavailableException(exception);
    }
  }
}
