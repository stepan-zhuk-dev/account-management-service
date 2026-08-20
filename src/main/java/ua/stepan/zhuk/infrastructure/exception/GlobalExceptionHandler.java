package ua.stepan.zhuk.infrastructure.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import ua.stepan.zhuk.account.AccountNotFoundException;
import ua.stepan.zhuk.account.InvalidAccountException;
import ua.stepan.zhuk.account.transaction.InsufficientFundsException;
import ua.stepan.zhuk.account.transaction.InvalidCurrencyException;

import java.net.URI;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AccountNotFoundException.class)
    public ProblemDetail notFound(AccountNotFoundException exception, HttpServletRequest request) {
        return buildProblemDetail(HttpStatus.NOT_FOUND, exception.getMessage(), request);
    }

    @ExceptionHandler({InvalidCurrencyException.class, InvalidAccountException.class})
    public ProblemDetail badRequest(RuntimeException exception, HttpServletRequest request) {
        return buildProblemDetail(HttpStatus.BAD_REQUEST, exception.getMessage(), request);
    }

    @ExceptionHandler(InsufficientFundsException.class)
    public ProblemDetail unprocessableContent(InsufficientFundsException exception, HttpServletRequest request) {
        return buildProblemDetail(HttpStatus.UNPROCESSABLE_CONTENT, exception.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail validation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        final ProblemDetail detail = buildProblemDetail(HttpStatus.BAD_REQUEST, "Request validation failed", request);
        final Map<String, String> errors = exception.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(FieldError::getField,
                        error -> error.getDefaultMessage() == null ? "invalid value" : error.getDefaultMessage(),
                        (first, ignored) -> first));
        detail.setProperty("errors", errors);

        return detail;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail malformed(HttpMessageNotReadableException exception, HttpServletRequest request) {
        return buildProblemDetail(HttpStatus.BAD_REQUEST, getMessageFromMalformed(exception), request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail malformed(MethodArgumentTypeMismatchException ignored, HttpServletRequest request) {
        return buildProblemDetail(HttpStatus.BAD_REQUEST, "Request validation failed", request);
    }

    @NonNull
    private static String getMessageFromMalformed(HttpMessageNotReadableException exception) {
        if (exception.getMessage() != null && exception.getMessage().contains("TransactionDirection")) {
            return "Invalid direction";
        }

        if (exception.getMessage() != null && exception.getMessage().contains("Currency")) {
            return "Invalid currency";
        }

        return "Malformed JSON";
    }

    private ProblemDetail buildProblemDetail(HttpStatus status, String message, HttpServletRequest request) {
        final ProblemDetail detail = ProblemDetail.forStatusAndDetail(status, message);
        detail.setTitle(status.getReasonPhrase());
        detail.setInstance(URI.create(request.getRequestURI()));

        return detail;
    }

}
