package ua.stepan.zhuk.account;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import ua.stepan.zhuk.AbstractIntegrationTest;
import ua.stepan.zhuk.account.enums.Currency;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AccountControllerIntegrationTest extends AbstractIntegrationTest {

    @Test
    void givenValidCreateAccountRequest_whenPostAccount_thenCreatesAccountBalancesAndOutboxEvent() {
        UUID customerId = UUID.randomUUID();
        HttpEntity<CreateAccountRequest> request = createAccountRequest(
                customerId,
                "Estonia",
                Set.of(Currency.EUR, Currency.USD)
        );

        ResponseEntity<AccountResponse> response = restTemplate.postForEntity(
                url("/api/v1/accounts"),
                request,
                AccountResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getHeaders().getLocation()).isNotNull();

        AccountResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.customerId()).isEqualTo(customerId);
        assertThat(body.accountId()).isNotNull();
        assertThat(body.balances()).hasSize(2);

        assertThat(countRows("accounts")).isEqualTo(1L);
        assertThat(countRows("balances")).isEqualTo(2L);
        assertThat(countRows("outbox_messages WHERE event_type = 'ACCOUNT_CREATED'")).isEqualTo(1L);
    }

    @Test
    void givenUnsupportedCurrency_whenPostAccount_thenReturnsInvalidCurrencyAndDoesNotPersistAccount() {
        HttpEntity<InvalidCreateAccountPayload> request = invalidCreateAccountRequest(
                UUID.randomUUID(),
                "Latvia",
                List.of("EUR", "XXX")
        );

        ResponseEntity<ProblemDetail> response = restTemplate.postForEntity(
                url("/api/v1/accounts"),
                request,
                ProblemDetail.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        ProblemDetail body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getDetail()).isEqualTo("Invalid currency");

        assertThat(countRows("accounts")).isZero();
    }

    @Test
    void givenMissingAccount_whenGetAccount_thenReturnsNotFound() {
        UUID accountId = UUID.randomUUID();

        ResponseEntity<ProblemDetail> response = restTemplate.exchange(
                url("/api/v1/accounts/{accountId}"),
                HttpMethod.GET,
                new HttpEntity<>(jsonHeaders()),
                ProblemDetail.class,
                accountId
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        ProblemDetail body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getDetail()).isEqualTo("Account not found");
    }

    private Long countRows(String tableExpression) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableExpression, Long.class);
    }
}
