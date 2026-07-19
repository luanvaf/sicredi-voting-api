package dev.luan.sicredi.voting.integration.messaging;

import static dev.luan.sicredi.voting.service.port.out.ResultEventPublicationException.FailureCode.BROKER_CONFIRM_TIMEOUT;
import static dev.luan.sicredi.voting.service.port.out.ResultEventPublicationException.FailureCode.BROKER_NACK;
import static dev.luan.sicredi.voting.service.port.out.ResultEventPublicationException.FailureCode.BROKER_UNAVAILABLE;
import static dev.luan.sicredi.voting.service.port.out.ResultEventPublicationException.FailureCode.PUBLICATION_INTERRUPTED;
import static dev.luan.sicredi.voting.service.port.out.ResultEventPublicationException.FailureCode.UNROUTABLE_MESSAGE;

import dev.luan.sicredi.voting.service.port.out.ResultEventPublicationException;
import dev.luan.sicredi.voting.service.port.out.ResultEventPublisher;
import dev.luan.sicredi.voting.service.port.out.VotingResultOutbox.ClaimedResultEvent;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.connection.CorrelationData.Confirm;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

public final class RabbitResultEventPublisher implements ResultEventPublisher {

  private final RabbitTemplate rabbitTemplate;
  private final String exchange;
  private final String routingKey;
  private final long confirmTimeoutMillis;

  public RabbitResultEventPublisher(
      RabbitTemplate rabbitTemplate, String exchange, String routingKey, Duration confirmTimeout) {
    this.rabbitTemplate =
        Objects.requireNonNull(rabbitTemplate, "Rabbit template must not be null");
    this.exchange = requireText(exchange, "Rabbit exchange");
    this.routingKey = requireText(routingKey, "Rabbit routing key");
    Objects.requireNonNull(confirmTimeout, "Publisher confirm timeout must not be null");
    this.confirmTimeoutMillis = confirmTimeout.toMillis();
    if (confirmTimeout.isNegative() || confirmTimeout.isZero() || confirmTimeoutMillis < 1) {
      throw new IllegalArgumentException(
          "Publisher confirm timeout must be at least one millisecond");
    }
    this.rabbitTemplate.setMandatory(true);
  }

  @Override
  public void publish(ClaimedResultEvent event) {
    Objects.requireNonNull(event, "Claimed result event must not be null");
    CorrelationData correlationData = new CorrelationData(event.eventId().toString());

    try {
      rabbitTemplate.send(exchange, routingKey, messageFor(event), correlationData);
      Confirm confirm =
          correlationData.getFuture().get(confirmTimeoutMillis, TimeUnit.MILLISECONDS);
      if (!confirm.ack()) {
        throw new ResultEventPublicationException(BROKER_NACK);
      }
      if (correlationData.getReturned() != null) {
        throw new ResultEventPublicationException(UNROUTABLE_MESSAGE);
      }
    } catch (TimeoutException exception) {
      throw new ResultEventPublicationException(BROKER_CONFIRM_TIMEOUT, exception);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new ResultEventPublicationException(PUBLICATION_INTERRUPTED, exception);
    } catch (ExecutionException | AmqpException exception) {
      throw new ResultEventPublicationException(BROKER_UNAVAILABLE, exception);
    }
  }

  private Message messageFor(ClaimedResultEvent event) {
    return MessageBuilder.withBody(event.payload().getBytes(StandardCharsets.UTF_8))
        .setMessageId(event.eventId().toString())
        .setCorrelationId(event.agendaId().toString())
        .setType(event.eventType())
        .setContentType(MessageProperties.CONTENT_TYPE_JSON)
        .setContentEncoding(StandardCharsets.UTF_8.name())
        .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
        .build();
  }

  private static String requireText(String value, String fieldName) {
    Objects.requireNonNull(value, fieldName + " must not be null");
    if (value.isBlank()) {
      throw new IllegalArgumentException(fieldName + " must not be blank");
    }
    return value;
  }
}
