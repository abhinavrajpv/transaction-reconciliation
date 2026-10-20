package com.example.reconciliation.processor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;

import com.example.reconciliation.constants.FileValidationMessages;
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
			   return FileValidationMessages.TRANSACTION_ID_REQUIRED;
		}
		if (!record.getTransactionId().trim().matches("[A-Za-z0-9]+")) {
			 return FileValidationMessages.INVALID_TRANSACTION_ID_FORMAT;
		}

		if (record.getAccountNumber() == null || record.getAccountNumber().isBlank()) {
			return FileValidationMessages.ACCOUNT_NUMBER_REQUIRED;
		}
		if (!record.getAccountNumber().trim().matches("[A-Za-z0-9]{8}")) {
			 return FileValidationMessages.INVALID_ACCOUNT_NUMBER_FORMAT;
		}

		if (record.getTransactionDate() == null || record.getTransactionDate().isBlank()) {
			return FileValidationMessages.TRANSACTION_DATE_REQUIRED;
		}

		if (record.getAmount() == null || record.getAmount().isBlank()) {    
			  return FileValidationMessages.AMOUNT_REQUIRED;
		}

		if (record.getCurrency() == null || record.getCurrency().isBlank()) {
			 return FileValidationMessages.CURRENCY_REQUIRED;
		}       
		if (!record.getCurrency().trim().matches("[A-Za-z]{3}")) {
			 return FileValidationMessages.INVALID_CURRENCY_FORMAT;
		}

		if (record.getTransactionType() == null || record.getTransactionType().isBlank()) {
			   return FileValidationMessages.TRANSACTION_TYPE_REQUIRED;
		}

		try {
			LocalDate.parse(record.getTransactionDate().trim());

			BigDecimal amount = new BigDecimal(record.getAmount().trim());

			if (amount.signum() < 0) {
			     return FileValidationMessages.NEGATIVE_AMOUNT;
			}

		} catch (DateTimeParseException ex) {
			   return FileValidationMessages.INVALID_TRANSACTION_DATE_FORMAT;
		} catch (NumberFormatException ex) {
			  return FileValidationMessages.INVALID_AMOUNT;
		}
		return null;
	}
}