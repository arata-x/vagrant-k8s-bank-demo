package dev.aratax.example.service;

import java.math.BigDecimal;
import java.util.UUID;

import dev.aratax.example.enums.TransactionType;
import dev.aratax.example.model.vo.TransactionResponse;

public interface AccountTransaction {

    TransactionResponse execute(UUID id, TransactionType type, BigDecimal amt, String reason);
    
}
