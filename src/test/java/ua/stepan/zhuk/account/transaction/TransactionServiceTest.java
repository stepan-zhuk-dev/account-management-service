package ua.stepan.zhuk.account.transaction;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ua.stepan.zhuk.account.Account;
import ua.stepan.zhuk.account.AccountMapper;
import ua.stepan.zhuk.account.AccountNotFoundException;
import ua.stepan.zhuk.account.InvalidAccountException;
import ua.stepan.zhuk.account.balance.Balance;
import ua.stepan.zhuk.account.balance.BalanceDto;
import ua.stepan.zhuk.account.balance.BalanceMapper;
import ua.stepan.zhuk.account.enums.Currency;
import ua.stepan.zhuk.account.transaction.enums.TransactionDirection;
import ua.stepan.zhuk.outbox.OutboxService;
import ua.stepan.zhuk.outbox.enums.AggregationType;
import ua.stepan.zhuk.outbox.enums.EventType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    private static final UUID ACCOUNT_PUBLIC_ID = UUID.randomUUID();
    private static final Long ACCOUNT_ID = 11L;

    @Mock
    private AccountMapper accountMapper;

    @Mock
    private BalanceMapper balanceMapper;

    @Mock
    private TransactionMapper transactionMapper;

    @Mock
    private OutboxService outboxService;

    @InjectMocks
    private TransactionService service;

    @Test
    void givenIncomingTransaction_whenCreate_thenAddsFundsAndPublishesEvents() {
        CreateTransactionRequest request = request("25.50", Currency.EUR, TransactionDirection.IN, " salary ");
        Balance current = balance("100.00", Currency.EUR);

        when(accountMapper.findByAccountId(ACCOUNT_PUBLIC_ID)).thenReturn(Optional.of(account()));
        when(balanceMapper.lock(ACCOUNT_ID, Currency.EUR)).thenReturn(Optional.of(current));
        when(transactionMapper.insert(any(Transaction.class))).thenReturn(31L);

        Transaction transaction = service.create(ACCOUNT_PUBLIC_ID, request);

        assertThat(transaction.id()).isEqualTo(31L);
        assertThat(transaction.publicId()).isNotNull();
        assertThat(transaction.accountId()).isEqualTo(ACCOUNT_ID);
        assertThat(transaction.amount()).isEqualByComparingTo("25.50");
        assertThat(transaction.direction()).isEqualTo(TransactionDirection.IN);
        assertThat(transaction.description()).isEqualTo("salary");
        assertThat(transaction.balanceAfter()).isEqualByComparingTo("125.50");

        ArgumentCaptor<Balance> updatedBalance = ArgumentCaptor.forClass(Balance.class);
        verify(balanceMapper).updateAmount(updatedBalance.capture());
        assertThat(updatedBalance.getValue().availableAmount())
                .isEqualByComparingTo("125.50");

        ArgumentCaptor<Transaction> insertedTransaction = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionMapper).insert(insertedTransaction.capture());

        assertThat(insertedTransaction.getValue().id()).isNull();
        assertThat(insertedTransaction.getValue().publicId()).isEqualTo(transaction.publicId());
        assertThat(insertedTransaction.getValue().balanceAfter()).isEqualByComparingTo("125.50");

        ArgumentCaptor<BalanceDto> balanceEvent = ArgumentCaptor.forClass(BalanceDto.class);
        verify(outboxService).create(
                eq(AggregationType.BALANCE),
                eq(21L),
                eq(EventType.BALANCE_UPDATED),
                balanceEvent.capture()
        );

        assertThat(balanceEvent.getValue().accountId()).isEqualTo(ACCOUNT_PUBLIC_ID);
        assertThat(balanceEvent.getValue().availableAmount()).isEqualByComparingTo("125.50");

        verify(outboxService).create(
                AggregationType.TRANSACTION,
                transaction.id(),
                EventType.TRANSACTION_CREATED,
                transaction
        );
    }

    @Test
    void givenOutgoingTransactionWithAvailableFunds_whenCreate_thenSubtractsFunds() {
        CreateTransactionRequest request = request("75.25", Currency.USD, TransactionDirection.OUT, "ATM");

        when(accountMapper.findByAccountId(ACCOUNT_PUBLIC_ID)).thenReturn(Optional.of(account()));
        when(balanceMapper.lock(ACCOUNT_ID, Currency.USD)).thenReturn(Optional.of(balance("100.00", Currency.USD)));

        Transaction transaction = service.create(ACCOUNT_PUBLIC_ID, request);

        assertThat(transaction.balanceAfter()).isEqualByComparingTo("24.75");

        ArgumentCaptor<Balance> updatedBalance = ArgumentCaptor.forClass(Balance.class);
        verify(balanceMapper).updateAmount(updatedBalance.capture());
        assertThat(updatedBalance.getValue().availableAmount()).isEqualByComparingTo("24.75");
    }

    @Test
    void givenMissingAccount_whenCreate_thenThrowsAccountNotFoundException() {
        CreateTransactionRequest request = request("10.00", Currency.EUR, TransactionDirection.IN, "top up");

        when(accountMapper.findByAccountId(ACCOUNT_PUBLIC_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(ACCOUNT_PUBLIC_ID, request))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessage("Account not found");

        verify(balanceMapper, never()).lock(any(), any());
    }

    @Test
    void givenUnsupportedCurrency_whenCreate_thenThrowsInvalidCurrencyException() {
        CreateTransactionRequest request = request("10.00", Currency.GBP, TransactionDirection.IN, "top up");
        when(accountMapper.findByAccountId(ACCOUNT_PUBLIC_ID)).thenReturn(Optional.of(account()));
        when(balanceMapper.lock(ACCOUNT_ID, Currency.GBP)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(ACCOUNT_PUBLIC_ID, request))
                .isInstanceOf(InvalidCurrencyException.class)
                .hasMessage("Invalid currency");

        verify(balanceMapper, never()).updateAmount(any());
        verify(transactionMapper, never()).insert(any());
    }

    @Test
    void givenOutgoingAmountExceedsBalance_whenCreate_thenThrowsInsufficientFundsException() {
        CreateTransactionRequest request = request("101.00", Currency.EUR, TransactionDirection.OUT, "transfer");
        when(accountMapper.findByAccountId(ACCOUNT_PUBLIC_ID)).thenReturn(Optional.of(account()));
        when(balanceMapper.lock(ACCOUNT_ID, Currency.EUR)).thenReturn(Optional.of(balance("99.99", Currency.EUR)));

        assertThatThrownBy(() -> service.create(ACCOUNT_PUBLIC_ID, request))
                .isInstanceOf(InsufficientFundsException.class)
                .hasMessage("Insufficient funds");

        verify(balanceMapper, never()).updateAmount(any());
        verify(transactionMapper, never()).insert(any());
    }

    @Test
    void givenExistingAccountWithTransactions_whenFindByAccountId_thenReturnsHistoryResponses() {
        Transaction first = transaction("10.00", Currency.EUR, TransactionDirection.IN, "salary");
        Transaction second = transaction("3.00", Currency.USD, TransactionDirection.OUT, "fee");
        when(accountMapper.findByAccountId(ACCOUNT_PUBLIC_ID)).thenReturn(Optional.of(account()));
        when(transactionMapper.findByAccountId(ACCOUNT_ID)).thenReturn(List.of(first, second));

        assertThat(service.findByAccountId(ACCOUNT_PUBLIC_ID))
                .containsExactly(first, second);
    }

    @Test
    void givenMissingAccount_whenFindByAccountId_thenThrowsInvalidAccountException() {
        when(accountMapper.findByAccountId(ACCOUNT_PUBLIC_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByAccountId(ACCOUNT_PUBLIC_ID))
                .isInstanceOf(InvalidAccountException.class)
                .hasMessage("Invalid account");
    }

    private static Account account() {
        return new Account(ACCOUNT_ID, ACCOUNT_PUBLIC_ID, UUID.randomUUID(), "EE", 1L, 2L, List.of());
    }

    private static Balance balance(String amount, Currency currency) {
        return new Balance(
                21L,
                UUID.randomUUID(),
                ACCOUNT_ID,
                new BigDecimal(amount),
                currency,
                1L,
                2L
        );
    }

    private static CreateTransactionRequest request(
            String amount,
            Currency currency,
            TransactionDirection direction,
            String description
    ) {
        return new CreateTransactionRequest(new BigDecimal(amount), currency, direction, description);
    }

    private static Transaction transaction(
            String amount,
            Currency currency,
            TransactionDirection direction,
            String description
    ) {
        return Transaction.create(
                ACCOUNT_ID,
                new BigDecimal(amount),
                currency,
                direction,
                description,
                new BigDecimal(amount)
        );
    }
}
