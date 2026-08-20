package ua.stepan.zhuk.account;

import ua.stepan.zhuk.account.balance.BalanceResponse;

import java.util.List;
import java.util.UUID;

public record AccountResponse(UUID accountId, UUID customerId, List<BalanceResponse> balances) {
    public static AccountResponse toResponse(Account account) {
        return new AccountResponse(account.publicId(), account.customerId(), BalanceResponse.toResponses(account.balances()));
    }
}
