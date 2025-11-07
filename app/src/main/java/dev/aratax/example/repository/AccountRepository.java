package dev.aratax.example.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import dev.aratax.example.model.po.Account;
import jakarta.persistence.LockModeType;

public interface AccountRepository extends JpaRepository<Account, UUID> {

  // Pessimistic row lock (SELECT ... FOR UPDATE)
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select a from Account a where a.id = :id")
  Optional<Account> findForUpdate(@Param("id") UUID id);
  
}

