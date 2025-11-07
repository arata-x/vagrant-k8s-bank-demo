package dev.aratax.example.enums;

public enum Direction { 
    DEBIT, CREDIT;

    public static Direction fromTransactionType(TransactionType type){
        if (type == TransactionType.DEPOSIT) {
            return CREDIT;
        } else if (type == TransactionType.WITHDRAWAL) {
            return DEBIT;
        }
        throw new IllegalArgumentException("Unknown transaction type: " + type);
    }
}
