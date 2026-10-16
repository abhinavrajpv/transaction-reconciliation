package com.example.reconciliation.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.reconciliation.dto.ReconciliationResultDto;
import com.example.reconciliation.dto.SecondCsvRecordDto;
import com.example.reconciliation.entity.TransactionRecord;
import com.example.reconciliation.repository.TransactionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TransactionReconciliationService {

	private final TransactionRepository transactionRepository;

	@Transactional(readOnly = true)
	public ReconciliationResultDto reconcile(SecondCsvRecordDto record, Long jobInstanceId) {

		ReconciliationResultDto result = new ReconciliationResultDto();
		result.setRowNumber(record.getRowNumber());
		result.setTransactionId(record.getTransactionId());

		TransactionRecord existing = transactionRepository
				.findByTransactionIdAndJobInstanceId(record.getTransactionId().trim(), jobInstanceId).orElse(null);

		if (existing == null) {
			result.setStatus("MISSING_FROM_DATABASE");
			result.setAction("NOT_INSERTED");
			result.setMessage("Transaction ID not found in database");
			result.setActualAmount(parseAmount(record.getAmount()));
			return result;
		}

		BigDecimal databaseAmount = existing.getAmount();
		BigDecimal sourceAmount = parseAmount(record.getAmount());

		result.setExistingTransaction(existing);
		result.setExpectedAmount(databaseAmount);
		result.setActualAmount(sourceAmount);

		if (databaseAmount.compareTo(sourceAmount) == 0) {
			result.setStatus("MATCHED");
			result.setAction("NO_CHANGE");
			result.setMessage("Transaction amount matches");
		} else {
			result.setStatus("MISMATCHED");
			result.setAction("DATABASE_UPDATED");
			result.setMessage("Transaction amount differs from source");
		}

		return result;
	}

	private BigDecimal parseAmount(String amount) {
		return new BigDecimal(amount.trim());
	}
}