package dev.luan.sicredi.voting.bootstrap;

import dev.luan.sicredi.voting.engine.VotingSessionDurationPolicy;
import dev.luan.sicredi.voting.service.CastVoteService;
import dev.luan.sicredi.voting.service.CreateAgendaService;
import dev.luan.sicredi.voting.service.FinalizeExpiredVotingSessionsService;
import dev.luan.sicredi.voting.service.GetVotingResultService;
import dev.luan.sicredi.voting.service.OpenVotingSessionService;
import dev.luan.sicredi.voting.service.port.in.CastVote;
import dev.luan.sicredi.voting.service.port.in.CreateAgenda;
import dev.luan.sicredi.voting.service.port.in.FinalizeExpiredVotingSessions;
import dev.luan.sicredi.voting.service.port.in.GetVotingResult;
import dev.luan.sicredi.voting.service.port.in.OpenVotingSession;
import dev.luan.sicredi.voting.service.port.out.AgendaRepository;
import dev.luan.sicredi.voting.service.port.out.AssociateEligibilityGateway;
import dev.luan.sicredi.voting.service.port.out.TransactionExecutor;
import dev.luan.sicredi.voting.service.port.out.VoteRepository;
import dev.luan.sicredi.voting.service.port.out.VotingResultOutbox;
import dev.luan.sicredi.voting.service.port.out.VotingResultRepository;
import dev.luan.sicredi.voting.service.port.out.VotingSessionRepository;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class VotingUseCaseConfiguration {

  @Bean
  Clock votingClock() {
    return Clock.systemUTC();
  }

  @Bean
  VotingSessionDurationPolicy votingSessionDurationPolicy(VotingProperties properties) {
    return new VotingSessionDurationPolicy(
        properties.defaultSessionDuration(), properties.maximumSessionDuration());
  }

  @Bean
  CreateAgenda createAgenda(
      AgendaRepository agendaRepository,
      TransactionExecutor transactionExecutor,
      Clock votingClock) {
    return new CreateAgendaService(agendaRepository, transactionExecutor, votingClock);
  }

  @Bean
  OpenVotingSession openVotingSession(
      AgendaRepository agendaRepository,
      VotingSessionRepository votingSessionRepository,
      TransactionExecutor transactionExecutor,
      VotingSessionDurationPolicy durationPolicy,
      Clock votingClock) {
    return new OpenVotingSessionService(
        agendaRepository,
        votingSessionRepository,
        transactionExecutor,
        durationPolicy,
        votingClock);
  }

  @Bean
  CastVote castVote(
      VotingSessionRepository votingSessionRepository,
      VoteRepository voteRepository,
      AssociateEligibilityGateway associateEligibilityGateway,
      TransactionExecutor transactionExecutor,
      Clock votingClock) {
    return new CastVoteService(
        votingSessionRepository,
        voteRepository,
        associateEligibilityGateway,
        transactionExecutor,
        votingClock);
  }

  @Bean
  GetVotingResult getVotingResult(
      AgendaRepository agendaRepository, VotingResultRepository votingResultRepository) {
    return new GetVotingResultService(agendaRepository, votingResultRepository);
  }

  @Bean
  FinalizeExpiredVotingSessions finalizeExpiredVotingSessions(
      VotingSessionRepository votingSessionRepository,
      VoteRepository voteRepository,
      VotingResultRepository votingResultRepository,
      VotingResultOutbox votingResultOutbox,
      TransactionExecutor transactionExecutor,
      Clock votingClock) {
    return new FinalizeExpiredVotingSessionsService(
        votingSessionRepository,
        voteRepository,
        votingResultRepository,
        votingResultOutbox,
        transactionExecutor,
        votingClock);
  }
}
