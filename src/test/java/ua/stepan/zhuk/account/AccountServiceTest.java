package ua.stepan.zhuk.account;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ua.stepan.zhuk.account.balance.Balance;
import ua.stepan.zhuk.account.balance.BalanceMapper;
import ua.stepan.zhuk.account.enums.Currency;
import ua.stepan.zhuk.outbox.OutboxService;
import ua.stepan.zhuk.outbox.enums.AggregationType;
import ua.stepan.zhuk.outbox.enums.EventType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountMapper accountMapper;

    @Mock
    private BalanceMapper balanceMapper;

    @Mock
    private OutboxService outboxService;

    @InjectMocks
    private AccountService service;

    @Test
    void givenValidRequest_whenCreate_thenPersistsAccountBalancesAndPublishesEvent() {
        UUID customerId = UUID.randomUUID();
        CreateAccountRequest request = new CreateAccountRequest(
                customerId,
                "EE",
                Set.of(Currency.EUR, Currency.USD)
        );

        when(accountMapper.insert(any(Account.class))).thenReturn(42L);
        when(balanceMapper.insert(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Account created = service.create(request);

        assertThat(created.id()).isEqualTo(42L);
        assertThat(created.publicId()).isNotNull();
        assertThat(created.customerId()).isEqualTo(customerId);
        assertThat(created.country()).isEqualTo("EE");
        assertThat(created.balances())
                .hasSize(2)
                .allSatisfy(balance -> {
                    assertThat(balance.accountId()).isEqualTo(42L);
                    assertThat(balance.availableAmount()).isEqualByComparingTo(BigDecimal.ZERO);
                    assertThat(balance.publicId()).isNotNull();
                });

        ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);
        verify(accountMapper).insert(accountCaptor.capture());
        assertThat(accountCaptor.getValue().publicId()).isEqualTo(created.publicId());
        assertThat(accountCaptor.getValue().customerId()).isEqualTo(customerId);

        ArgumentCaptor<AccountDto> eventCaptor = ArgumentCaptor.forClass(AccountDto.class);
        verify(outboxService).create(
                eq(AggregationType.ACCOUNT),
                eq(42L),
                eq(EventType.ACCOUNT_CREATED),
                eventCaptor.capture()
        );

        assertThat(eventCaptor.getValue().accountId()).isEqualTo(created.publicId());
        assertThat(eventCaptor.getValue().customerId()).isEqualTo(customerId);
        assertThat(eventCaptor.getValue().balances()).hasSize(2);
    }

    @Test
    void givenExistingAccount_whenFindByAccountId_thenReturnsAccountWithBalances() {
        UUID accountId = UUID.randomUUID();
        Account account = account(accountId);

        when(accountMapper.findByAccountId(accountId))
                .thenReturn(Optional.of(Account.withIdAndBalances(account, account.id(), null)));
        when(balanceMapper.findByAccountId(account.id())).thenReturn(account.balances());

        assertThat(service.findByAccountId(accountId)).isEqualTo(account);
    }

    @Test
    void givenMissingAccount_whenFindByAccountId_thenThrowsAccountNotFoundException() {
        UUID accountId = UUID.randomUUID();
        when(accountMapper.findByAccountId(accountId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByAccountId(accountId))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessage("Account not found");
    }

    private static Account account(UUID accountId) {
        return new Account(
                7L,
                accountId,
                UUID.randomUUID(),
                "EE",
                1L,
                2L,
                List.of(new Balance(9L, UUID.randomUUID(), 7L, BigDecimal.TEN, Currency.EUR, 1L, 2L))
        );
    }
}
