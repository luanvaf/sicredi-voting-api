package dev.luan.sicredi.voting.service.port.out;

import dev.luan.sicredi.voting.domainmodel.VotingResultFinalizedEvent;

public interface VotingResultOutbox {

  void append(VotingResultFinalizedEvent event);
}
