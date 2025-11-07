package dev.aratax.example.model.po;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;
import org.springframework.lang.NonNull;

import dev.aratax.example.enums.Direction;
import dev.aratax.example.enums.TransactionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "ledger_entries", schema = "app", 
  indexes = {
    @Index(name = "idx_ledger_account", columnList = "account_id")
})
public class LedgerEntry {

  @Id
  @GeneratedValue
  @Column(columnDefinition = "UUID DEFAULT uuidv7()")
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "account_id", foreignKey = @ForeignKey(name = "fk_ledger_account"))
  private Account account;

  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private Direction direction;

  @Column(nullable = false, precision = 18, scale = 2)
  private BigDecimal amount;

  private String reason;

  @Column(name = "created_at", columnDefinition = "timestamptz", nullable = false)
  private Instant createdAt = Instant.now();

  @NonNull
  public static LedgerEntry of(Account account, TransactionType type, BigDecimal amt, String reason) {
    var record = new LedgerEntry(); 
    record.account = account; 
    record.direction = Direction.fromTransactionType(type);
    record.amount = amt; 
    record.reason = reason; 
    return record;
  }

}
