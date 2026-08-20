package ua.stepan.zhuk.outbox;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.UUID;

public interface OutboxMapper {
    @Insert("""
            INSERT INTO outbox_messages
                (id, aggregate_type, aggregate_id, event_type, routing_key, payload, created_at, retry_count)
            VALUES
                (
                    #{id},
                    #{aggregateType},
                    #{aggregateId},
                    #{eventType},
                    #{routingKey},
                    CAST(#{payload} AS jsonb),
                    #{createdAt},
                    #{retryCount}
                )
            """)
    void insert(OutboxMessage message);

    @Select("""
            SELECT
                id,
                aggregate_type AS "aggregateType",
                aggregate_id AS "aggregateId",
                event_type AS "eventType",
                routing_key AS "routingKey",
                payload::text AS payload,
                created_at AS "createdAt",
                retry_count AS "retryCount",
                published_at AS "publishedAt"
            FROM outbox_messages
            WHERE
                published_at IS NULL AND retry_count < 20
            ORDER BY created_at
            LIMIT #{batchSize}
            FOR UPDATE SKIP LOCKED
            """)
    List<OutboxMessage> lockNextBatch(@Param("batchSize") int batchSize);

    @Update("""
            UPDATE
                outbox_messages
            SET
                published_at = #{publishedAt},
                last_error = NULL
            WHERE
                id = #{id}
                AND published_at IS NULL
            """)
    void markPublished(@Param("id") UUID id, @Param("publishedAt") Long publishedAt);

    @Update("""
            UPDATE
                outbox_messages
            SET
                retry_count = retry_count + 1,
                last_error = #{error}
            WHERE
                id = #{id}
                AND published_at IS NULL
            """)
    void recordFailure(@Param("id") UUID id, @Param("error") String error);
}
