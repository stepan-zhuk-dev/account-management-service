package ua.stepan.zhuk.outbox;

import lombok.Builder;
import ua.stepan.zhuk.outbox.enums.AggregationType;
import ua.stepan.zhuk.outbox.enums.EventType;

import java.util.UUID;

@Builder
public record OutboxMessage(
        UUID id,
        AggregationType aggregateType,
        Long aggregateId,
        EventType eventType,
        String routingKey,
        String payload,
        Long createdAt,
        Integer retryCount,
        Long publishedAt
) {
}
