package com.example.reconciliation.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "transaction_record", uniqueConstraints = @UniqueConstraint(name = "uk_transaction_id", columnNames = "transaction_id"))
@Getter
@Setter
public class TransactionRecord {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "transaction_id", nullable = false, unique = true)
	private String transactionId;

	private String accountNumber;
	private LocalDate transactionDate;
	private BigDecimal amount;
	private String currency;
	private String transactionType;
	private String description;
}