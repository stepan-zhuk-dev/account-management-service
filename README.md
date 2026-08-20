# Account Management Service

Spring Boot API for creating customer accounts, keeping multi-currency balances, recording deposits and withdrawals, returning transaction history, and persisting outbox events for downstream publishing.

## Features

- Create accounts with one balance per requested currency.
- Deposit funds with `IN` transactions.
- Withdraw funds with `OUT` transactions.
- Reject withdrawals when the selected currency balance is too low.
- Return account details and transaction history by public account ID.
- Persist account, balance, and transaction events through the transactional outbox pattern.
- Publish pending outbox events to RabbitMQ.

## Tech Stack

- Java 21
- Spring Boot 4
- Gradle
- MyBatis
- PostgreSQL
- Flyway
- RabbitMQ
- JUnit 5
- Testcontainers
- JaCoCo

## Requirements

- Java 21
- Docker

Docker is required for the local Compose environment and for integration tests that start PostgreSQL with Testcontainers.

## Run Locally

Start PostgreSQL, RabbitMQ, and the application:

```bash
docker compose up --build
```

Local URLs:

```text
API:              http://localhost:8080
Health:           http://localhost:8080/actuator/health
Liveness probe:   http://localhost:8080/actuator/health/liveness
RabbitMQ UI:      http://localhost:15672
```

RabbitMQ credentials:

```text
username: banking
password: banking
```

Stop containers:

```bash
docker compose down
```

Stop containers and remove PostgreSQL/RabbitMQ volumes:

```bash
docker compose down -v
```

## Build And Test

Run all tests:

```bash
./gradlew test
```

Run tests and enforce the JaCoCo coverage gate:

```bash
./gradlew test jacocoTestCoverageVerification
```

Build the application jar:

```bash
./gradlew bootJar
```

Coverage report:

```text
build/reports/jacoco/test/html/index.html
```

The configured minimum coverage is `80%`.

## API

All endpoints accept and return JSON.

### Create Account

```http
POST /api/v1/accounts
```

Request:

```json
{
  "customerId": "00000000-0000-0000-0000-000000000001",
  "country": "Estonia",
  "currencies": ["EUR", "USD"]
}
```

Response status:

```text
201 Created
```

Response:

```json
{
  "accountId": "11111111-1111-1111-1111-111111111111",
  "customerId": "00000000-0000-0000-0000-000000000001",
  "balances": [
    {
      "availableAmount": 0,
      "currency": "EUR"
    },
    {
      "availableAmount": 0,
      "currency": "USD"
    }
  ]
}
```

### Get Account

```http
GET /api/v1/accounts/{accountId}
```

Response:

```json
{
  "accountId": "11111111-1111-1111-1111-111111111111",
  "customerId": "00000000-0000-0000-0000-000000000001",
  "balances": [
    {
      "availableAmount": 100.00,
      "currency": "EUR"
    }
  ]
}
```

### Create Transaction

```http
POST /api/v1/accounts/{accountId}/transactions
```

Deposit request:

```json
{
  "amount": 100.00,
  "currency": "EUR",
  "direction": "IN",
  "description": "salary"
}
```

Withdrawal request:

```json
{
  "amount": 40.25,
  "currency": "EUR",
  "direction": "OUT",
  "description": "card payment"
}
```

Response status:

```text
201 Created
```

Response:

```json
{
  "accountId": "11111111-1111-1111-1111-111111111111",
  "transactionId": "22222222-2222-2222-2222-222222222222",
  "amount": 100.00,
  "currency": "EUR",
  "direction": "IN",
  "description": "salary",
  "balanceAfter": 100.00
}
```

### Get Transaction History

```http
GET /api/v1/accounts/{accountId}/transactions
```

Response:

```json
[
  {
    "accountId": "11111111-1111-1111-1111-111111111111",
    "transactionId": "22222222-2222-2222-2222-222222222222",
    "amount": 100.00,
    "currency": "EUR",
    "direction": "IN",
    "description": "salary"
  }
]
```

## Validation

Supported currencies:

```text
EUR, SEK, GBP, USD
```

Supported transaction directions:

```text
IN, OUT
```

Request rules:

- `customerId` must be a UUID.
- `country` is required and must be 2 to 45 characters.
- `currencies` is required and cannot be empty.
- `amount` must be greater than zero and can have up to 17 integer digits and 2 fractional digits.
- `description` is required and can be up to 255 characters.

Errors are returned with Spring `ProblemDetail`. Common messages:

- `Request validation failed`
- `Malformed JSON`
- `Invalid currency`
- `Invalid direction`
- `Invalid account`
- `Account not found`
- `Insufficient funds`

Validation errors include an `errors` object keyed by request field.

## Configuration

Default local values are defined in `src/main/resources/application.properties`.

| Variable | Default | Purpose |
| --- | --- | --- |
| `DB_URL` | `jdbc:postgresql://localhost:5432/banking` | PostgreSQL JDBC URL |
| `DB_USERNAME` | `banking` | PostgreSQL username |
| `DB_PASSWORD` | `banking` | PostgreSQL password |
| `RABBITMQ_HOST` | `localhost` | RabbitMQ host |
| `RABBITMQ_PORT` | `5672` | RabbitMQ AMQP port |
| `RABBITMQ_USERNAME` | `banking` | RabbitMQ username |
| `RABBITMQ_PASSWORD` | `banking` | RabbitMQ password |
| `APP_OUTBOX_BATCH_SIZE` | `200` | Number of outbox rows processed per poll |
| `APP_OUTBOX_POLL_INTERVAL` | `100` | Outbox polling delay in milliseconds |

Flyway migrations run from:

```text
classpath:db/migration
```

The Compose profile overrides several server, datasource, RabbitMQ, and outbox settings for local container execution.

## Outbox

Domain changes are stored in `outbox_messages` in the same transaction as the account, balance, or transaction update. A scheduled publisher reads pending messages and sends them to RabbitMQ exchange `banking.events`.

Event routing keys:

```text
account.created
balance.updated
transaction.created
```
