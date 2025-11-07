package dev.aratax.example.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.aratax.example.enums.Direction;
import dev.aratax.example.enums.LockingMode;
import dev.aratax.example.enums.TransactionType;
import dev.aratax.example.exception.AccountNotFoundException;
import dev.aratax.example.exception.InsufficientFundsException;
import dev.aratax.example.model.po.Account;
import dev.aratax.example.model.po.LedgerEntry;
import dev.aratax.example.model.vo.TransactionRequest;
import dev.aratax.example.model.vo.TransactionResponse;
import dev.aratax.example.repository.AccountRepository;
import dev.aratax.example.repository.LedgerEntryRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccountServiceImpl Tests")
class AccountServiceImplTest {

    @Mock
    private AccountRepository accountRepo;

    @Mock
    private LedgerEntryRepository ledgerRepo;

    @InjectMocks
    private AccountServiceImpl accountService;

    private Account testAccount;
    private UUID accountId;
    private LedgerEntry testLedgerEntry;

    @BeforeEach
    void setUp() {
        accountId = UUID.randomUUID();
        testAccount = new Account();
        testAccount.setId(accountId);
        testAccount.setOwnerName("Test User");
        testAccount.setCurrency("USD");
        testAccount.setBalance(BigDecimal.valueOf(1000));
        testAccount.setVersion(1L);
        testAccount.setUpdatedAt(Instant.now());

        testLedgerEntry = new LedgerEntry();
        testLedgerEntry.setId(UUID.randomUUID());
        testLedgerEntry.setAccount(testAccount);
        testLedgerEntry.setAmount(BigDecimal.valueOf(100));
        testLedgerEntry.setDirection(Direction.CREDIT);
        testLedgerEntry.setReason("TEST_REASON");
        testLedgerEntry.setCreatedAt(Instant.now());
    }

    @Nested
    @DisplayName("Find Account Tests")
    class FindAccountTests {

        @Test
        @DisplayName("Should successfully find account by ID")
        void testFindAccount_Success() {
            // Given
            when(accountRepo.findById(accountId)).thenReturn(Optional.of(testAccount));

            // When
            Account result = accountService.find(accountId);

            // Then
            assertNotNull(result);
            assertEquals(accountId, result.getId());
            assertEquals("Test User", result.getOwnerName());
            assertEquals("USD", result.getCurrency());
            assertEquals(0, BigDecimal.valueOf(1000).compareTo(result.getBalance()));

            verify(accountRepo).findById(accountId);
        }

        @Test
        @DisplayName("Should throw AccountNotFoundException when account not found")
        void testFindAccount_NotFound() {
            // Given
            UUID nonExistentId = UUID.randomUUID();
            when(accountRepo.findById(nonExistentId)).thenReturn(Optional.empty());

            // When & Then
            assertThrows(AccountNotFoundException.class, () -> {
                accountService.find(nonExistentId);
            });

            verify(accountRepo).findById(nonExistentId);
        }
    }

    @Nested
    @DisplayName("Open Account Tests")
    class OpenAccountTests {

        @Test
        @DisplayName("Should successfully open account with seed amount")
        void testOpenAccount_WithSeed() {
            // Given
            String owner = "John Doe";
            String currency = "USD";
            BigDecimal seed = BigDecimal.valueOf(500);

            Account savedAccount = new Account();
            savedAccount.setId(UUID.randomUUID());
            savedAccount.setOwnerName(owner);
            savedAccount.setCurrency(currency);
            savedAccount.setBalance(seed);

            when(accountRepo.save(any(Account.class))).thenReturn(savedAccount);
            when(ledgerRepo.save(any(LedgerEntry.class))).thenReturn(testLedgerEntry);

            // When
            TransactionResponse result = accountService.open(owner, currency, seed);

            // Then
            assertNotNull(result);
            assertEquals(owner, result.getAccount().getOwnerName());
            assertEquals(currency, result.getAccount().getCurrency());
            assertEquals(0, seed.compareTo(result.getAccount().getBalance()));

            // Verify account was saved
            ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);
            verify(accountRepo).save(accountCaptor.capture());
            Account capturedAccount = accountCaptor.getValue();
            assertEquals(owner, capturedAccount.getOwnerName());
            assertEquals(currency, capturedAccount.getCurrency());

            // Verify ledger entry was created for seed
            ArgumentCaptor<LedgerEntry> ledgerCaptor = ArgumentCaptor.forClass(LedgerEntry.class);
            verify(ledgerRepo).save(ledgerCaptor.capture());
            LedgerEntry capturedEntry = ledgerCaptor.getValue();
            assertEquals(0, seed.compareTo(capturedEntry.getAmount()));
            assertEquals("OPEN_ACCOUNT_SEED", capturedEntry.getReason());
        }

        @Test
        @DisplayName("Should successfully open account without seed")
        void testOpenAccount_WithoutSeed() {
            // Given
            String owner = "Jane Doe";
            String currency = "EUR";

            Account savedAccount = new Account();
            savedAccount.setId(UUID.randomUUID());
            savedAccount.setOwnerName(owner);
            savedAccount.setCurrency(currency);
            savedAccount.setBalance(BigDecimal.ZERO);

            when(accountRepo.save(any(Account.class))).thenReturn(savedAccount);

            // When
            TransactionResponse result = accountService.open(owner, currency, null);

            // Then
            assertNotNull(result);
            assertEquals(owner, result.getAccount().getOwnerName());
            assertEquals(currency, result.getAccount().getCurrency());
            assertEquals(0, BigDecimal.ZERO.compareTo(result.getAccount().getBalance()));

            // Verify no ledger entry was created
            verify(ledgerRepo, never()).save(any(LedgerEntry.class));
        }

        @Test
        @DisplayName("Should successfully open account with zero seed")
        void testOpenAccount_WithZeroSeed() {
            // Given
            String owner = "Bob Smith";
            String currency = "GBP";
            BigDecimal seed = BigDecimal.ZERO;

            Account savedAccount = new Account();
            savedAccount.setId(UUID.randomUUID());
            savedAccount.setOwnerName(owner);
            savedAccount.setCurrency(currency);
            savedAccount.setBalance(BigDecimal.ZERO);

            when(accountRepo.save(any(Account.class))).thenReturn(savedAccount);

            // When
            TransactionResponse result = accountService.open(owner, currency, seed);

            // Then
            assertNotNull(result);
            assertEquals(0, BigDecimal.ZERO.compareTo(result.getAccount().getBalance()));

            // Verify no ledger entry was created for zero seed
            verify(ledgerRepo, never()).save(any(LedgerEntry.class));
        }

        @Test
        @DisplayName("Should handle large seed amounts")
        void testOpenAccount_WithLargeSeed() {
            // Given
            String owner = "Rich User";
            String currency = "USD";
            BigDecimal largeSeed = new BigDecimal("999999999.99");

            Account savedAccount = new Account();
            savedAccount.setId(UUID.randomUUID());
            savedAccount.setOwnerName(owner);
            savedAccount.setCurrency(currency);
            savedAccount.setBalance(largeSeed);

            when(accountRepo.save(any(Account.class))).thenReturn(savedAccount);
            when(ledgerRepo.save(any(LedgerEntry.class))).thenReturn(testLedgerEntry);

            // When
            TransactionResponse result = accountService.open(owner, currency, largeSeed);

            // Then
            assertNotNull(result);
            assertEquals(0, largeSeed.compareTo(result.getAccount().getBalance()));

            verify(accountRepo).save(any(Account.class));
            verify(ledgerRepo).save(any(LedgerEntry.class));
        }
    }

    @Nested
    @DisplayName("Execute Transaction Tests")
    class ExecuteTransactionTests {

        @Test
        @DisplayName("Should execute deposit transaction")
        void testExecuteTransaction_Deposit() {
            // Given
            TransactionRequest request = TransactionRequest.builder()
                .type(TransactionType.DEPOSIT)
                .amount(BigDecimal.valueOf(200))
                .lockingMode(LockingMode.OPTIMISTIC)
                .reason("DEPOSIT_TEST")
                .build();

            TransactionResponse expectedResponse = TransactionResponse.success(testAccount, testLedgerEntry);

            // Mock the actual service method that will be called
            AccountServiceImpl spyService = spy(accountService);
            doReturn(expectedResponse).when(spyService).deposit(
                eq(accountId), 
                eq(BigDecimal.valueOf(200)), 
                eq(LockingMode.OPTIMISTIC), 
                eq("DEPOSIT_TEST")
            );

            // When
            TransactionResponse result = spyService.executeTransaction(accountId, request);

            // Then
            assertNotNull(result);
            assertEquals("SUCCESS", result.getStatus());
            verify(spyService).deposit(accountId, BigDecimal.valueOf(200), LockingMode.OPTIMISTIC, "DEPOSIT_TEST");
        }

        @Test
        @DisplayName("Should execute withdrawal transaction")
        void testExecuteTransaction_Withdrawal() {
            // Given
            TransactionRequest request = TransactionRequest.builder()
                .type(TransactionType.WITHDRAWAL)
                .amount(BigDecimal.valueOf(150))
                .lockingMode(LockingMode.PESSIMISTIC)
                .reason("WITHDRAWAL_TEST")
                .build();

            TransactionResponse expectedResponse = TransactionResponse.success(testAccount, testLedgerEntry);

            // Mock the actual service method that will be called
            AccountServiceImpl spyService = spy(accountService);
            doReturn(expectedResponse).when(spyService).withdraw(
                eq(accountId), 
                eq(BigDecimal.valueOf(150)), 
                eq(LockingMode.PESSIMISTIC), 
                eq("WITHDRAWAL_TEST")
            );

            // When
            TransactionResponse result = spyService.executeTransaction(accountId, request);

            // Then
            assertNotNull(result);
            assertEquals("SUCCESS", result.getStatus());
            verify(spyService).withdraw(accountId, BigDecimal.valueOf(150), LockingMode.PESSIMISTIC, "WITHDRAWAL_TEST");
        }
    }

    @Nested
    @DisplayName("Deposit Tests")
    class DepositTests {

        @Test
        @DisplayName("Should deposit with pessimistic locking")
        void testDeposit_PessimisticLocking() {
            // Given
            BigDecimal depositAmount = BigDecimal.valueOf(200);
            when(accountRepo.findForUpdate(accountId)).thenReturn(Optional.of(testAccount));
            when(ledgerRepo.save(any(LedgerEntry.class))).thenReturn(testLedgerEntry);

            // Create a spy to test the actual method
            AccountServiceImpl spyService = spy(accountService);
            
            // When
            TransactionResponse result = spyService.doPessimisticOnce(accountId, TransactionType.DEPOSIT, depositAmount, "DEPOSIT_TEST");

            // Then
            assertNotNull(result);
            assertEquals("SUCCESS", result.getStatus());
            assertEquals(0, BigDecimal.valueOf(1200).compareTo(testAccount.getBalance()));

            // Verify pessimistic lock was used
            verify(accountRepo).findForUpdate(accountId);

            // Verify ledger entry was created
            ArgumentCaptor<LedgerEntry> ledgerCaptor = ArgumentCaptor.forClass(LedgerEntry.class);
            verify(ledgerRepo).save(ledgerCaptor.capture());
            LedgerEntry capturedEntry = ledgerCaptor.getValue();
            assertEquals(0, depositAmount.compareTo(capturedEntry.getAmount()));
            assertEquals("DEPOSIT_TEST", capturedEntry.getReason());
            assertEquals(Direction.CREDIT, capturedEntry.getDirection());
        }

        @Test
        @DisplayName("Should handle large deposit amounts")
        void testDeposit_LargeAmount() {
            // Given
            BigDecimal largeAmount = new BigDecimal("999999999.99");
            when(accountRepo.findById(accountId)).thenReturn(Optional.of(testAccount));
            when(ledgerRepo.save(any(LedgerEntry.class))).thenReturn(testLedgerEntry);

            AccountServiceImpl spyService = spy(accountService);

            // When
            TransactionResponse result = spyService.doOptimisticOnce(accountId, TransactionType.DEPOSIT, largeAmount, "LARGE_DEPOSIT");

            // Then
            assertNotNull(result);
            BigDecimal expectedBalance = BigDecimal.valueOf(1000).add(largeAmount);
            assertEquals(0, expectedBalance.compareTo(testAccount.getBalance()));
        }

        @Test
        @DisplayName("Should handle account not found for deposit")
        void testDeposit_AccountNotFound() {
            // Given
            UUID nonExistentId = UUID.randomUUID();
            when(accountRepo.findForUpdate(nonExistentId)).thenReturn(Optional.empty());

            AccountServiceImpl spyService = spy(accountService);

            // When & Then
            assertThrows(Exception.class, () -> {
                spyService.doPessimisticOnce(nonExistentId, TransactionType.DEPOSIT, BigDecimal.valueOf(100), "FAIL");
            });
        }
    }

    @Nested
    @DisplayName("Withdrawal Tests")
    class WithdrawalTests {

        @Test
        @DisplayName("Should withdraw with pessimistic locking")
        void testWithdraw_PessimisticLocking() {
            // Given
            BigDecimal withdrawAmount = BigDecimal.valueOf(400);
            when(accountRepo.findForUpdate(accountId)).thenReturn(Optional.of(testAccount));
            when(ledgerRepo.save(any(LedgerEntry.class))).thenReturn(testLedgerEntry);

            AccountServiceImpl spyService = spy(accountService);

            // When
            TransactionResponse result = spyService.doPessimisticOnce(accountId, TransactionType.WITHDRAWAL, withdrawAmount,"WITHDRAW_TEST");

            // Then
            assertNotNull(result);
            assertEquals("SUCCESS", result.getStatus());
            assertEquals(0, BigDecimal.valueOf(600).compareTo(testAccount.getBalance()));

            // Verify pessimistic lock was used
            verify(accountRepo).findForUpdate(accountId);

            // Verify ledger entry was created
            ArgumentCaptor<LedgerEntry> ledgerCaptor = ArgumentCaptor.forClass(LedgerEntry.class);
            verify(ledgerRepo).save(ledgerCaptor.capture());
            LedgerEntry capturedEntry = ledgerCaptor.getValue();
            assertEquals(0, withdrawAmount.compareTo(capturedEntry.getAmount()));
            assertEquals("WITHDRAW_TEST", capturedEntry.getReason());
            assertEquals(Direction.DEBIT, capturedEntry.getDirection());
        }

        @Test
        @DisplayName("Should throw exception for insufficient funds")
        void testWithdraw_InsufficientFunds() {
            // Given
            BigDecimal withdrawAmount = BigDecimal.valueOf(1500); // More than balance
            when(accountRepo.findForUpdate(accountId)).thenReturn(Optional.of(testAccount));

            AccountServiceImpl spyService = spy(accountService);

            // When & Then
            assertThrows(InsufficientFundsException.class, () -> {
                spyService.doPessimisticOnce(accountId,TransactionType.WITHDRAWAL, withdrawAmount, "WITHDRAW_FAIL");
            });

            // Verify balance unchanged
            assertEquals(0, BigDecimal.valueOf(1000).compareTo(testAccount.getBalance()));

            // Verify no ledger entry was created
            verify(ledgerRepo, never()).save(any(LedgerEntry.class));
        }

        @Test
        @DisplayName("Should handle account not found for withdrawal")
        void testWithdraw_AccountNotFound() {
            // Given
            UUID nonExistentId = UUID.randomUUID();
            when(accountRepo.findById(nonExistentId)).thenReturn(Optional.empty());

            AccountServiceImpl spyService = spy(accountService);

            // When & Then
            assertThrows(Exception.class, () -> {
                spyService.doOptimisticOnce(nonExistentId, TransactionType.WITHDRAWAL, BigDecimal.valueOf(100), "FAIL");
            });
        }
    }

    @Nested
    @DisplayName("Edge Cases and Validation Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle negative amount in deposit")
        void testDeposit_NegativeAmount() {
            // Given
            BigDecimal negativeAmount = BigDecimal.valueOf(-100);
            when(accountRepo.findById(accountId)).thenReturn(Optional.of(testAccount));
            when(ledgerRepo.save(any(LedgerEntry.class))).thenReturn(testLedgerEntry);

            AccountServiceImpl spyService = spy(accountService);

            // When - The service doesn't validate negative amounts, it just adds them
            TransactionResponse result = spyService.doOptimisticOnce(accountId, TransactionType.DEPOSIT, negativeAmount, "NEGATIVE_DEPOSIT");

            // Then - Balance should decrease (credit with negative amount)
            assertNotNull(result);
            assertEquals(0, BigDecimal.valueOf(900).compareTo(testAccount.getBalance()));
        }

        @Test
        @DisplayName("Should handle zero amount transaction")
        void testTransaction_ZeroAmount() {
            // Given
            BigDecimal zeroAmount = BigDecimal.ZERO;
            when(accountRepo.findById(accountId)).thenReturn(Optional.of(testAccount));
            when(ledgerRepo.save(any(LedgerEntry.class))).thenReturn(testLedgerEntry);

            AccountServiceImpl spyService = spy(accountService);

            // When
            TransactionResponse result = spyService.doOptimisticOnce(accountId, TransactionType.DEPOSIT, zeroAmount, "ZERO_DEPOSIT");

            // Then - Balance should remain unchanged
            assertNotNull(result);
            assertEquals(0, BigDecimal.valueOf(1000).compareTo(testAccount.getBalance()));

            // But ledger entry should still be created
            verify(ledgerRepo).save(any(LedgerEntry.class));
        }

        @Test
        @DisplayName("Should handle decimal amounts correctly")
        void testTransaction_DecimalAmount() {
            // Given
            BigDecimal decimalAmount = new BigDecimal("123.45");
            when(accountRepo.findById(accountId)).thenReturn(Optional.of(testAccount));
            when(ledgerRepo.save(any(LedgerEntry.class))).thenReturn(testLedgerEntry);

            AccountServiceImpl spyService = spy(accountService);

            // When
            TransactionResponse result = spyService.doOptimisticOnce(accountId, TransactionType.DEPOSIT, decimalAmount, "DECIMAL_DEPOSIT");

            // Then
            assertNotNull(result);
            BigDecimal expectedBalance = new BigDecimal("1123.45");
            assertEquals(0, expectedBalance.compareTo(testAccount.getBalance()));
        }

        @Test
        @DisplayName("Should handle very small amounts")
        void testTransaction_VerySmallAmount() {
            // Given
            BigDecimal smallAmount = new BigDecimal("0.01");
            when(accountRepo.findById(accountId)).thenReturn(Optional.of(testAccount));
            when(ledgerRepo.save(any(LedgerEntry.class))).thenReturn(testLedgerEntry);

            AccountServiceImpl spyService = spy(accountService);

            // When
            TransactionResponse result = spyService.doOptimisticOnce(accountId,TransactionType.DEPOSIT, smallAmount, "SMALL_DEPOSIT");

            // Then
            assertNotNull(result);
            BigDecimal expectedBalance = new BigDecimal("1000.01");
            assertEquals(0, expectedBalance.compareTo(testAccount.getBalance()));
        }

        @Test
        @DisplayName("Should handle null reason in transaction")
        void testTransaction_NullReason() {
            // Given
            BigDecimal amount = BigDecimal.valueOf(100);
            when(accountRepo.findById(accountId)).thenReturn(Optional.of(testAccount));
            when(ledgerRepo.save(any(LedgerEntry.class))).thenReturn(testLedgerEntry);

            AccountServiceImpl spyService = spy(accountService);

            // When
            TransactionResponse result = spyService.doOptimisticOnce(accountId, TransactionType.DEPOSIT, amount, null);

            // Then
            assertNotNull(result);
            assertEquals(0, BigDecimal.valueOf(1100).compareTo(testAccount.getBalance()));

            // Verify ledger entry was created with null reason
            ArgumentCaptor<LedgerEntry> ledgerCaptor = ArgumentCaptor.forClass(LedgerEntry.class);
            verify(ledgerRepo).save(ledgerCaptor.capture());
            LedgerEntry capturedEntry = ledgerCaptor.getValue();
            assertNull(capturedEntry.getReason());
        }
    }

    @Nested
    @DisplayName("Transaction Response Tests")
    class TransactionResponseTests {

        @Test
        @DisplayName("Should return correct response for successful deposit")
        void testTransactionResponse_SuccessfulDeposit() {
            // Given
            BigDecimal amount = BigDecimal.valueOf(200);
            when(accountRepo.findById(accountId)).thenReturn(Optional.of(testAccount));
            when(ledgerRepo.save(any(LedgerEntry.class))).thenReturn(testLedgerEntry);

            AccountServiceImpl spyService = spy(accountService);

            // When
            TransactionResponse result = spyService.doOptimisticOnce(accountId, TransactionType.DEPOSIT, amount, "DEPOSIT");

            // Then
            assertNotNull(result);
            assertEquals("SUCCESS", result.getStatus());
            assertNotNull(result.getTimestamp());
            assertNotNull(result.getAccount());
            assertEquals(accountId, result.getAccount().getId());
            assertNotNull(result.getLedgerEntry());
            assertNotNull(result.getTransactionId());
        }

        @Test
        @DisplayName("Should return correct response for successful withdrawal")
        void testTransactionResponse_SuccessfulWithdrawal() {
            // Given
            BigDecimal amount = BigDecimal.valueOf(300);
            testLedgerEntry.setDirection(Direction.DEBIT);
            when(accountRepo.findById(accountId)).thenReturn(Optional.of(testAccount));
            when(ledgerRepo.save(any(LedgerEntry.class))).thenReturn(testLedgerEntry);

            AccountServiceImpl spyService = spy(accountService);

            // When
            TransactionResponse result = spyService.doOptimisticOnce(accountId, TransactionType.WITHDRAWAL, amount, "WITHDRAWAL");

            // Then
            assertNotNull(result);
            assertEquals("SUCCESS", result.getStatus());
            assertNotNull(result.getAccount());
            assertEquals(0, BigDecimal.valueOf(700).compareTo(result.getAccount().getBalance()));
            assertEquals("DEBIT", result.getLedgerEntry().getDirection());
        }
    }

    @Nested
    @DisplayName("Concurrent Transaction Tests")
    class ConcurrentTransactionTests {

        @Test
        @DisplayName("Should handle concurrent deposits correctly")
        void testConcurrentDeposits() {
            // This would typically be tested in integration tests
            // Unit tests can verify the locking mechanism is called
            
            // Given
            BigDecimal amount = BigDecimal.valueOf(100);
            when(accountRepo.findForUpdate(accountId)).thenReturn(Optional.of(testAccount));
            when(ledgerRepo.save(any(LedgerEntry.class))).thenReturn(testLedgerEntry);

            AccountServiceImpl spyService = spy(accountService);

            // When
            TransactionResponse result = spyService.doPessimisticOnce(accountId, TransactionType.DEPOSIT, amount, "CONCURRENT_TEST");

            // Then
            assertNotNull(result);
            // Verify pessimistic lock was acquired
            verify(accountRepo).findForUpdate(accountId);
        }

        @Test
        @DisplayName("Should handle concurrent withdrawals correctly")
        void testConcurrentWithdrawals() {
            // Given
            BigDecimal amount = BigDecimal.valueOf(200);
            when(accountRepo.findForUpdate(accountId)).thenReturn(Optional.of(testAccount));
            when(ledgerRepo.save(any(LedgerEntry.class))).thenReturn(testLedgerEntry);

            AccountServiceImpl spyService = spy(accountService);

            // When
            TransactionResponse result = spyService.doPessimisticOnce(accountId, TransactionType.WITHDRAWAL, amount, "CONCURRENT_WITHDRAW");

            // Then
            assertNotNull(result);
            verify(accountRepo).findForUpdate(accountId);
        }
    }
}
