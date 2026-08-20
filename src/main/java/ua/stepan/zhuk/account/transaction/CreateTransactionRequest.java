package ua.stepan.zhuk.account.transaction;

import jakarta.validation.constraints.*;
import ua.stepan.zhuk.account.enums.Currency;
import ua.stepan.zhuk.account.transaction.enums.TransactionDirection;

import java.math.BigDecimal;

public record CreateTransactionRequest(
        @NotNull
        @DecimalMin(value = "0.0", inclusive = false, message = "Invalid amount")
        @Digits(integer = 17, fraction = 2)
        BigDecimal amount,
        @NotNull
        Currency currency,
        @NotNull
        TransactionDirection direction,
        @NotNull(message = "Description missing")
        @NotBlank(message = "Description missing")
        @Size(max = 255)
        String description
) {
}
