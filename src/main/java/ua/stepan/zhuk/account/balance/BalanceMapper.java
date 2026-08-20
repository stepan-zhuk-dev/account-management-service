package ua.stepan.zhuk.account.balance;

import org.apache.ibatis.annotations.*;
import ua.stepan.zhuk.account.enums.Currency;

import java.util.List;
import java.util.Optional;

public interface BalanceMapper {
    @Select(
            value = """
                <script>
                    INSERT INTO balances (
                        public_id,
                        account_id,
                        available_amount,
                        currency,
                        created_at,
                        updated_at
                    )
                    VALUES
                        <foreach collection="balances"
                                 item="balance"
                                 separator=",">
                            (
                                #{balance.publicId},
                                #{balance.accountId},
                                #{balance.availableAmount},
                                #{balance.currency},
                                #{balance.createdAt},
                                #{balance.updatedAt}
                            )
                        </foreach>
                    RETURNING
                        id,
                        public_id AS "publicId",
                        account_id AS "accountId",
                        available_amount AS "availableAmount",
                        currency,
                        created_at AS "createdAt",
                        updated_at AS "updatedAt"
                </script>
                """,
            affectData = true
    )
    @Options(
            flushCache = Options.FlushCachePolicy.TRUE,
            useCache = false
    )
    List<Balance> insert(@Param("balances") List<Balance> balances);

    @Select("""
            SELECT
                id,
                public_id AS "publicId",
                account_id AS "accountId",
                currency,
                available_amount AS "availableAmount",
                created_at AS "createdAt",
                updated_at AS "updatedAt"
            FROM balances
            WHERE
                account_id = #{accountId}
                AND currency = #{currency}
            FOR UPDATE
            """)
    Optional<Balance> lock(@Param("accountId") Long accountId, @Param("currency") Currency currency);

    @Select("""
            SELECT
                id,
                public_id AS "publicId",
                account_id AS "accountId",
                available_amount AS "availableAmount",
                currency,
                created_at AS "createdAt",
                updated_at AS "updatedAt"
            FROM balances
            WHERE account_id = #{accountId}
            ORDER BY id
            """)
    List<Balance> findByAccountId(Long accountId);

    @Update("""
            UPDATE
                balances
            SET
                available_amount = #{balance.availableAmount},
                updated_at = #{balance.updatedAt}
            WHERE
                id = #{balance.id}
            """)
    void updateAmount(@Param("balance") Balance balance);
}
