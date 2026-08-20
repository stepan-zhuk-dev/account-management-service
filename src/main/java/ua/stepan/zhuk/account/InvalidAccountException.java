package ua.stepan.zhuk.account;

public class InvalidAccountException extends RuntimeException {
    public InvalidAccountException() {
        super("Invalid account");
    }
}
