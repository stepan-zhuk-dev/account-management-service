package ua.stepan.zhuk.account.transaction;

import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface TransactionMapper {
    @Select(
            value = """
            INSERT INTO transactions
                (
                     public_id,
                     account_id,
                     amount,
                     currency,
                     direction,
                     description,
                     balance_after,
                     created_at
                 )
            VALUES
                (
                     #{publicId},
                     #{accountId},
                     #{amount},
                     #{currency},
                     #{direction},
                     #{description},
                     #{balanceAfter},
                     #{createdAt}
                 )
            RETURNING id
            """,
            affectData = true
    )
    @Options(
            flushCache = Options.FlushCachePolicy.TRUE,
            useCache = false
    )
    Long insert(Transaction transaction);

    @Select("""
            SELECT
                id,
                public_id AS "publicId",
                account_id AS "accountId",
                amount,
                currency,
                direction,
                description,
                balance_after AS "balanceAfter",
                created_at AS "createdAt"
            FROM transactions
            WHERE account_id = #{accountId}
            ORDER BY id
            """)
    List<Transaction> findByAccountId(Long accountId);
}
