package ua.stepan.zhuk;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.NoOpResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ua.stepan.zhuk.account.AccountResponse;
import ua.stepan.zhuk.account.CreateAccountRequest;
import ua.stepan.zhuk.account.enums.Currency;
import ua.stepan.zhuk.account.transaction.CreateTransactionRequest;
import ua.stepan.zhuk.account.transaction.enums.TransactionDirection;
import ua.stepan.zhuk.outbox.OutboxPublisher;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.rabbitmq.listener.simple.auto-startup=false"
        }
)
public abstract class AbstractIntegrationTest {

    @Container
    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("banking")
            .withUsername("banking")
            .withPassword("banking");

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @LocalServerPort
    private int port;

    @MockitoBean
    private OutboxPublisher outboxPublisher;

    protected RestTemplate restTemplate;

    @DynamicPropertySource
    static void registerDataSourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @BeforeEach
    void cleanDatabase() {
        restTemplate = new RestTemplate();
        restTemplate.setErrorHandler(new NoOpResponseErrorHandler());
        jdbcTemplate.update("DELETE FROM transactions");
        jdbcTemplate.update("DELETE FROM balances");
        jdbcTemplate.update("DELETE FROM outbox_messages");
        jdbcTemplate.update("DELETE FROM accounts");
    }

    protected UUID createAccount(UUID customerId, String country, Set<Currency> currencies) {
        HttpEntity<CreateAccountRequest> request = createAccountRequest(customerId, country, currencies);

        ResponseEntity<AccountResponse> response = restTemplate.postForEntity(
                url("/api/v1/accounts"),
                request,
                AccountResponse.class
        );

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        AccountResponse body = response.getBody();
        assertThat(body).isNotNull();

        return body.accountId();
    }

    protected static HttpEntity<CreateAccountRequest> createAccountRequest(
            UUID customerId,
            String country,
            Set<Currency> currencies
    ) {
        return new HttpEntity<>(new CreateAccountRequest(customerId, country, currencies), jsonHeaders());
    }

    protected static HttpEntity<InvalidCreateAccountPayload> invalidCreateAccountRequest(
            UUID customerId,
            String country,
            List<String> currencies
    ) {
        return new HttpEntity<>(new InvalidCreateAccountPayload(customerId, country, currencies), jsonHeaders());
    }

    protected static HttpEntity<CreateTransactionRequest> transactionRequest(
            BigDecimal amount,
            Currency currency,
            TransactionDirection direction,
            String description
    ) {
        final CreateTransactionRequest request = new CreateTransactionRequest(amount, currency, direction, description);
        return new HttpEntity<>(request, jsonHeaders());
    }

    protected String url(String path) {
        return "http://localhost:" + port + path;
    }

    protected static HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        return headers;
    }

    protected record InvalidCreateAccountPayload(UUID customerId, String country, List<String> currencies) {
    }
}
