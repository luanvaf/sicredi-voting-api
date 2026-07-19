package dev.luan.sicredi.voting.integration.messaging;

import static dev.luan.sicredi.voting.service.port.out.ResultEventPublicationException.FailureCode.BROKER_CONFIRM_TIMEOUT;
import static dev.luan.sicredi.voting.service.port.out.ResultEventPublicationException.FailureCode.BROKER_NACK;
import static dev.luan.sicredi.voting.service.port.out.ResultEventPublicationException.FailureCode.UNROUTABLE_MESSAGE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import dev.luan.sicredi.voting.service.port.out.ResultEventPublicationException;
import dev.luan.sicredi.voting.service.port.out.VotingResultOutbox.ClaimedResultEvent;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.connection.CorrelationData.Confirm;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

class RabbitResultEventPublisherTest {

  private static final String EXCHANGE = "voting.events";
  private static final String ROUTING_KEY = "voting.result.finalized";

  private final RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);

  @Test
  void shouldPublishAPersistentJsonMessageAndWaitForItsCorrelatedConfirmation() {
    ClaimedResultEvent event = claimedEvent();
    RabbitResultEventPublisher publisher = publisher();
    doAnswer(
            invocation -> {
              CorrelationData correlationData = invocation.getArgument(3);
              correlationData.getFuture().complete(new Confirm(true, null));
              return null;
            })
        .when(rabbitTemplate)
        .send(eq(EXCHANGE), eq(ROUTING_KEY), any(Message.class), any(CorrelationData.class));

    publisher.publish(event);

    ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
    ArgumentCaptor<CorrelationData> correlationCaptor =
        ArgumentCaptor.forClass(CorrelationData.class);
    verify(rabbitTemplate)
        .send(eq(EXCHANGE), eq(ROUTING_KEY), messageCaptor.capture(), correlationCaptor.capture());
    Message message = messageCaptor.getValue();
    assertThat(message.getBody()).isEqualTo(event.payload().getBytes(StandardCharsets.UTF_8));
    assertThat(message.getMessageProperties().getMessageId()).isEqualTo(event.eventId().toString());
    assertThat(message.getMessageProperties().getCorrelationId())
        .isEqualTo(event.agendaId().toString());
    assertThat(message.getMessageProperties().getContentType())
        .isEqualTo(MessageProperties.CONTENT_TYPE_JSON);
    assertThat(message.getMessageProperties().getDeliveryMode())
        .isEqualTo(MessageDeliveryMode.PERSISTENT);
    assertThat(correlationCaptor.getValue().getId()).isEqualTo(event.eventId().toString());
    verify(rabbitTemplate).setMandatory(true);
  }

  @Test
  void shouldRejectANegativePublisherConfirmationWithoutExposingItsReason() {
    ClaimedResultEvent event = claimedEvent();
    RabbitResultEventPublisher publisher = publisher();
    doAnswer(
            invocation -> {
              CorrelationData correlationData = invocation.getArgument(3);
              correlationData
                  .getFuture()
                  .complete(new Confirm(false, "broker-secret-that-must-not-leak"));
              return null;
            })
        .when(rabbitTemplate)
        .send(eq(EXCHANGE), eq(ROUTING_KEY), any(Message.class), any(CorrelationData.class));

    assertThatThrownBy(() -> publisher.publish(event))
        .isInstanceOf(ResultEventPublicationException.class)
        .hasMessage("Result event publication failed: " + BROKER_NACK.name())
        .extracting(exception -> ((ResultEventPublicationException) exception).safeErrorCode())
        .isEqualTo(BROKER_NACK.name());
  }

  @Test
  void shouldTreatAReturnedMessageAsAPublicationFailure() {
    ClaimedResultEvent event = claimedEvent();
    RabbitResultEventPublisher publisher = publisher();
    doAnswer(
            invocation -> {
              Message message = invocation.getArgument(2);
              CorrelationData correlationData = invocation.getArgument(3);
              correlationData.setReturned(
                  new ReturnedMessage(message, 312, "NO_ROUTE", EXCHANGE, ROUTING_KEY));
              correlationData.getFuture().complete(new Confirm(true, null));
              return null;
            })
        .when(rabbitTemplate)
        .send(eq(EXCHANGE), eq(ROUTING_KEY), any(Message.class), any(CorrelationData.class));

    assertThatThrownBy(() -> publisher.publish(event))
        .isInstanceOf(ResultEventPublicationException.class)
        .extracting(exception -> ((ResultEventPublicationException) exception).safeErrorCode())
        .isEqualTo(UNROUTABLE_MESSAGE.name());
  }

  @Test
  void shouldTreatAMissingConfirmationAsATimeout() {
    ClaimedResultEvent event = claimedEvent();
    RabbitResultEventPublisher publisher =
        new RabbitResultEventPublisher(rabbitTemplate, EXCHANGE, ROUTING_KEY, Duration.ofMillis(1));

    assertThatThrownBy(() -> publisher.publish(event))
        .isInstanceOf(ResultEventPublicationException.class)
        .extracting(exception -> ((ResultEventPublicationException) exception).safeErrorCode())
        .isEqualTo(BROKER_CONFIRM_TIMEOUT.name());
  }

  private RabbitResultEventPublisher publisher() {
    return new RabbitResultEventPublisher(
        rabbitTemplate, EXCHANGE, ROUTING_KEY, Duration.ofSeconds(1));
  }

  private static ClaimedResultEvent claimedEvent() {
    return new ClaimedResultEvent(
        UUID.randomUUID(),
        UUID.randomUUID(),
        "voting-result.finalized.v1",
        "{\"outcome\":\"APPROVED\"}",
        1,
        Instant.parse("2026-07-19T12:00:30Z"));
  }
}
