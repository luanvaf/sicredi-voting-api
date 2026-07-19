package dev.luan.sicredi.voting.service.port.out;

import dev.luan.sicredi.voting.domainmodel.Vote;
import dev.luan.sicredi.voting.domainmodel.VoteTally;
import dev.luan.sicredi.voting.domainmodel.VotingSessionId;

public interface VoteRepository {

  Vote save(Vote vote);

  VoteTally tally(VotingSessionId votingSessionId);
}
