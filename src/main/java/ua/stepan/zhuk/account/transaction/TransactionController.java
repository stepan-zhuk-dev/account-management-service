package ua.stepan.zhuk.account.transaction;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/accounts/{accountId}/transactions")
@RequiredArgsConstructor
public class TransactionController {
    private final TransactionService transactionService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TransactionResponse> create(@PathVariable UUID accountId, @Valid @RequestBody CreateTransactionRequest request) {
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest()
                        .buildAndExpand(accountId)
                        .toUri())
                .body(TransactionResponse.toResponse(accountId, transactionService.create(accountId, request)));
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public List<TransactionHistoryResponse> findHistoryByAccountId(@PathVariable UUID accountId) {
        return transactionService.findByAccountId(accountId)
                .stream()
                .map(transaction -> TransactionHistoryResponse.toResponse(accountId, transaction))
                .toList();
    }
}
