package dev.luan.sicredi.voting.bootstrap;

import dev.luan.sicredi.voting.integration.messaging.PostgresVotingResultOutbox;
import dev.luan.sicredi.voting.integration.messaging.RabbitResultEventPublisher;
import dev.luan.sicredi.voting.service.PublishPendingResultEventsService;
import dev.luan.sicredi.voting.service.port.in.PublishPendingResultEvents;
import dev.luan.sicredi.voting.service.port.out.ResultEventPublisher;
import dev.luan.sicredi.voting.service.port.out.TransactionExecutor;
import dev.luan.sicredi.voting.service.port.out.VotingResultOutbox;
import java.time.Clock;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.ObjectMapper;

@Configuration(proxyBeanMethods = false)
class MessagingConfiguration {

  @Bean
  TopicExchange votingResultExchange(MessagingProperties properties) {
    return new TopicExchange(properties.resultExchange(), true, false);
  }

  @Bean
  Queue votingResultQueue(MessagingProperties properties) {
    return QueueBuilder.durable(properties.resultQueue()).build();
  }

  @Bean
  Binding votingResultBinding(
      @Qualifier("votingResultQueue") Queue queue,
      @Qualifier("votingResultExchange") TopicExchange exchange,
      MessagingProperties properties) {
    return BindingBuilder.bind(queue).to(exchange).with(properties.resultRoutingKey());
  }

  @Bean
  VotingResultOutbox votingResultOutbox(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
    return new PostgresVotingResultOutbox(jdbcTemplate, objectMapper);
  }

  @Bean
  ResultEventPublisher resultEventPublisher(
      RabbitTemplate rabbitTemplate, MessagingProperties properties) {
    return new RabbitResultEventPublisher(
        rabbitTemplate,
        properties.resultExchange(),
        properties.resultRoutingKey(),
        properties.confirmTimeout());
  }

  @Bean
  PublishPendingResultEvents publishPendingResultEvents(
      VotingResultOutbox outbox,
      ResultEventPublisher publisher,
      TransactionExecutor transactionExecutor,
      Clock votingClock,
      MessagingProperties properties) {
    return new PublishPendingResultEventsService(
        outbox,
        publisher,
        transactionExecutor,
        votingClock,
        properties.claimLease(),
        properties.initialRetryDelay(),
        properties.maximumRetryDelay());
  }
}
