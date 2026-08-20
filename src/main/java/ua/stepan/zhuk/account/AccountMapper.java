package ua.stepan.zhuk.account;

import org.apache.ibatis.annotations.*;

import java.util.Optional;
import java.util.UUID;

public interface AccountMapper {

    @Select(
            value = """
                    INSERT INTO accounts (
                        public_id,
                        customer_id,
                        country,
                        created_at,
                        updated_at
                    )
                    VALUES (
                        #{publicId},
                        #{customerId},
                        #{country},
                        #{createdAt},
                        #{updatedAt}
                    )
                    RETURNING id
                    """,
            affectData = true
    )
    @Options(
            flushCache = Options.FlushCachePolicy.TRUE,
            useCache = false
    )
    Long insert(Account account);

    @Select("""
            SELECT
                id,
                public_id AS "publicId",
                customer_id AS "customerId",
                country,
                created_at AS "createdAt",
                updated_at AS "updatedAt"
            FROM accounts
            WHERE
                public_id = #{accountId}
            """)
    Optional<Account> findByAccountId(UUID accountId);
}
