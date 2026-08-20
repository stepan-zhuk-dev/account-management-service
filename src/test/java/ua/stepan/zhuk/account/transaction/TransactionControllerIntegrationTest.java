package ua.stepan.zhuk.account.transaction;

import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import ua.stepan.zhuk.AbstractIntegrationTest;
import ua.stepan.zhuk.account.AccountResponse;
import ua.stepan.zhuk.account.enums.Currency;
import ua.stepan.zhuk.account.transaction.enums.TransactionDirection;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionControllerIntegrationTest extends AbstractIntegrationTest {

    @Test
    void givenAccountWithFunds_whenPostTransactions_thenUpdatesBalanceAndReturnsHistory() {
        UUID accountId = createAccount(UUID.randomUUID(), "Estonia", Set.of(Currency.EUR));
        HttpEntity<CreateTransactionRequest> incomingRequest = transactionRequest(
                new BigDecimal("100.00"),
                Currency.EUR,
                TransactionDirection.IN,
                "salary"
        );

        ResponseEntity<TransactionResponse> incoming = restTemplate.postForEntity(
                url("/api/v1/accounts/{accountId}/transactions"),
                incomingRequest,
                TransactionResponse.class,
                accountId
        );

        HttpEntity<CreateTransactionRequest> outgoingRequest = transactionRequest(
                new BigDecimal("40.25"),
                Currency.EUR,
                TransactionDirection.OUT,
                "card payment"
        );

        ResponseEntity<TransactionResponse> outgoing = restTemplate.postForEntity(
                url("/api/v1/accounts/{accountId}/transactions"),
                outgoingRequest,
                TransactionResponse.class,
                accountId
        );

        assertThat(incoming.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        TransactionResponse incomingBody = incoming.getBody();
        assertThat(incomingBody).isNotNull();
        assertThat(incomingBody.balanceAfter()).isEqualByComparingTo("100.00");

        assertThat(outgoing.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        TransactionResponse outgoingBody = outgoing.getBody();
        assertThat(outgoingBody).isNotNull();
        assertThat(outgoingBody.balanceAfter()).isEqualByComparingTo("59.75");

        ResponseEntity<AccountResponse> accountResponse = restTemplate.getForEntity(
                url("/api/v1/accounts/{accountId}"),
                AccountResponse.class,
                accountId
        );

        assertThat(accountResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        AccountResponse accountBody = accountResponse.getBody();
        assertThat(accountBody).isNotNull();
        assertThat(accountBody.balances()).singleElement()
                .satisfies(balance -> {
                    assertThat(balance.currency().name()).isEqualTo("EUR");
                    assertThat(balance.availableAmount()).isEqualByComparingTo("59.75");
                });

        ResponseEntity<List<TransactionHistoryResponse>> history = restTemplate.exchange(
                url("/api/v1/accounts/{accountId}/transactions"),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<>() {
                },
                accountId
        );

        assertThat(history.getStatusCode()).isEqualTo(HttpStatus.OK);

        List<TransactionHistoryResponse> historyBody = history.getBody();
        assertThat(historyBody).isNotNull();
        assertThat(historyBody).hasSize(2);

        assertThat(countRows("transactions")).isEqualTo(2L);
        assertThat(countRows("outbox_messages")).isEqualTo(5L);
    }

    @Test
    void givenInsufficientFunds_whenPostOutgoingTransaction_thenReturnsUnprocessableContent() {
        UUID accountId = createAccount(UUID.randomUUID(), "Latvia", Set.of(Currency.USD));
        HttpEntity<CreateTransactionRequest> request = transactionRequest(
                new BigDecimal("1.00"),
                Currency.USD,
                TransactionDirection.OUT,
                "card payment"
        );

        ResponseEntity<ProblemDetail> response = restTemplate.postForEntity(
                url("/api/v1/accounts/{accountId}/transactions"),
                request,
                ProblemDetail.class,
                accountId
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);

        ProblemDetail body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getDetail()).contains("Insufficient funds");

        assertThat(countRows("transactions")).isZero();
    }

    private Long countRows(String tableName) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableName, Long.class);
    }
}
