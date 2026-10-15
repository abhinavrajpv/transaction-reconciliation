package com.example.reconciliation.repository;

import java.util.List;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.reconciliation.entity.TransactionRecord;

public interface TransactionRepository extends JpaRepository<TransactionRecord, Long> {

	boolean existsByTransactionId(String transactionId);

	List<TransactionRecord> findByTransactionIdIn(
            Set<String> transactionIds);
}