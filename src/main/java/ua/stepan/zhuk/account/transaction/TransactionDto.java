package ua.stepan.zhuk.account.transaction;

import ua.stepan.zhuk.account.enums.Currency;
import ua.stepan.zhuk.account.transaction.enums.TransactionDirection;

import java.math.BigDecimal;
import java.util.UUID;

public record TransactionDto(
        UUID transactionId,
        UUID accountId,
        BigDecimal amount,
        Currency currency,
        TransactionDirection direction,
        String description,
        BigDecimal balanceAfter,
        Long createdAt
) {

    public static TransactionDto toDto(UUID publicAccountId, Transaction transaction) {
        return new TransactionDto(
                transaction.publicId(),
                publicAccountId,
                transaction.amount(),
                transaction.currency(),
                transaction.direction(),
                transaction.description(),
                transaction.balanceAfter(),
                transaction.createdAt()
        );
    }

}
