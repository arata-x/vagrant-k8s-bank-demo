package dev.aratax.example.model.po;
    
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import dev.aratax.example.exception.InsufficientFundsException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Data;

@Data
@Entity
@Table(name = "accounts", schema = "app",
       indexes = @Index(name = "idx_accounts_owner", columnList = "owner_name"))
public class Account {

  @Id @UuidGenerator
  private UUID id;

  @Column(name = "owner_name", nullable = false)
  private String ownerName;

  @Column(length = 3, nullable = false)
  private String currency;

  @Column(nullable = false, precision = 18, scale = 2)
  private BigDecimal balance = BigDecimal.ZERO;

  @Version
  @Column(nullable = false)
  private long version;

  @Column(name = "updated_at", columnDefinition = "timestamptz", nullable = false)
  private Instant updatedAt = Instant.now();
  
  public void deposit(BigDecimal amt) { 
    this.balance = this.balance.add(amt); 
  }
  
  public void withdraw(BigDecimal amt) {
    var newBal = this.balance.subtract(amt);
    if (newBal.signum() < 0) 
      throw new InsufficientFundsException(this.id, amt, this.balance);
    this.balance = newBal;
  }

}
