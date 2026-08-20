package ua.stepan.zhuk.account.transaction;

import ua.stepan.zhuk.account.enums.Currency;
import ua.stepan.zhuk.account.transaction.enums.TransactionDirection;

import java.math.BigDecimal;
import java.util.UUID;

public record TransactionResponse(
        UUID accountId,
        UUID transactionId,
        BigDecimal amount,
        Currency currency,
        TransactionDirection direction,
        String description,
        BigDecimal balanceAfter
) {
    public static TransactionResponse toResponse(UUID publicAccountId, Transaction transaction) {
        return new TransactionResponse(
                publicAccountId,
                transaction.publicId(),
                transaction.amount(),
                transaction.currency(),
                transaction.direction(),
                transaction.description(),
                transaction.balanceAfter()
        );
    }
}
