package ua.stepan.zhuk.account.transaction;

import ua.stepan.zhuk.account.enums.Currency;
import ua.stepan.zhuk.account.transaction.enums.TransactionDirection;

import java.math.BigDecimal;
import java.util.UUID;

public record TransactionHistoryResponse(
        UUID accountId,
        UUID transactionId,
        BigDecimal amount,
        Currency currency,
        TransactionDirection direction,
        String description
) {
    public static TransactionHistoryResponse toResponse(UUID publicAccountId, Transaction transaction) {
        return new TransactionHistoryResponse(
                publicAccountId,
                transaction.publicId(),
                transaction.amount(),
                transaction.currency(),
                transaction.direction(),
                transaction.description()
        );
    }
}
