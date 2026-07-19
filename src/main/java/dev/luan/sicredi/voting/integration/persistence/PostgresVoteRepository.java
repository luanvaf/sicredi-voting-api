package dev.luan.sicredi.voting.integration.persistence;

import dev.luan.sicredi.voting.domainmodel.Vote;
import dev.luan.sicredi.voting.domainmodel.VoteTally;
import dev.luan.sicredi.voting.domainmodel.VotingSessionId;
import dev.luan.sicredi.voting.service.error.PersistenceUnavailableException;
import dev.luan.sicredi.voting.service.port.out.DuplicateVoteException;
import dev.luan.sicredi.voting.service.port.out.VoteRepository;
import java.sql.Timestamp;
import java.util.Objects;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PostgresVoteRepository implements VoteRepository {

  private static final String INSERT_VOTE =
      """
      INSERT INTO votes (id, voting_session_id, associate_cpf, choice, cast_at)
      VALUES (?, ?, ?, ?, ?)
      ON CONFLICT (voting_session_id, associate_cpf) DO NOTHING
      """;

  private static final String TALLY_VOTES =
      """
      SELECT
          COUNT(*) FILTER (WHERE choice = 'YES') AS yes_votes,
          COUNT(*) FILTER (WHERE choice = 'NO') AS no_votes
      FROM votes
      WHERE voting_session_id = ?
      """;

  private final JdbcTemplate jdbcTemplate;

  public PostgresVoteRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "JDBC template must not be null");
  }

  @Override
  public Vote save(Vote vote) {
    Objects.requireNonNull(vote, "Vote must not be null");
    try {
      int insertedRows =
          jdbcTemplate.update(
              INSERT_VOTE,
              vote.id().value(),
              vote.votingSessionId().value(),
              vote.associateCpf().value(),
              vote.choice().name(),
              Timestamp.from(vote.castAt()));
      if (insertedRows == 0) {
        throw new DuplicateVoteException();
      }
      return vote;
    } catch (DataAccessException exception) {
      throw new PersistenceUnavailableException(exception);
    }
  }

  @Override
  public VoteTally tally(VotingSessionId votingSessionId) {
    Objects.requireNonNull(votingSessionId, "Voting session ID must not be null");
    try {
      return jdbcTemplate.queryForObject(
          TALLY_VOTES,
          (resultSet, rowNumber) ->
              new VoteTally(resultSet.getLong("yes_votes"), resultSet.getLong("no_votes")),
          votingSessionId.value());
    } catch (DataAccessException exception) {
      throw new PersistenceUnavailableException(exception);
    }
  }
}
