package ua.stepan.zhuk.outbox;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import ua.stepan.zhuk.outbox.enums.AggregationType;
import ua.stepan.zhuk.outbox.enums.EventType;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class OutboxServiceTest {

    @Test
    void givenOutboxEventData_whenCreate_thenSerializesEventAndStoresMessage() {
        OutboxMapper mapper = mock(OutboxMapper.class);
        ObjectMapper objectMapper = new ObjectMapper();
        OutboxService service = new OutboxService(mapper, objectMapper);

        service.create(
                AggregationType.ACCOUNT,
                77L,
                EventType.ACCOUNT_CREATED,
                Map.of("status", "created")
        );

        ArgumentCaptor<OutboxMessage> captor = ArgumentCaptor.forClass(OutboxMessage.class);
        verify(mapper).insert(captor.capture());

        OutboxMessage message = captor.getValue();
        assertThat(message.id()).isNotNull();
        assertThat(message.aggregateType()).isEqualTo(AggregationType.ACCOUNT);
        assertThat(message.aggregateId()).isEqualTo(77L);
        assertThat(message.eventType()).isEqualTo(EventType.ACCOUNT_CREATED);
        assertThat(message.routingKey()).isEqualTo("account.created");
        assertThat(message.retryCount()).isZero();
        assertThat(message.createdAt()).isPositive();
        assertThat(message.publishedAt()).isNull();

        JsonNode payload = objectMapper.readTree(message.payload());
        assertThat(payload.get("eventId").asString()).isEqualTo(message.id().toString());
        assertThat(payload.get("eventType").asString()).isEqualTo("ACCOUNT_CREATED");
        assertThat(payload.get("createdAt").asLong()).isEqualTo(message.createdAt());
        assertThat(payload.get("data").get("status").asString()).isEqualTo("created");
    }
}
