package ua.stepan.zhuk.infrastructure.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.web.MockHttpServletRequest;
import ua.stepan.zhuk.account.AccountNotFoundException;
import ua.stepan.zhuk.account.InvalidAccountException;
import ua.stepan.zhuk.account.transaction.InsufficientFundsException;
import ua.stepan.zhuk.account.transaction.InvalidCurrencyException;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/accounts");

    @Test
    void givenAccountNotFoundException_whenHandlingNotFound_thenBuildsProblemDetail() {
        ProblemDetail detail = handler.notFound(new AccountNotFoundException(), request);

        assertThat(detail.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(detail.getTitle()).isEqualTo("Not Found");
        assertThat(detail.getDetail()).isEqualTo("Account not found");
        assertThat(detail.getInstance()).isEqualTo(URI.create("/api/v1/accounts"));
    }

    @Test
    void givenInvalidCurrencyOrAccount_whenHandlingBadRequest_thenBuildsProblemDetail() {
        ProblemDetail currency = handler.badRequest(new InvalidCurrencyException(), request);
        ProblemDetail account = handler.badRequest(new InvalidAccountException(), request);

        assertThat(currency.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(currency.getDetail()).isEqualTo("Invalid currency");
        assertThat(account.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(account.getDetail()).isEqualTo("Invalid account");
    }

    @Test
    void givenInsufficientFundsException_whenHandlingUnprocessableContent_thenBuildsProblemDetail() {
        ProblemDetail detail = handler.unprocessableContent(new InsufficientFundsException(), request);

        assertThat(detail.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT.value());
        assertThat(detail.getTitle()).isEqualTo("Unprocessable Content");
        assertThat(detail.getDetail()).isEqualTo("Insufficient funds");
    }

    @Test
    void givenMalformedMessage_whenHandlingMalformed_thenDistinguishesDirectionFromCurrencyOrJsonErrors() {
        ProblemDetail direction = handler.malformed(
                new HttpMessageNotReadableException("Cannot parse TransactionDirection", null),
                request
        );
        ProblemDetail currency = handler.malformed(
                new HttpMessageNotReadableException("Cannot parse Currency", null),
                request
        );
        ProblemDetail generic = handler.malformed(new HttpMessageNotReadableException("bad json", null), request);

        assertThat(direction.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(direction.getDetail()).isEqualTo("Invalid direction");
        assertThat(currency.getDetail()).isEqualTo("Invalid currency");
        assertThat(generic.getDetail()).isEqualTo("Malformed JSON");
    }
}
