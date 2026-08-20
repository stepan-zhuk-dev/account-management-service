package ua.stepan.zhuk.account;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ua.stepan.zhuk.account.balance.Balance;
import ua.stepan.zhuk.account.enums.Currency;
import ua.stepan.zhuk.infrastructure.exception.GlobalExceptionHandler;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AccountControllerTest {

    private final AccountService accountService = mock(AccountService.class);
    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new AccountController(accountService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @Test
    void givenValidRequest_whenCreateAccount_thenReturnsCreatedAccount() throws Exception {
        UUID accountId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        Account account = account(accountId, customerId, zeroBalance(Currency.EUR), zeroBalance(Currency.USD));

        when(accountService.create(any(CreateAccountRequest.class))).thenReturn(account);

        mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerId": "%s",
                                  "country": "Estonia",
                                  "currencies": ["EUR", "USD"]
                                }
                                """.formatted(customerId)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/accounts/" + accountId))
                .andExpect(jsonPath("$.accountId").value(accountId.toString()))
                .andExpect(jsonPath("$.customerId").value(customerId.toString()))
                .andExpect(jsonPath("$.balances[0].availableAmount").value(0.00))
                .andExpect(jsonPath("$.balances[0].currency").value("EUR"))
                .andExpect(jsonPath("$.balances[1].currency").value("USD"));

        verify(accountService).create(new CreateAccountRequest(
                customerId,
                "Estonia",
                Set.of(Currency.EUR, Currency.USD)
        ));
    }

    @Test
    void givenInvalidCreateAccountRequest_whenCreateAccount_thenReturnsValidationErrors() throws Exception {
        mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "country": " ",
                                  "currencies": []
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Request validation failed"))
                .andExpect(jsonPath("$.errors.customerId").value("must not be null"))
                .andExpect(jsonPath("$.errors.country").exists())
                .andExpect(jsonPath("$.errors.currencies").value("must not be empty"));

        verifyNoInteractions(accountService);
    }

    @Test
    void givenUnsupportedCurrency_whenCreateAccount_thenReturnsInvalidCurrency() throws Exception {
        mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerId": "00000000-0000-0000-0000-000000000001",
                                  "country": "Estonia",
                                  "currencies": ["EUR", "XXX"]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Invalid currency"));

        verifyNoInteractions(accountService);
    }

    @Test
    void givenMalformedJson_whenCreateAccount_thenReturnsMalformedJson() throws Exception {
        mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerId": "00000000-0000-0000-0000-000000000001",
                                  "country": "Estonia",
                                  "currencies": ["EUR"]
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Malformed JSON"));

        verifyNoInteractions(accountService);
    }

    @Test
    void givenExistingAccount_whenFindByAccountId_thenReturnsAccount() throws Exception {
        UUID accountId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        Account account = account(accountId, customerId, balance("15.50", Currency.EUR));

        when(accountService.findByAccountId(accountId)).thenReturn(account);

        mockMvc.perform(get("/api/v1/accounts/{accountId}", accountId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value(accountId.toString()))
                .andExpect(jsonPath("$.customerId").value(customerId.toString()))
                .andExpect(jsonPath("$.balances[0].availableAmount").value(15.50))
                .andExpect(jsonPath("$.balances[0].currency").value("EUR"));
    }

    @Test
    void givenMissingAccount_whenFindByAccountId_thenReturnsNotFound() throws Exception {
        UUID accountId = UUID.randomUUID();
        when(accountService.findByAccountId(accountId)).thenThrow(new AccountNotFoundException());

        mockMvc.perform(get("/api/v1/accounts/{accountId}", accountId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Account not found"));
    }

    private static Account account(UUID accountId, UUID customerId, Balance... balances) {
        return new Account(10L, accountId, customerId, "Estonia", 1L, 2L, List.of(balances));
    }

    private static Balance zeroBalance(Currency currency) {
        return balance("0.00", currency);
    }

    private static Balance balance(String amount, Currency currency) {
        return new Balance(
                20L,
                UUID.randomUUID(),
                10L,
                new BigDecimal(amount),
                currency,
                1L,
                2L
        );
    }
}
