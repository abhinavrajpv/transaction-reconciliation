package com.example.reconciliation.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "batch_audit", uniqueConstraints = @UniqueConstraint(name = "uk_batch_audit_job_instance", columnNames = "job_instance_id"))
@Getter
@Setter
public class BatchAudit {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private Long jobInstanceId;
	private String firstFileName;
	private String secondFileName;
	private long totalFirstFileRecords;
	private long matchedRecords;
	private long mismatchedRecords;
	private long invalidRecords;
	private long duplicateRecords;
	private long missingFromFirstFileRecords;
	private LocalDateTime startedAt;
	private LocalDateTime completedAt;
	private String status;
}