package ua.stepan.zhuk.account.transaction;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.stepan.zhuk.account.Account;
import ua.stepan.zhuk.account.AccountMapper;
import ua.stepan.zhuk.account.AccountNotFoundException;
import ua.stepan.zhuk.account.InvalidAccountException;
import ua.stepan.zhuk.account.balance.Balance;
import ua.stepan.zhuk.account.balance.BalanceDto;
import ua.stepan.zhuk.account.balance.BalanceMapper;
import ua.stepan.zhuk.account.transaction.enums.TransactionDirection;
import ua.stepan.zhuk.outbox.OutboxService;
import ua.stepan.zhuk.outbox.enums.AggregationType;
import ua.stepan.zhuk.outbox.enums.EventType;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionService {
    private final AccountMapper accountMapper;
    private final BalanceMapper balanceMapper;
    private final TransactionMapper transactionMapper;
    private final OutboxService outboxService;

    @Transactional(rollbackFor = Throwable.class)
    public Transaction create(UUID accountId, CreateTransactionRequest request) {
        final Account account = accountMapper.findByAccountId(accountId)
                .orElseThrow(AccountNotFoundException::new);

        final Balance currentBalance = balanceMapper.lock(account.id(), request.currency())
                .orElseThrow(InvalidCurrencyException::new);

        final BigDecimal balanceAfter = calculateBalance(currentBalance.availableAmount(), request);

        final Balance updatedBalance = Balance.updateAmount(currentBalance, balanceAfter);
        balanceMapper.updateAmount(updatedBalance);

        final Transaction transaction = Transaction.create(
                account.id(),
                request.amount(),
                request.currency(),
                request.direction(),
                request.description().trim(),
                balanceAfter
        );
        final Long transactionId = transactionMapper.insert(transaction);
        final Transaction transactionWithId = Transaction.withId(transaction, transactionId);

        final BalanceDto balanceDto = BalanceDto.toDto(account.publicId(), updatedBalance);
        outboxService.create(AggregationType.BALANCE, updatedBalance.id(), EventType.BALANCE_UPDATED, balanceDto);

        outboxService.create(AggregationType.TRANSACTION, transactionWithId.id(), EventType.TRANSACTION_CREATED, transactionWithId);

        return transactionWithId;
    }

    @Transactional(readOnly = true)
    public List<Transaction> findByAccountId(UUID accountId) {
        final Account account = accountMapper.findByAccountId(accountId)
                .orElseThrow(InvalidAccountException::new);

        return transactionMapper.findByAccountId(account.id());
    }

    private BigDecimal calculateBalance(BigDecimal current, CreateTransactionRequest request) {
        if (request.direction() == TransactionDirection.IN) {
            return current.add(request.amount());
        }

        if (current.compareTo(request.amount()) < 0) {
            throw new InsufficientFundsException();
        }

        return current.subtract(request.amount());
    }
}
