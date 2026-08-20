package ua.stepan.zhuk.account.transaction;

import org.apache.ibatis.annotations.AutomapConstructor;
import ua.stepan.zhuk.account.enums.Currency;
import ua.stepan.zhuk.account.transaction.enums.TransactionDirection;

import java.math.BigDecimal;
import java.util.UUID;

public record Transaction(
        Long id,
        UUID publicId,
        Long accountId,
        BigDecimal amount,
        Currency currency,
        TransactionDirection direction,
        String description,
        BigDecimal balanceAfter,
        Long createdAt
) {
    @AutomapConstructor
    public Transaction {
    }

    public static Transaction create(
            Long accountId,
            BigDecimal amount,
            Currency currency,
            TransactionDirection direction,
            String description,
            BigDecimal balanceAfter
    ) {
        return new Transaction(
                null,
                UUID.randomUUID(),
                accountId,
                amount,
                currency,
                direction,
                description,
                balanceAfter,
                System.currentTimeMillis()
        );
    }

    public static Transaction withId(Transaction transaction, Long id) {
        return new Transaction(
                id,
                transaction.publicId(),
                transaction.accountId(),
                transaction.amount(),
                transaction.currency(),
                transaction.direction(),
                transaction.description(),
                transaction.balanceAfter(),
                transaction.createdAt()
        );
    }
}
