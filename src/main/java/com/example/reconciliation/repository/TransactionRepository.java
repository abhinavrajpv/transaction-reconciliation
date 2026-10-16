package com.example.reconciliation.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.reconciliation.entity.TransactionRecord;

public interface TransactionRepository extends JpaRepository<TransactionRecord, Long> {

	boolean existsByTransactionId(String transactionId);

	Optional<TransactionRecord> findByTransactionId(String transactionId);

	List<TransactionRecord> findByTransactionIdIn(Set<String> transactionIds);

	Optional<TransactionRecord> findByTransactionIdAndJobInstanceId(String transactionId, Long jobInstanceId);

	List<TransactionRecord> findByTransactionIdInAndJobInstanceId(Collection<String> transactionIds,
			Long jobInstanceId);
}