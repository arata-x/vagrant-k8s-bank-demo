package dev.aratax.example.exception;

import java.util.UUID;

public class AccountNotFoundException extends RuntimeException {
    
    private final UUID accountId;
    
    public AccountNotFoundException(UUID accountId) {
        super(String.format("Account not found with id: %s", accountId));
        this.accountId = accountId;
    }
    
    public UUID getAccountId() {
        return accountId;
    }
}
