package ua.stepan.zhuk.account.balance;

import ua.stepan.zhuk.account.enums.Currency;

import java.math.BigDecimal;
import java.util.UUID;

public record Balance(
        Long id,
        UUID publicId,
        Long accountId,
        BigDecimal availableAmount,
        Currency currency,
        Long createdAt,
        Long updatedAt
) {
    public static Balance createWithZeroAmount(Long accountId, Currency currency) {
        final long createdAt = System.currentTimeMillis();

        return new Balance(null, UUID.randomUUID(), accountId, BigDecimal.ZERO, currency, createdAt, createdAt);
    }

    public static Balance updateAmount(Balance balance, BigDecimal updatedAmount) {
        return new Balance(balance.id(), balance.publicId(), balance.accountId(), updatedAmount, balance.currency(), balance.createdAt(), System.currentTimeMillis());
    }
}
