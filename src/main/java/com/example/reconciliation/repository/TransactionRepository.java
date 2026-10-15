package com.example.reconciliation.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.reconciliation.entity.TransactionRecord;

public interface TransactionRepository extends JpaRepository<TransactionRecord, Long> {

	boolean existsByTransactionId(String transactionId);
}