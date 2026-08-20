package ua.stepan.zhuk.account.balance;

import ua.stepan.zhuk.account.enums.Currency;

import java.math.BigDecimal;
import java.util.List;

public record BalanceResponse(BigDecimal availableAmount, Currency currency) {

    public static List<BalanceResponse> toResponses(List<Balance> balances) {
        return balances.stream()
                .map(BalanceResponse::toResponse)
                .toList();
    }

    public static BalanceResponse toResponse(Balance balance) {
        return new BalanceResponse(balance.availableAmount(), balance.currency());
    }
}
