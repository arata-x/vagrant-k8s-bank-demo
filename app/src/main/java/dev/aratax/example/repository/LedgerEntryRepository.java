package dev.aratax.example.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import dev.aratax.example.model.po.LedgerEntry;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> { }
