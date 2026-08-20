package ua.stepan.zhuk.account;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ua.stepan.zhuk.account.enums.Currency;

import java.util.Set;
import java.util.UUID;

public record CreateAccountRequest(
        @NotNull
        UUID customerId,
        @NotNull
        @NotBlank
        @Size(min = 2, max = 45)
        String country,
        @NotNull
        @NotEmpty
        Set<@NotNull(message = "Invalid currency") Currency> currencies
) {

}
