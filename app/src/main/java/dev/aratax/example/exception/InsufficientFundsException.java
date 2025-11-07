package dev.aratax.example.exception;

import java.math.BigDecimal;
import java.util.UUID;

public class InsufficientFundsException extends RuntimeException {
    
    private final UUID accountId;
    private final BigDecimal requestedAmount;
    private final BigDecimal availableBalance;
    
    public InsufficientFundsException(UUID accountId, BigDecimal requestedAmount, BigDecimal availableBalance) {
        super(String.format("Insufficient funds in account %s. Requested: %s, Available: %s", 
            accountId, requestedAmount, availableBalance));
        this.accountId = accountId;
        this.requestedAmount = requestedAmount;
        this.availableBalance = availableBalance;
    }
    
    public UUID getAccountId() {
        return accountId;
    }
    
    public BigDecimal getRequestedAmount() {
        return requestedAmount;
    }
    
    public BigDecimal getAvailableBalance() {
        return availableBalance;
    }
}
