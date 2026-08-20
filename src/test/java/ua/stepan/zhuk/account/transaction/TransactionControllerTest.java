package ua.stepan.zhuk.account.transaction;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ua.stepan.zhuk.account.AccountNotFoundException;
import ua.stepan.zhuk.account.InvalidAccountException;
import ua.stepan.zhuk.account.enums.Currency;
import ua.stepan.zhuk.account.transaction.enums.TransactionDirection;
import ua.stepan.zhuk.infrastructure.exception.GlobalExceptionHandler;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TransactionControllerTest {

    private final TransactionService transactionService = mock(TransactionService.class);
    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new TransactionController(transactionService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @Test
    void givenValidRequest_whenCreateTransaction_thenReturnsCreatedTransaction() throws Exception {
        UUID accountId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();
        Transaction transaction = transaction(
                transactionId,
                "25.50",
                Currency.EUR,
                TransactionDirection.IN,
                "salary",
                "125.50"
        );

        when(transactionService.create(eq(accountId), any(CreateTransactionRequest.class))).thenReturn(transaction);

        mockMvc.perform(post("/api/v1/accounts/{accountId}/transactions", accountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "amount": 25.50,
                                  "currency": "EUR",
                                  "direction": "IN",
                                  "description": "salary"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location",
                        "http://localhost/api/v1/accounts/" + accountId + "/transactions"
                ))
                .andExpect(jsonPath("$.accountId").value(accountId.toString()))
                .andExpect(jsonPath("$.transactionId").value(transactionId.toString()))
                .andExpect(jsonPath("$.amount").value(25.50))
                .andExpect(jsonPath("$.currency").value("EUR"))
                .andExpect(jsonPath("$.direction").value("IN"))
                .andExpect(jsonPath("$.description").value("salary"))
                .andExpect(jsonPath("$.balanceAfter").value(125.50));

        verify(transactionService).create(
                accountId,
                new CreateTransactionRequest(
                        new BigDecimal("25.50"),
                        Currency.EUR,
                        TransactionDirection.IN,
                        "salary"
                )
        );
    }

    @Test
    void givenNegativeAmount_whenCreateTransaction_thenReturnsInvalidAmount() throws Exception {
        UUID accountId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/accounts/{accountId}/transactions", accountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "amount": -10.00,
                                  "currency": "EUR",
                                  "direction": "IN",
                                  "description": "top up"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Request validation failed"))
                .andExpect(jsonPath("$.errors.amount").value("Invalid amount"));

        verifyNoInteractions(transactionService);
    }

    @Test
    void givenMissingDescription_whenCreateTransaction_thenReturnsDescriptionMissing() throws Exception {
        UUID accountId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/accounts/{accountId}/transactions", accountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "amount": 10.00,
                                  "currency": "EUR",
                                  "direction": "IN",
                                  "description": " "
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Request validation failed"))
                .andExpect(jsonPath("$.errors.description").value("Description missing"));

        verifyNoInteractions(transactionService);
    }

    @Test
    void givenUnsupportedCurrency_whenCreateTransaction_thenReturnsInvalidCurrency() throws Exception {
        UUID accountId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/accounts/{accountId}/transactions", accountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "amount": 10.00,
                                  "currency": "XXX",
                                  "direction": "IN",
                                  "description": "top up"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Invalid currency"));

        verifyNoInteractions(transactionService);
    }

    @Test
    void givenUnsupportedDirection_whenCreateTransaction_thenReturnsInvalidTransactionDirection() throws Exception {
        UUID accountId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/accounts/{accountId}/transactions", accountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "amount": 10.00,
                                  "currency": "EUR",
                                  "direction": "SIDEWAYS",
                                  "description": "top up"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Invalid direction"));

        verifyNoInteractions(transactionService);
    }

    @Test
    void givenMissingAccount_whenCreateTransaction_thenReturnsNotFound() throws Exception {
        UUID accountId = UUID.randomUUID();
        when(transactionService.create(eq(accountId), any(CreateTransactionRequest.class)))
                .thenThrow(new AccountNotFoundException());

        mockMvc.perform(post("/api/v1/accounts/{accountId}/transactions", accountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validTransactionRequest()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Account not found"));
    }

    @Test
    void givenCurrencyNotConfiguredForAccount_whenCreateTransaction_thenReturnsBadRequest() throws Exception {
        UUID accountId = UUID.randomUUID();
        when(transactionService.create(eq(accountId), any(CreateTransactionRequest.class)))
                .thenThrow(new InvalidCurrencyException());

        mockMvc.perform(post("/api/v1/accounts/{accountId}/transactions", accountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validTransactionRequest()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Invalid currency"));
    }

    @Test
    void givenInsufficientFunds_whenCreateTransaction_thenReturnsUnprocessableContent() throws Exception {
        UUID accountId = UUID.randomUUID();
        when(transactionService.create(eq(accountId), any(CreateTransactionRequest.class)))
                .thenThrow(new InsufficientFundsException());

        mockMvc.perform(post("/api/v1/accounts/{accountId}/transactions", accountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validTransactionRequest()))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("Insufficient funds"));
    }

    @Test
    void givenExistingAccountWithTransactions_whenFindByAccountId_thenReturnsHistory() throws Exception {
        UUID accountId = UUID.randomUUID();
        UUID firstTransactionId = UUID.randomUUID();
        UUID secondTransactionId = UUID.randomUUID();

        when(transactionService.findByAccountId(accountId))
                .thenReturn(List.of(
                        transaction(
                                firstTransactionId,
                                "10.00",
                                Currency.EUR,
                                TransactionDirection.IN,
                                "salary",
                                "30.00"
                        ),
                        transaction(
                                secondTransactionId,
                                "3.50",
                                Currency.EUR,
                                TransactionDirection.OUT,
                                "fee",
                                "26.50"
                        )
                ));

        mockMvc.perform(get("/api/v1/accounts/{accountId}/transactions", accountId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].accountId").value(accountId.toString()))
                .andExpect(jsonPath("$[0].transactionId").value(firstTransactionId.toString()))
                .andExpect(jsonPath("$[0].amount").value(10.00))
                .andExpect(jsonPath("$[0].direction").value("IN"))
                .andExpect(jsonPath("$[1].transactionId").value(secondTransactionId.toString()))
                .andExpect(jsonPath("$[1].amount").value(3.50))
                .andExpect(jsonPath("$[1].direction").value("OUT"));
    }

    @Test
    void givenInvalidAccount_whenFindByAccountId_thenReturnsBadRequest() throws Exception {
        UUID accountId = UUID.randomUUID();

        when(transactionService.findByAccountId(accountId))
                .thenThrow(new InvalidAccountException());

        mockMvc.perform(get("/api/v1/accounts/{accountId}/transactions", accountId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Invalid account"));
    }

    private static Transaction transaction(
            UUID transactionId,
            String amount,
            Currency currency,
            TransactionDirection direction,
            String description,
            String balanceAfter
    ) {
        return new Transaction(
                30L,
                transactionId,
                10L,
                new BigDecimal(amount),
                currency,
                direction,
                description,
                new BigDecimal(balanceAfter),
                1L
        );
    }

    private static String validTransactionRequest() {
        return """
                {
                  "amount": 10.00,
                  "currency": "EUR",
                  "direction": "IN",
                  "description": "top up"
                }
                """;
    }
}
