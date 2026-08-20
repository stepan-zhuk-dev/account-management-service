package ua.stepan.zhuk.outbox;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.MediaType;
import ua.stepan.zhuk.outbox.enums.AggregationType;
import ua.stepan.zhuk.outbox.enums.EventType;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class OutboxPublisherTest {

    private final OutboxMapper mapper = mock(OutboxMapper.class);
    private final RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
    private final OutboxPublisher publisher = new OutboxPublisher(mapper, rabbitTemplate, "banking.exchange", 25);

    @Test
    void givenPendingMessageAndBrokerAck_whenPublishPending_thenSendsMessageAndMarksPublished() {
        OutboxMessage message = message("payload");
        when(mapper.lockNextBatch(25)).thenReturn(List.of(message));
        doAnswer(invocation -> {
            CorrelationData correlation = invocation.getArgument(3);
            correlation.getFuture().complete(new CorrelationData.Confirm(true, null));
            return null;
        }).when(rabbitTemplate)
                .send(
                        eq("banking.exchange"),
                        eq("account.created"),
                        any(Message.class),
                        any(CorrelationData.class)
                );

        publisher.publishPending();

        ArgumentCaptor<Message> sentMessage = ArgumentCaptor.forClass(Message.class);
        ArgumentCaptor<CorrelationData> correlation = ArgumentCaptor.forClass(CorrelationData.class);

        verify(rabbitTemplate).send(
                eq("banking.exchange"),
                eq("account.created"),
                sentMessage.capture(),
                correlation.capture()
        );

        assertThat(correlation.getValue().getId()).isEqualTo(message.id().toString());
        assertThat(new String(sentMessage.getValue().getBody(), StandardCharsets.UTF_8)).isEqualTo("payload");
        assertThat(sentMessage.getValue().getMessageProperties().getMessageId()).isEqualTo(message.id().toString());
        assertThat(sentMessage.getValue().getMessageProperties().getContentType())
                .isEqualTo(MediaType.APPLICATION_JSON_VALUE);
        assertThat(sentMessage.getValue().getMessageProperties().getDeliveryMode())
                .isEqualTo(MessageDeliveryMode.PERSISTENT);
        assertThat(sentMessage.getValue().getMessageProperties().getHeaders())
                .containsEntry("eventType", EventType.ACCOUNT_CREATED)
                .containsEntry("aggregateId", "77");
        verify(mapper).markPublished(eq(message.id()), any(Long.class));
        verify(mapper, never()).recordFailure(any(), any());
    }

    @Test
    void givenPendingMessageAndBrokerRejects_whenPublishPending_thenRecordsFailure() {
        OutboxMessage message = message("payload1");
        when(mapper.lockNextBatch(25)).thenReturn(List.of(message));
        doAnswer(invocation -> {
            CorrelationData correlation = invocation.getArgument(3);
            correlation.getFuture().complete(new CorrelationData.Confirm(false, "no route"));
            return null;
        }).when(rabbitTemplate)
                .send(
                        eq("banking.exchange"),
                        eq("account.created"),
                        any(Message.class),
                        any(CorrelationData.class)
                );

        publisher.publishPending();

        verify(mapper).recordFailure(message.id(), "no route");
        verify(mapper, never()).markPublished(any(), any());
    }

    @Test
    void givenPendingMessageAndBrokerReturnsMessage_whenPublishPending_thenRecordsFailure() {
        OutboxMessage message = message("payload1");
        when(mapper.lockNextBatch(25)).thenReturn(List.of(message));
        doAnswer(invocation -> {
            Message sentMessage = invocation.getArgument(2);
            CorrelationData correlation = invocation.getArgument(3);
            correlation.setReturned(new ReturnedMessage(
                    sentMessage,
                    312,
                    "NO_ROUTE",
                    "banking.exchange",
                    "account.created"
            ));
            correlation.getFuture().complete(new CorrelationData.Confirm(true, null));
            return null;
        }).when(rabbitTemplate)
                .send(
                        eq("banking.exchange"),
                        eq("account.created"),
                        any(Message.class),
                        any(CorrelationData.class)
                );

        publisher.publishPending();

        verify(mapper).recordFailure(message.id(), "RabbitMQ rejected message");
        verify(mapper, never()).markPublished(any(), any());
    }

    @Test
    void givenPendingMessageAndLongSendFailure_whenPublishPending_thenTruncatesRecordedError() {
        OutboxMessage message = message("payload2");
        String error = "x".repeat(1200);
        when(mapper.lockNextBatch(25)).thenReturn(List.of(message));
        doAnswer(invocation -> {
            throw new IllegalStateException(error);
        }).when(rabbitTemplate)
                .send(
                        eq("banking.exchange"),
                        eq("account.created"),
                        any(Message.class),
                        any(CorrelationData.class)
                );

        publisher.publishPending();

        ArgumentCaptor<String> errorCaptor = ArgumentCaptor.forClass(String.class);
        verify(mapper).recordFailure(eq(message.id()), errorCaptor.capture());
        assertThat(errorCaptor.getValue()).isEqualTo("x".repeat(1000));
        verify(mapper, never()).markPublished(any(), any());
    }

    private static OutboxMessage message(String payload) {
        return OutboxMessage.builder()
                .id(UUID.randomUUID())
                .aggregateType(AggregationType.ACCOUNT)
                .aggregateId(77L)
                .eventType(EventType.ACCOUNT_CREATED)
                .routingKey(EventType.ACCOUNT_CREATED.getRoutingKey())
                .payload(payload)
                .createdAt(1L)
                .retryCount(0)
                .build();
    }
}
