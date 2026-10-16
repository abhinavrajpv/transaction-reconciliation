package com.example.reconciliation.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.reconciliation.entity.BatchAudit;

public interface BatchAuditRepository extends JpaRepository<BatchAudit, Long> {

	boolean existsByJobInstanceId(Long jobInstanceId);
}