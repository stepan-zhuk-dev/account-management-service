package ua.stepan.zhuk.account.balance;

import ua.stepan.zhuk.account.enums.Currency;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record BalanceDto(UUID balanceId, UUID accountId, BigDecimal availableAmount, Currency currency) {
    public static List<BalanceDto> toDto(UUID publicAccountId, List<Balance> balances) {
        return balances.stream()
                .map(balance -> toDto(publicAccountId, balance))
                .toList();
    }

    public static BalanceDto toDto(UUID publicAccountId, Balance balance) {
        return new BalanceDto(balance.publicId(), publicAccountId, balance.availableAmount(), balance.currency());
    }
}
