CREATE SEQUENCE account_id_seq
    AS BIGINT
    START WITH 1
    INCREMENT BY 1
    CACHE 50;

CREATE SEQUENCE balance_id_seq
    AS BIGINT
    START WITH 1
    INCREMENT BY 1
    CACHE 50;

CREATE SEQUENCE transaction_id_seq
    AS BIGINT
    START WITH 1
    INCREMENT BY 1
    CACHE 50;

CREATE TABLE accounts
(
    id          BIGINT     NOT NULL DEFAULT nextval('account_id_seq'),
    public_id   UUID       NOT NULL,
    customer_id UUID       NOT NULL,
    country     VARCHAR(45) NOT NULL,
    created_at  BIGINT     NOT NULL,
    updated_at  BIGINT     NOT NULL,

    CONSTRAINT pk_accounts
        PRIMARY KEY (id),

    CONSTRAINT uq_accounts_public_id
        UNIQUE (public_id)
);

ALTER SEQUENCE account_id_seq
    OWNED BY accounts.id;

CREATE TABLE balances
(
    id               BIGINT         NOT NULL DEFAULT nextval('balance_id_seq'),
    public_id        UUID           NOT NULL,
    account_id       BIGINT         NOT NULL,
    available_amount NUMERIC(19, 4) NOT NULL DEFAULT 0,
    currency         VARCHAR(3)     NOT NULL,
    created_at       BIGINT         NOT NULL,
    updated_at       BIGINT         NOT NULL,

    CONSTRAINT pk_balances
        PRIMARY KEY (id),

    CONSTRAINT uq_balances_public_id
        UNIQUE (public_id),

    CONSTRAINT fk_balances_account
        FOREIGN KEY (account_id)
            REFERENCES accounts (id)
            ON DELETE CASCADE
);

ALTER SEQUENCE balance_id_seq
    OWNED BY balances.id;

CREATE INDEX idx_balances_account_currency
    ON balances (account_id, currency);

CREATE TABLE transactions
(
    id            BIGINT         NOT NULL DEFAULT nextval('transaction_id_seq'),
    public_id     UUID           NOT NULL,
    account_id    BIGINT         NOT NULL,
    amount        NUMERIC(19, 4) NOT NULL,
    currency      VARCHAR(3)     NOT NULL,
    direction     VARCHAR(3)     NOT NULL,
    description   VARCHAR(500)   NOT NULL,
    balance_after NUMERIC(19, 4) NOT NULL,
    created_at    BIGINT         NOT NULL,

    CONSTRAINT pk_transactions
        PRIMARY KEY (id),

    CONSTRAINT uq_transactions_public_id
        UNIQUE (public_id),

    CONSTRAINT fk_transactions_account
        FOREIGN KEY (account_id)
            REFERENCES accounts (id)
            ON DELETE RESTRICT
);

ALTER SEQUENCE transaction_id_seq
    OWNED BY transactions.id;

CREATE TABLE outbox_messages
(
    id             UUID         NOT NULL,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id   BIGINT       NOT NULL,
    event_type     VARCHAR(100) NOT NULL,
    routing_key    VARCHAR(255) NOT NULL,
    payload        JSONB        NOT NULL,
    created_at     BIGINT       NOT NULL,
    retry_count    INTEGER      NOT NULL DEFAULT 0,
    published_at   BIGINT,
    last_error     VARCHAR(1000),

    CONSTRAINT pk_outbox_messages
        PRIMARY KEY (id)
);
