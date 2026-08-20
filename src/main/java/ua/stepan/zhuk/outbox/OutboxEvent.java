package ua.stepan.zhuk.outbox;

import ua.stepan.zhuk.outbox.enums.EventType;

import java.util.UUID;

public record OutboxEvent<T>(UUID eventId, EventType eventType, Long createdAt, T data) {
}
