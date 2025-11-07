package dev.aratax.example.model.vo;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;

import dev.aratax.example.model.po.Account;
import dev.aratax.example.model.po.LedgerEntry;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TransactionResponse {
    
    private String status;
    private Instant timestamp;
    private AccountVo account;
    private LedgerEntryDto ledgerEntry;
    private String transactionId;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AccountVo {
        private UUID id;
        private String ownerName;
        private String currency;
        private BigDecimal balance;
        private long version;
        private Instant updatedAt;
        
        public static AccountVo from(Account account) {
            return AccountVo.builder()
                .id(account.getId())
                .ownerName(account.getOwnerName())
                .currency(account.getCurrency())
                .balance(account.getBalance())
                .version(account.getVersion())
                .updatedAt(account.getUpdatedAt())
                .build();
        }
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LedgerEntryDto {
        private UUID id;
        private UUID accountId;
        private String direction;
        private BigDecimal amount;
        private String reason;
        private Instant createdAt;
        
        public static LedgerEntryDto from(LedgerEntry entry) {
            return LedgerEntryDto.builder()
                .id(entry.getId())
                .accountId(entry.getAccount().getId())
                .direction(entry.getDirection().name())
                .amount(entry.getAmount())
                .reason(entry.getReason())
                .createdAt(entry.getCreatedAt())
                .build();
        }
    }
    
    public static TransactionResponse success(Account account, LedgerEntry ledgerEntry) {
        return TransactionResponse.builder()
            .status("SUCCESS")
            .timestamp(Instant.now())
            .account(AccountVo.from(account))
            .ledgerEntry(ledgerEntry != null ? LedgerEntryDto.from(ledgerEntry) : null)
            .transactionId(ledgerEntry != null ? ledgerEntry.getId().toString() : null)
            .build();
    }
}
