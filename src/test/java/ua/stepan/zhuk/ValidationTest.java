package ua.stepan.zhuk;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import ua.stepan.zhuk.account.CreateAccountRequest;
import ua.stepan.zhuk.account.enums.Currency;
import ua.stepan.zhuk.account.transaction.CreateTransactionRequest;
import ua.stepan.zhuk.account.transaction.enums.TransactionDirection;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class ValidationTest {

    private static final ValidatorFactory FACTORY = Validation.buildDefaultValidatorFactory();
    private static final Validator VALIDATOR = FACTORY.getValidator();

    @AfterAll
    static void closeValidatorFactory() {
        FACTORY.close();
    }

    @Test
    void givenInvalidCreateAccountRequest_whenValidate_thenReportsCustomerCountryAndCurrencies() {
        CreateAccountRequest request = new CreateAccountRequest(null, " ", Set.of());

        Map<String, Set<String>> messages = messagesByField(VALIDATOR.validate(request));

        assertThat(messages)
                .containsEntry("customerId", Set.of("must not be null"))
                .containsEntry("country", Set.of("must not be blank", "size must be between 2 and 45"))
                .containsEntry("currencies", Set.of("must not be empty"));
    }

    @Test
    void givenTooShortCountry_whenValidateCreateAccountRequest_thenReportsCountrySize() {
        CreateAccountRequest request = new CreateAccountRequest(UUID.randomUUID(), "E", Set.of(Currency.EUR));

        Map<String, Set<String>> messages = messagesByField(VALIDATOR.validate(request));

        assertThat(messages)
                .containsEntry("country", Set.of("size must be between 2 and 45"));
    }

    @Test
    void givenCreateAccountRequestWithNullCurrency_whenValidate_thenReportsInvalidCurrency() {
        Set<Currency> currencies = new HashSet<>();
        currencies.add(Currency.EUR);
        currencies.add(null);
        CreateAccountRequest request = new CreateAccountRequest(UUID.randomUUID(), "Estonia", currencies);

        Map<String, Set<String>> messages = messagesByField(VALIDATOR.validate(request));

        assertThat(messages)
                .containsEntry("currencies[].<iterable element>", Set.of("Invalid currency"));
    }

    @Test
    void givenValidCreateAccountRequest_whenValidate_thenReportsNoViolations() {
        CreateAccountRequest request = new CreateAccountRequest(
                UUID.randomUUID(),
                "Estonia",
                Set.of(Currency.EUR, Currency.USD)
        );

        assertThat(VALIDATOR.validate(request)).isEmpty();
    }

    @Test
    void givenInvalidCreateTransactionRequest_whenValidate_thenReportsRequiredFields() {
        CreateTransactionRequest request = new CreateTransactionRequest(
                BigDecimal.ZERO,
                null,
                null,
                " "
        );

        Map<String, Set<String>> messages = messagesByField(VALIDATOR.validate(request));

        assertThat(messages)
                .containsEntry("amount", Set.of("Invalid amount"))
                .containsEntry("currency", Set.of("must not be null"))
                .containsEntry("direction", Set.of("must not be null"))
                .containsEntry("description", Set.of("Description missing"));
    }

    @Test
    void givenTransactionWithTooManyFractionDigitsAndLongDescription_whenValidate_thenReportsAmountAndDescription() {
        CreateTransactionRequest request = new CreateTransactionRequest(
                new BigDecimal("1.999"),
                Currency.EUR,
                TransactionDirection.IN,
                "x".repeat(256)
        );

        Map<String, Set<String>> messages = messagesByField(VALIDATOR.validate(request));

        assertThat(messages)
                .containsEntry(
                        "amount",
                        Set.of("numeric value out of bounds (<17 digits>.<2 digits> expected)")
                )
                .containsEntry("description", Set.of("size must be between 0 and 255"));
    }

    @Test
    void givenValidCreateTransactionRequest_whenValidate_thenReportsNoViolations() {
        CreateTransactionRequest request = new CreateTransactionRequest(
                new BigDecimal("10.55"),
                Currency.GBP,
                TransactionDirection.OUT,
                "transfer"
        );

        assertThat(VALIDATOR.validate(request)).isEmpty();
    }

    private static Map<String, Set<String>> messagesByField(Set<? extends ConstraintViolation<?>> violations) {
        return violations.stream()
                .collect(Collectors.groupingBy(
                        violation -> violation.getPropertyPath().toString(),
                        Collectors.mapping(ConstraintViolation::getMessage, Collectors.toSet())
                ));
    }
}
