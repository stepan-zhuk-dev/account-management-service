package ua.stepan.zhuk.account;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.stepan.zhuk.account.balance.Balance;
import ua.stepan.zhuk.account.balance.BalanceMapper;
import ua.stepan.zhuk.outbox.OutboxService;
import ua.stepan.zhuk.outbox.enums.AggregationType;
import ua.stepan.zhuk.outbox.enums.EventType;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountService {
    private final AccountMapper accountMapper;
    private final BalanceMapper balanceMapper;
    private final OutboxService outboxService;

    @Transactional(rollbackFor = Throwable.class)
    public Account create(CreateAccountRequest request) {
        final Account account = Account.create(UUID.randomUUID(), request.customerId(), request.country());
        final Long id = accountMapper.insert(account);

        final List<Balance> balances = request.currencies()
                .stream()
                .map(currency -> Balance.createWithZeroAmount(id, currency))
                .toList();

        final List<Balance> savedBalances = balanceMapper.insert(balances);

        final Account accountWithIdAndBalances = Account.withIdAndBalances(account, id, savedBalances);

        outboxService.create(AggregationType.ACCOUNT, accountWithIdAndBalances.id(), EventType.ACCOUNT_CREATED, AccountDto.toDto(accountWithIdAndBalances));

        return accountWithIdAndBalances;
    }

    @Transactional(readOnly = true)
    public Account findByAccountId(UUID accountId) {
        final Account account = accountMapper.findByAccountId(accountId)
                .orElseThrow(AccountNotFoundException::new);

        final List<Balance> balances = balanceMapper.findByAccountId(account.id());

        return Account.withIdAndBalances(account, account.id(), balances);
    }
}
