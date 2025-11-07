package dev.aratax.example.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import dev.aratax.example.exception.InsufficientFundsException;
import dev.aratax.example.model.po.Account;
import jakarta.annotation.Resource;

@SpringBootTest
@Testcontainers
@Transactional
class AccountRepositoryTest {

    @Container
    static final PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:18")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test")
            .withInitScript("init.sql"); 

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Resource
    private AccountRepository accountRepository;

    @BeforeEach
    void setUp() {
        accountRepository.deleteAll();
    }

    @Test
    void testSaveAndFindAccount() {
        // Create an account
        Account account = new Account();
        account.setOwnerName("John Doe");
        account.setBalance(BigDecimal.valueOf(1000));
        account.setCurrency("USD");
        account.setUpdatedAt(Instant.now());
        
        // Save the account
        Account savedAccount = accountRepository.save(account);
        
        // Verify it was saved
        assertNotNull(savedAccount.getId());
        assertEquals(1, accountRepository.count());
        assertEquals(0L, savedAccount.getVersion());
        
        // Find by ID
        Account foundAccount = accountRepository.findById(savedAccount.getId()).orElse(null);
        assertNotNull(foundAccount);
        assertEquals("John Doe", foundAccount.getOwnerName());
        assertEquals(0, BigDecimal.valueOf(1000).compareTo(foundAccount.getBalance()));
        assertEquals("USD", foundAccount.getCurrency());
    }

    @Test
    void testMultipleAccounts() {
        // Create multiple accounts
        Account account1 = new Account();
        account1.setOwnerName("Alice Smith");
        account1.setBalance(BigDecimal.valueOf(5000));
        account1.setCurrency("USD");
        account1.setUpdatedAt(Instant.now());
        
        Account account2 = new Account();
        account2.setOwnerName("Bob Johnson");
        account2.setBalance(BigDecimal.valueOf(3000));
        account2.setCurrency("EUR");
        account2.setUpdatedAt(Instant.now());
        
        Account account3 = new Account();
        account3.setOwnerName("Charlie Brown");
        account3.setBalance(BigDecimal.valueOf(7500));
        account3.setCurrency("GBP");
        account3.setUpdatedAt(Instant.now());
        
        // Save all accounts
        accountRepository.save(account1);
        accountRepository.save(account2);
        accountRepository.save(account3);
        
        // Verify all were saved
        assertEquals(3, accountRepository.count());
        
        // Find all accounts
        List<Account> allAccounts = accountRepository.findAll();
        assertEquals(3, allAccounts.size());
        
        // Verify each account exists
        assertTrue(allAccounts.stream().anyMatch(a -> "Alice Smith".equals(a.getOwnerName())));
        assertTrue(allAccounts.stream().anyMatch(a -> "Bob Johnson".equals(a.getOwnerName())));
        assertTrue(allAccounts.stream().anyMatch(a -> "Charlie Brown".equals(a.getOwnerName())));
    }

    @Test
    void testUpdateAccount() {
        // Create and save an account
        Account account = new Account();
        account.setOwnerName("Test User");
        account.setBalance(BigDecimal.valueOf(1000));
        account.setCurrency("USD");
        account.setUpdatedAt(Instant.now());
        Account savedAccount = accountRepository.save(account);
        
        // Update the account
        savedAccount.setBalance(BigDecimal.valueOf(1500));
        savedAccount.setUpdatedAt(Instant.now());
        Account updatedAccount = accountRepository.saveAndFlush(savedAccount);
        
        // Verify the update
        assertEquals(savedAccount.getId(), updatedAccount.getId());
        assertEquals(0, BigDecimal.valueOf(1500).compareTo(updatedAccount.getBalance()));
        assertEquals(1L, updatedAccount.getVersion()); // Version should increment
    }

    @Test
    void testDeleteAccount() {
        // Create and save an account
        Account account = new Account();
        account.setOwnerName("To Be Deleted");
        account.setBalance(BigDecimal.valueOf(500));
        account.setCurrency("USD");
        account.setUpdatedAt(Instant.now());
        Account savedAccount = accountRepository.save(account);
        
        // Verify it exists
        assertEquals(1, accountRepository.count());
        
        // Delete the account
        accountRepository.deleteById(savedAccount.getId());
        
        // Verify it was deleted
        assertEquals(0, accountRepository.count());
        assertFalse(accountRepository.findById(savedAccount.getId()).isPresent());
    }

    @Test
    void testCreditMethod() {
        // Create an account with initial balance
        Account account = new Account();
        account.setOwnerName("Credit Test");
        account.setBalance(BigDecimal.valueOf(1000));
        account.setCurrency("USD");
        account.setUpdatedAt(Instant.now());
        
        // Test credit method
        account.deposit(BigDecimal.valueOf(500));
        
        // Verify balance increased
        assertEquals(0, BigDecimal.valueOf(1500).compareTo(account.getBalance()));
        
        // Save and verify persistence
        Account savedAccount = accountRepository.save(account);
        assertEquals(0, BigDecimal.valueOf(1500).compareTo(savedAccount.getBalance()));
    }

    @Test
    void testDebitMethod() {
        // Create an account with initial balance
        Account account = new Account();
        account.setOwnerName("Debit Test");
        account.setBalance(BigDecimal.valueOf(1000));
        account.setCurrency("USD");
        account.setUpdatedAt(Instant.now());
        
        // Test debit method
        account.withdraw(BigDecimal.valueOf(300));
        
        // Verify balance decreased
        assertEquals(0, BigDecimal.valueOf(700).compareTo(account.getBalance()));
        
        // Save and verify persistence
        Account savedAccount = accountRepository.save(account);
        assertEquals(0, BigDecimal.valueOf(700).compareTo(savedAccount.getBalance()));
    }

    @Test
    void testDebitInsufficientFunds() {
        // Create an account with low balance
        Account account = new Account();
        account.setOwnerName("Insufficient Funds Test");
        account.setBalance(BigDecimal.valueOf(100));
        account.setCurrency("USD");
        account.setUpdatedAt(Instant.now());
        // Try to debit more than available
        assertThrows(InsufficientFundsException.class, () -> {
            account.withdraw(BigDecimal.valueOf(200));
        });
        
        // Verify balance unchanged
        assertEquals(0, BigDecimal.valueOf(100).compareTo(account.getBalance()));
    }

    @Test
    void testAccountWithZeroBalance() {
        // Create an account with zero balance
        Account account = new Account();
        account.setOwnerName("Zero Balance");
        account.setCurrency("USD");
        // Balance defaults to ZERO
        account.setUpdatedAt(Instant.now());
        
        // Save and verify
        Account savedAccount = accountRepository.save(account);
        
        assertNotNull(savedAccount.getId());
        assertEquals(0, BigDecimal.ZERO.compareTo(savedAccount.getBalance()));
    }

    @Test
    void testAccountWithDifferentCurrencies() {
        // Create accounts with different currencies
        String[] currencies = {"USD", "EUR", "GBP", "JPY", "CHF"};
        
        for (String currency : currencies) {
            Account account = new Account();
            account.setOwnerName("Currency Test - " + currency);
            account.setBalance(BigDecimal.valueOf(1000));
            account.setCurrency(currency);
            account.setUpdatedAt(Instant.now());
            accountRepository.save(account);
        }
        
        // Verify all accounts were saved
        assertEquals(currencies.length, accountRepository.count());
        
        // Verify each currency
        List<Account> allAccounts = accountRepository.findAll();
        for (String currency : currencies) {
            assertTrue(allAccounts.stream().anyMatch(a -> currency.equals(a.getCurrency())));
        }
    }

    @Test
    void testFindByIdWithNonExistentId() {
        UUID randomId = UUID.randomUUID();
        
        // Try to find non-existent account
        assertFalse(accountRepository.findById(randomId).isPresent());
    }

    @Test
    void testLargeBalanceValues() {
        // Test with large balance values
        BigDecimal largeBalance = new BigDecimal("999999999999999.99");
        
        Account account = new Account();
        account.setOwnerName("Large Balance Test");
        account.setBalance(largeBalance);
        account.setCurrency("USD");
        account.setUpdatedAt(Instant.now());
        
        Account savedAccount = accountRepository.save(account);
        
        assertNotNull(savedAccount.getId());
        assertEquals(0, largeBalance.compareTo(savedAccount.getBalance()));
    }

    @Test
    void testTimestampPersistence() {
        Instant beforeSave = Instant.now();
        
        Account account = new Account();
        account.setOwnerName("Timestamp Test");
        account.setBalance(BigDecimal.valueOf(1000));
        account.setCurrency("USD");
        
        Account savedAccount = accountRepository.save(account);
        
        assertNotNull(savedAccount.getUpdatedAt());
        assertTrue(savedAccount.getUpdatedAt().isAfter(beforeSave.minusSeconds(1)));
        assertTrue(savedAccount.getUpdatedAt().isBefore(Instant.now().plusSeconds(1)));
    }

    @Test
    void testAccountWithSpecialCharactersInName() {
        // Test with special characters in owner name
        Account account = new Account();
        account.setOwnerName("O'Brien & Co. <Test> \"Account\"");
        account.setBalance(BigDecimal.valueOf(1000));
        account.setCurrency("USD");
        account.setUpdatedAt(Instant.now());
        
        Account savedAccount = accountRepository.save(account);
        
        assertNotNull(savedAccount.getId());
        assertEquals("O'Brien & Co. <Test> \"Account\"", savedAccount.getOwnerName());
    }

    @Test
    void testMultipleAccountsWithSameOwnerName() {
        // Create multiple accounts with same owner name but different currencies
        String ownerName = "Same Owner";
        
        Account account1 = new Account();
        account1.setOwnerName(ownerName);
        account1.setBalance(BigDecimal.valueOf(1000));
        account1.setCurrency("USD");
        account1.setUpdatedAt(Instant.now());
        
        Account account2 = new Account();
        account2.setOwnerName(ownerName);
        account2.setBalance(BigDecimal.valueOf(2000));
        account2.setCurrency("EUR");
        account2.setUpdatedAt(Instant.now());
        
        accountRepository.save(account1);
        accountRepository.save(account2);
        
        // Both accounts should be saved as separate entities
        assertEquals(2, accountRepository.count());
        
        List<Account> accounts = accountRepository.findAll();
        assertEquals(2, accounts.stream().filter(a -> ownerName.equals(a.getOwnerName())).count());
    }

    @Test
    void testAccountPrecisionAndScale() {
        // Test with precise decimal values
        BigDecimal preciseAmount = new BigDecimal("1234567890123456.78");
        
        Account account = new Account();
        account.setOwnerName("Precision Test");
        account.setBalance(preciseAmount);
        account.setCurrency("USD");
        account.setUpdatedAt(Instant.now());
        
        Account savedAccount = accountRepository.save(account);
        Account foundAccount = accountRepository.findById(savedAccount.getId()).orElse(null);
        
        assertNotNull(foundAccount);
        assertEquals(0, preciseAmount.compareTo(foundAccount.getBalance()));
    }

    @Test
    void testDeleteAllAccounts() {
        // Create multiple accounts
        for (int i = 0; i < 5; i++) {
            Account account = new Account();
            account.setOwnerName("Account " + i);
            account.setBalance(BigDecimal.valueOf(100 * (i + 1)));
            account.setCurrency("USD");
            account.setUpdatedAt(Instant.now());
            accountRepository.save(account);
        }
        
        // Verify all were saved
        assertEquals(5, accountRepository.count());
        
        // Delete all
        accountRepository.deleteAll();
        
        // Verify all were deleted
        assertEquals(0, accountRepository.count());
        assertTrue(accountRepository.findAll().isEmpty());
    }

    @Test
    void testAccountExistsById() {
        // Create and save an account
        Account account = new Account();
        account.setOwnerName("Exists Test");
        account.setBalance(BigDecimal.valueOf(1000));
        account.setCurrency("USD");
        account.setUpdatedAt(Instant.now());
        
        Account savedAccount = accountRepository.save(account);
        
        // Test existsById
        assertTrue(accountRepository.existsById(savedAccount.getId()));
        assertFalse(accountRepository.existsById(UUID.randomUUID()));
    }

    @Test
    void testNegativeBalanceAfterDebit() {
        // Create an account
        Account account = new Account();
        account.setOwnerName("Negative Test");
        account.setBalance(BigDecimal.valueOf(50));
        account.setCurrency("USD");
        account.setUpdatedAt(Instant.now());
        
        // Try to debit more than balance
        assertThrows(InsufficientFundsException.class, () -> {
            account.withdraw(BigDecimal.valueOf(100));
        }, "Insufficient funds");
        
        // Balance should remain unchanged
        assertEquals(0, BigDecimal.valueOf(50).compareTo(account.getBalance()));
    }

}
