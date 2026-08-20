package ua.stepan.zhuk.outbox;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class OutboxPublisher {
    private final RabbitTemplate rabbitTemplate;
    private final OutboxMapper outboxMapper;
    private final String exchange;
    private final int batchSize;

    public OutboxPublisher(
            OutboxMapper outboxMapper,
            RabbitTemplate rabbitTemplate,
            @Value("${app.rabbit.exchange}") String exchange,
            @Value("${app.outbox.batch-size:50}") int batchSize
    ) {
        this.outboxMapper = outboxMapper;
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${app.outbox.poll-interval:500}")
    @Transactional
    public void publishPending() {
        outboxMapper.lockNextBatch(batchSize)
                .forEach(this::publish);
    }

    private void publish(OutboxMessage outbox) {
        final CorrelationData correlation = new CorrelationData(outbox.id().toString());
        final Message message = MessageBuilder.withBody(outbox.payload().getBytes(StandardCharsets.UTF_8))
                .setMessageId(outbox.id().toString())
                .setContentType(MediaType.APPLICATION_JSON_VALUE)
                .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
                .setHeader("eventType", outbox.eventType())
                .setHeader("aggregateId", outbox.aggregateId().toString())
                .build();

        try {
            rabbitTemplate.send(exchange, outbox.routingKey(), message, correlation);

            final CorrelationData.Confirm confirm = correlation.getFuture()
                    .get(5, TimeUnit.SECONDS);

            if (!confirm.ack() || correlation.getReturned() != null) {
                throw new IllegalStateException(confirm.reason() == null ? "RabbitMQ rejected message" : confirm.reason());
            }

            final long now = System.currentTimeMillis();
            outboxMapper.markPublished(outbox.id(), now);
        } catch (Exception exception) {
            final String error = exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
            outboxMapper.recordFailure(outbox.id(), error.substring(0, Math.min(error.length(), 1000)));

            log.warn("Publishing outbox event {} failed with error: {}", outbox.id(), error);
        }
    }
}
