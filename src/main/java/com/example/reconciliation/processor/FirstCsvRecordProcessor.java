package com.example.reconciliation.processor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;

import com.example.reconciliation.dto.FirstCsvRecordDto;
import com.example.reconciliation.dto.TransactionProcessingResultDto;
import com.example.reconciliation.entity.TransactionRecord;
import com.example.reconciliation.enums.RecordProcessingStatus;

@Component
public class FirstCsvRecordProcessor implements ItemProcessor<FirstCsvRecordDto, TransactionProcessingResultDto> {

	@Override
	public TransactionProcessingResultDto process(FirstCsvRecordDto record) {
		TransactionProcessingResultDto result = new TransactionProcessingResultDto();
		result.setOriginalRecord(record);
		String validationError = validate(record);
		if (validationError != null) {
			result.setStatus(RecordProcessingStatus.INVALID);
			result.setMessage(validationError);
			return result;
		}
		TransactionRecord transaction = new TransactionRecord();
		transaction.setTransactionId(record.getTransactionId().trim());
		transaction.setAccountNumber(record.getAccountNumber().trim());
		transaction.setTransactionDate(LocalDate.parse(record.getTransactionDate().trim()));
		transaction.setAmount(new BigDecimal(record.getAmount().trim()));
		transaction.setCurrency(record.getCurrency().trim());
		transaction.setTransactionType(record.getTransactionType().trim());
		transaction.setDescription(record.getDescription());
		result.setTransaction(transaction);
		result.setStatus(RecordProcessingStatus.VALID);
		result.setMessage("Record is valid");
		return result;
	}

	private String validate(FirstCsvRecordDto record) {
		if (record.getTransactionId() == null || record.getTransactionId().isBlank()) {
			return "Transaction ID is required";
		}
		if (!record.getTransactionId().trim().matches("[A-Za-z0-9]+")) {
		    return "Transaction ID must contain only letters and numbers";
		}

		if (record.getAccountNumber() == null || record.getAccountNumber().isBlank()) {
			return "Account number is required";
		}
		if (!record.getAccountNumber().trim().matches("[A-Za-z0-9]{8}")) {
		    return "Account number must contain exactly 8 letters or digits";
		}

		if (record.getTransactionDate() == null || record.getTransactionDate().isBlank()) {
			return "Transaction date is required";
		}

		if (record.getAmount() == null || record.getAmount().isBlank()) {    
			return "Amount is required";
		}

		if (record.getCurrency() == null || record.getCurrency().isBlank()) {
			return "Currency is required";
		}
		if (!record.getCurrency().trim().matches("[A-Za-z]{3}")) {
		    return "Currency must contain exactly 3 letters";
		}

		if (record.getTransactionType() == null || record.getTransactionType().isBlank()) {
			return "Transaction type is required";
		}

		try {
			LocalDate.parse(record.getTransactionDate().trim());

			BigDecimal amount = new BigDecimal(record.getAmount().trim());

			if (amount.signum() < 0) {
				return "Amount cannot be negative";
			}

		} catch (DateTimeParseException ex) {
			return "Transaction date must use yyyy-MM-dd format";
		} catch (NumberFormatException ex) {
			return "Amount must be a valid number";
		}
		return null;
	}
}