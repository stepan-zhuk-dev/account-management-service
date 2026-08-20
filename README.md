# Core Banking Service

Core Banking Service is a Spring Boot API for account creation, multi-currency balances, deposits, withdrawals, transaction history, and outbox event persistence.

## Features

- Create accounts for customers.
- Keep one balance per supported currency.
- Add money with `IN` transactions.
- Withdraw money with `OUT` transactions.
- Reject withdrawals when the selected currency balance is too low.
- Return transaction history by account.
- Persist outbox messages for account, balance, and transaction changes.

## Tech Stack

- Java 21
- Spring Boot 4
- MyBatis
- PostgreSQL
- Flyway
- RabbitMQ
- Gradle
- JUnit 5
- Testcontainers
- JaCoCo

## Requirements

- Java 21
- Docker

Docker is used for PostgreSQL, RabbitMQ, the local Compose environment, and integration tests.

## Run Locally

Start PostgreSQL, RabbitMQ, and the service:

```bash
docker compose up --build
```

Service URLs:

```text
API:       http://localhost:8080
Health:    http://localhost:8080/actuator/health
RabbitMQ:  http://localhost:15672
```

RabbitMQ local credentials:

```text
username: banking
password: banking
```

Stop the environment:

```bash
docker compose down
```

Remove local PostgreSQL and RabbitMQ volumes too:

```bash
docker compose down -v
```

## Run Tests

Run all tests:

```bash
./gradlew test
```

Run tests with the coverage gate:

```bash
./gradlew test jacocoTestCoverageVerification
```

The coverage report is generated at:

```text
build/reports/jacoco/test/html/index.html
```

The configured minimum coverage is `80%`.

## API

All examples use JSON request and response bodies.

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

Important request rules:

- `customerId` must be a UUID.
- `country` is required and must be 2 to 45 characters.
- `currencies` is required and cannot be empty.
- `amount` must be greater than zero and can have up to 17 integer digits and 2 fractional digits.
- `description` is required and can be up to 255 characters.

Errors are returned as `application/problem+json` using Spring `ProblemDetail`. Common messages include:

- `Invalid currency`
- `Invalid direction`
- `Invalid amount`
- `Description missing`
- `Request validation failed`
- `Insufficient {currency} funds: available {available}, requested {requested}`
- `Account {id} was not found`

## Configuration

Default local values are defined in `src/main/resources/application.properties`.

| Variable | Default |
| --- | --- |
| `DB_URL` | `jdbc:postgresql://localhost:5432/banking` |
| `DB_USERNAME` | `banking` |
| `DB_PASSWORD` | `banking` |
| `RABBITMQ_HOST` | `localhost` |
| `RABBITMQ_PORT` | `5672` |
| `RABBITMQ_USERNAME` | `banking` |
| `RABBITMQ_PASSWORD` | `banking` |

Flyway runs migrations from:

```text
classpath:db/migration
```
