package ua.stepan.zhuk.account.transaction;

public class InvalidCurrencyException extends RuntimeException {
    public InvalidCurrencyException() {
        super("Invalid currency");
    }
}
