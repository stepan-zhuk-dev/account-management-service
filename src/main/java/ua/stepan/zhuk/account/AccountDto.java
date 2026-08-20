package ua.stepan.zhuk.account;

import ua.stepan.zhuk.account.balance.BalanceDto;

import java.util.List;
import java.util.UUID;

public record AccountDto(UUID accountId, UUID customerId, String country, List<BalanceDto> balances) {
    public static AccountDto toDto(Account account) {
        return new AccountDto(
                account.publicId(),
                account.customerId(),
                account.country(),
                BalanceDto.toDto(account.publicId(), account.balances())
        );
    }
}
