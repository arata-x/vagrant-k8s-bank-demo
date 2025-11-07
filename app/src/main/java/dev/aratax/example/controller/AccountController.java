package dev.aratax.example.controller;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.aratax.example.model.po.Account;
import dev.aratax.example.model.vo.ApiResponse;
import dev.aratax.example.model.vo.OpenAccountRequest;
import dev.aratax.example.model.vo.TransactionRequest;
import dev.aratax.example.model.vo.TransactionResponse;
import dev.aratax.example.service.AccountService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    /**
     * Open a new account
     */
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TransactionResponse> openAccount(
            @Valid @RequestBody OpenAccountRequest request) {
        
        TransactionResponse response = accountService.open(
                request.getOwnerName(),
                request.getCurrency(),
                request.getInitialDeposit() != null ? request.getInitialDeposit() : BigDecimal.ZERO
        );
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get account details
     */
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Account>> getAccount(@PathVariable UUID id) {
        
        Account account = accountService.find(id);
        return ResponseEntity.ok()
            .body(new ApiResponse<>(HttpStatus.OK.value(), "Account retrieved successfully", account));
    }

   /**
     * Convenience endpoint that wraps the transaction endpoint
     */
    @PostMapping(value = "/{id}/transaction", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TransactionResponse> transaction(
            @PathVariable UUID id,
            @Valid @RequestBody TransactionRequest request) {

        TransactionResponse response = accountService.executeTransaction(id, request );
        return ResponseEntity.ok(response);
    }

}
