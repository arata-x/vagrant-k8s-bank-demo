package dev.aratax.example.service;

import java.math.BigDecimal;
import java.util.UUID;

import dev.aratax.example.enums.LockingMode;
import dev.aratax.example.model.po.Account;
import dev.aratax.example.model.vo.TransactionRequest;
import dev.aratax.example.model.vo.TransactionResponse;

public interface AccountService {

    Account find(UUID id);
    TransactionResponse open(String owner, String currency, BigDecimal seed);
    TransactionResponse deposit(UUID accountId, BigDecimal amt, LockingMode mode, String reason);
    TransactionResponse withdraw(UUID accountId, BigDecimal amt, LockingMode mode, String reason);
    TransactionResponse executeTransaction(UUID id, TransactionRequest request);

}
