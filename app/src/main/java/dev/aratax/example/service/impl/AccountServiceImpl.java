package dev.aratax.example.service.impl;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import dev.aratax.example.enums.LockingMode;
import dev.aratax.example.enums.TransactionType;
import dev.aratax.example.exception.AccountNotFoundException;
import dev.aratax.example.model.po.Account;
import dev.aratax.example.model.po.LedgerEntry;
import dev.aratax.example.model.vo.TransactionRequest;
import dev.aratax.example.model.vo.TransactionResponse;
import dev.aratax.example.repository.AccountRepository;
import dev.aratax.example.repository.LedgerEntryRepository;
import dev.aratax.example.service.AccountService;
import dev.aratax.example.service.AccountTransaction;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class AccountServiceImpl implements AccountService {
  
  private static final int MAX_RETRIES = 5;    
  
  private final AccountRepository accountRepo;
  private final LedgerEntryRepository ledgerRepo;
  @Resource(name= OptimsticAccountTransactionImpl.BEAN_ID)
  private AccountTransaction optimsticAccountTransaction;
  @Resource(name= PessimisticAccountTransactionImpl.BEAN_ID)
  private AccountTransaction pessimisticAccountTransaction;

  public AccountServiceImpl(AccountRepository accountRepo, LedgerEntryRepository ledgerRepo) {
    this.accountRepo =  accountRepo;
    this.ledgerRepo = ledgerRepo;
  }

  @Override
  @Transactional(isolation = Isolation.READ_COMMITTED)
  public Account find(UUID id) {
    log.debug("Finding account with id: {}", id);
    return accountRepo.findById(id)
          .orElseThrow(() -> new AccountNotFoundException(id));
  }

  @Override
  @Transactional
  public TransactionResponse open(String owner, String currency, BigDecimal seed) {
    var account = new Account();
    account.setOwnerName(owner);
    account.setCurrency(currency);
    account.deposit(seed == null ? BigDecimal.ZERO : seed);
    var saved = accountRepo.save(account);
    LedgerEntry ledgerEntry = null;
    if (seed != null && seed.signum() > 0) {
      ledgerEntry = ledgerRepo.save(LedgerEntry.of(account, TransactionType.DEPOSIT, seed, "OPEN_ACCOUNT_SEED"));
    }
    return TransactionResponse.success(saved, ledgerEntry);
  }

  @Override
  public TransactionResponse executeTransaction(UUID id, TransactionRequest request) {
    
    if(TransactionType.DEPOSIT.equals(request.getType())){
      return deposit(id, request.getAmount(), request.getLockingMode(), request.getReason());
    } else {
      return withdraw(id, request.getAmount(), request.getLockingMode(), request.getReason());
    }

  }
  
  /** Deposit using specified locking mode */
  @Override
  public TransactionResponse deposit(UUID accountId, BigDecimal amt, LockingMode mode, String reason) {
    return executeWithLock(accountId, TransactionType.DEPOSIT, amt, mode, reason);
  }

  /** Withdraw using specified locking mode */
  @Override
  public TransactionResponse withdraw(UUID accountId, BigDecimal amt, LockingMode mode, String reason) {
    return executeWithLock(accountId, TransactionType.WITHDRAWAL, amt, mode, reason);
  }

  private TransactionResponse executeWithLock(UUID id, TransactionType type, BigDecimal amt,
                               LockingMode mode, String reason) {
    return LockingMode.PESSIMISTIC.equals(mode) ? depositOrWithdrawPessimistic(id, type, amt, reason) : 
              depositOrWithdrawOptimistic(id, type, amt, reason);

  }

  protected TransactionResponse depositOrWithdrawPessimistic(UUID id, TransactionType type, BigDecimal amt, String reason) {
   return optimsticAccountTransaction.execute(id, type, amt, reason);
  }

  protected TransactionResponse depositOrWithdrawOptimistic(UUID id,TransactionType type, BigDecimal amt, String reason) {
    int attempt = 0;
    while (true) {
      try {
        return optimsticAccountTransaction.execute(id, type, amt, reason);
      } catch (OptimisticLockingFailureException ex) {
        log.warn("OptimistiLockingException-uuid:{}, type:{}, amt:{}, reason:{}", id, type, amt, reason);
        if (++attempt > MAX_RETRIES) 
          throw ex;
        sleepJitter(attempt);
      }
    }
  }


  private void sleepJitter(int attempt) {
    try {
      long backoffMs = Math.min(100L * attempt, 1200L);
      Thread.sleep(backoffMs + ThreadLocalRandom.current().nextLong(60));
    } catch (InterruptedException ignored) { 
      Thread.currentThread().interrupt(); 
    }
  }

}
