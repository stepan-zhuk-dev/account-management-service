package ua.stepan.zhuk.account;

import org.junit.jupiter.api.Test;
import ua.stepan.zhuk.account.balance.Balance;
import ua.stepan.zhuk.account.balance.BalanceDto;
import ua.stepan.zhuk.account.balance.BalanceResponse;
import ua.stepan.zhuk.account.enums.Currency;
import ua.stepan.zhuk.account.transaction.Transaction;
import ua.stepan.zhuk.account.transaction.TransactionDto;
import ua.stepan.zhuk.account.transaction.TransactionHistoryResponse;
import ua.stepan.zhuk.account.transaction.TransactionResponse;
import ua.stepan.zhuk.account.transaction.enums.TransactionDirection;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ResponseMappingTest {

    @Test
    void givenAccountWithBalances_whenMappingToDtoAndResponse_thenExposesPublicIdsAndBalances() {
        UUID accountPublicId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        Balance eur = balance(3L, "12.34", Currency.EUR);
        Balance usd = balance(4L, "56.78", Currency.USD);
        Account account = new Account(1L, accountPublicId, customerId, "EE", 10L, 11L, List.of(eur, usd));

        AccountDto dto = AccountDto.toDto(account);
        AccountResponse response = AccountResponse.toResponse(account);

        assertThat(dto.accountId()).isEqualTo(accountPublicId);
        assertThat(dto.customerId()).isEqualTo(customerId);
        assertThat(dto.country()).isEqualTo("EE");
        assertThat(dto.balances()).containsExactly(
                BalanceDto.toDto(accountPublicId, eur),
                BalanceDto.toDto(accountPublicId, usd)
        );

        assertThat(response.accountId()).isEqualTo(accountPublicId);
        assertThat(response.customerId()).isEqualTo(customerId);
        assertThat(response.balances()).containsExactly(
                new BalanceResponse(new BigDecimal("12.34"), Currency.EUR),
                new BalanceResponse(new BigDecimal("56.78"), Currency.USD)
        );
    }

    @Test
    void givenTransaction_whenMappingToDtoAndResponses_thenExposesExpectedFields() {
        UUID accountPublicId = UUID.randomUUID();
        Transaction transaction = new Transaction(
                12L,
                UUID.randomUUID(),
                1L,
                new BigDecimal("99.99"),
                Currency.GBP,
                TransactionDirection.OUT,
                "card payment",
                new BigDecimal("100.01"),
                1234L
        );

        assertThat(TransactionDto.toDto(accountPublicId, transaction))
                .isEqualTo(new TransactionDto(
                        transaction.publicId(),
                        accountPublicId,
                        transaction.amount(),
                        transaction.currency(),
                        transaction.direction(),
                        transaction.description(),
                        transaction.balanceAfter(),
                        transaction.createdAt()
                ));
        assertThat(TransactionResponse.toResponse(accountPublicId, transaction))
                .isEqualTo(new TransactionResponse(
                        accountPublicId,
                        transaction.publicId(),
                        transaction.amount(),
                        transaction.currency(),
                        transaction.direction(),
                        transaction.description(),
                        transaction.balanceAfter()
                ));
        assertThat(TransactionHistoryResponse.toResponse(accountPublicId, transaction))
                .isEqualTo(new TransactionHistoryResponse(
                        accountPublicId,
                        transaction.publicId(),
                        transaction.amount(),
                        transaction.currency(),
                        transaction.direction(),
                        transaction.description()
                ));
    }

    @Test
    void givenBalanceFactoryMethods_whenCreatingAndUpdating_thenReturnsExpectedBalances() {
        Balance zero = Balance.createWithZeroAmount(10L, Currency.SEK);

        assertThat(zero.id()).isNull();
        assertThat(zero.publicId()).isNotNull();
        assertThat(zero.accountId()).isEqualTo(10L);
        assertThat(zero.availableAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(zero.currency()).isEqualTo(Currency.SEK);
        assertThat(zero.createdAt()).isPositive();
        assertThat(zero.updatedAt()).isEqualTo(zero.createdAt());

        Balance updated = Balance.updateAmount(zero, new BigDecimal("15.00"));
        assertThat(updated.publicId()).isEqualTo(zero.publicId());
        assertThat(updated.createdAt()).isEqualTo(zero.createdAt());
        assertThat(updated.availableAmount()).isEqualByComparingTo("15.00");
        assertThat(updated.updatedAt()).isGreaterThanOrEqualTo(zero.updatedAt());
    }

    private static Balance balance(Long id, String amount, Currency currency) {
        return new Balance(
                id,
                UUID.randomUUID(),
                1L,
                new BigDecimal(amount),
                currency,
                10L,
                11L
        );
    }
}
