package ua.stepan.zhuk.outbox.enums;

import lombok.Getter;

@Getter
public enum EventType {
    ACCOUNT_CREATED("account.created"),
    BALANCE_UPDATED("balance.updated"),
    TRANSACTION_CREATED("transaction.created");

    private final String routingKey;

    EventType(String routingKey) {
        this.routingKey = routingKey;
    }
}
