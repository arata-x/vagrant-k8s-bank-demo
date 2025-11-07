package dev.aratax.example.service.impl;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import dev.aratax.example.annotation.InjectJitter;
import dev.aratax.example.enums.TransactionType;
import dev.aratax.example.model.po.LedgerEntry;
import dev.aratax.example.model.vo.TransactionResponse;
import dev.aratax.example.repository.AccountRepository;
import dev.aratax.example.repository.LedgerEntryRepository;
import dev.aratax.example.service.AccountTransaction;
import jakarta.annotation.Resource;

@Service(OptimsticAccountTransactionImpl.BEAN_ID)
public class OptimsticAccountTransactionImpl implements AccountTransaction {

    public static final String BEAN_ID = "optimisticAccountTransactionImpl";

    @Resource
    private AccountRepository accountRepo;
    @Resource
    private LedgerEntryRepository ledgerRepo;

    @InjectJitter
    @Transactional(isolation= Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    @Override
    public TransactionResponse execute(UUID id, TransactionType type, BigDecimal amt, String reason) {
        var account = accountRepo.findById(id).orElseThrow();
        if (TransactionType.DEPOSIT.equals(type)) 
        account.deposit(amt); 
        else 
        account.withdraw(amt);
        var ledgerEntry = ledgerRepo.save(LedgerEntry.of(account, type, amt, reason));
        return TransactionResponse.success(account, ledgerEntry);
    }
    
}
