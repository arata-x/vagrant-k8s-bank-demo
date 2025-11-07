package dev.aratax.example.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class AmountValidator {
    
    private static final int MAX_DECIMAL_PLACES = 2;
    private static final BigDecimal MIN_AMOUNT = new BigDecimal("0.01");
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("999999999.99");
    
    private AmountValidator() {
        // Utility class, prevent instantiation
    }
    
    /**
     * Validates that an amount meets all business requirements
     * @param amount the amount to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValid(BigDecimal amount) {
        if (amount == null) {
            return false;
        }
        
        // Check if amount is positive
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        
        // Check minimum amount
        if (amount.compareTo(MIN_AMOUNT) < 0) {
            return false;
        }
        
        // Check maximum amount
        if (amount.compareTo(MAX_AMOUNT) > 0) {
            return false;
        }
        
        // Check decimal places
        if (amount.scale() > MAX_DECIMAL_PLACES) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Rounds an amount to the correct number of decimal places
     * @param amount the amount to round
     * @return the rounded amount
     */
    public static BigDecimal round(BigDecimal amount) {
        if (amount == null) {
            return null;
        }
        return amount.setScale(MAX_DECIMAL_PLACES, RoundingMode.HALF_UP);
    }
    
    /**
     * Validates that an amount is within the allowed range
     * @param amount the amount to check
     * @throws IllegalArgumentException if the amount is invalid
     */
    public static void validate(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        
        if (amount.compareTo(MIN_AMOUNT) < 0) {
            throw new IllegalArgumentException("Amount must be at least " + MIN_AMOUNT);
        }
        
        if (amount.compareTo(MAX_AMOUNT) > 0) {
            throw new IllegalArgumentException("Amount cannot exceed " + MAX_AMOUNT);
        }
        
        if (amount.scale() > MAX_DECIMAL_PLACES) {
            throw new IllegalArgumentException("Amount cannot have more than " + MAX_DECIMAL_PLACES + " decimal places");
        }
    }
    
    /**
     * Checks if two amounts are equal considering scale
     * @param amount1 first amount
     * @param amount2 second amount
     * @return true if amounts are equal
     */
    public static boolean areEqual(BigDecimal amount1, BigDecimal amount2) {
        if (amount1 == null && amount2 == null) {
            return true;
        }
        if (amount1 == null || amount2 == null) {
            return false;
        }
        return amount1.compareTo(amount2) == 0;
    }
}
