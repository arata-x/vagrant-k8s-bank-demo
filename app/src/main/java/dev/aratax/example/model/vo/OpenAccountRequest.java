package dev.aratax.example.model.vo;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpenAccountRequest {
    
    @NotBlank(message = "Owner name is required")
    @Size(min = 2, max = 100, message = "Owner name must be between 2 and 100 characters")
    @Pattern(regexp = "^[a-zA-Z\\s\\-']+$", message = "Owner name can only contain letters, spaces, hyphens, and apostrophes")
    private String ownerName;
    
    @NotBlank(message = "Currency is required")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a 3-letter ISO code (e.g., USD, EUR)")
    private String currency;
    
    @DecimalMin(value = "0.00", message = "Initial deposit cannot be negative")
    @DecimalMax(value = "999999999.99", message = "Initial deposit exceeds maximum allowed value")
    @Digits(integer = 9, fraction = 2, message = "Initial deposit must have at most 2 decimal places")
    private BigDecimal initialDeposit;
    
    // Optional fields
    private String email;
    private String phone;
    private String accountType; // e.g., CHECKING, SAVINGS
}
