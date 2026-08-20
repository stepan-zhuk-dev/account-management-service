package ua.stepan.zhuk.outbox;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;
import ua.stepan.zhuk.outbox.enums.AggregationType;
import ua.stepan.zhuk.outbox.enums.EventType;

import java.util.UUID;

@Service
@AllArgsConstructor
public class OutboxService {
    private final OutboxMapper outboxMapper;
    private final ObjectMapper objectMapper;

    public void create(AggregationType aggregateType, Long aggregateId, EventType eventType, Object data) {
        final UUID eventId = UUID.randomUUID();
        final long createdAt = System.currentTimeMillis();

        final String payload = objectMapper.writeValueAsString(new OutboxEvent<>(eventId, eventType, createdAt, data));
        final OutboxMessage outboxMessage = OutboxMessage.builder()
                .id(eventId)
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                .eventType(eventType)
                .routingKey(eventType.getRoutingKey())
                .payload(payload)
                .createdAt(createdAt)
                .retryCount(0)
                .build();

        outboxMapper.insert(outboxMessage);
    }
}
