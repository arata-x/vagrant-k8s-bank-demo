package dev.aratax.example.model.vo;

import java.math.BigDecimal;

import dev.aratax.example.enums.LockingMode;
import dev.aratax.example.enums.TransactionType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionRequest {
    
    @NotNull(message = "Transaction type is required")
    private TransactionType type;
    
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @DecimalMax(value = "999999999.99", message = "Amount exceeds maximum allowed value")
    @Digits(integer = 9, fraction = 2, message = "Amount must have at most 2 decimal places")
    private BigDecimal amount;
    
    @NotNull(message = "Locking mode is required")
    private LockingMode lockingMode;
    
    @Pattern(regexp = "^.{3,50}$", message = "Reason must be 3-50 letters")
    private String reason;
    
}
