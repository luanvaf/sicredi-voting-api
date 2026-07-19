package dev.luan.sicredi.voting.service.port.out;

import dev.luan.sicredi.voting.service.port.out.VotingResultOutbox.ClaimedResultEvent;

public interface ResultEventPublisher {

  void publish(ClaimedResultEvent event);
}
