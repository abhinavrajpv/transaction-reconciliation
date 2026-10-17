
package com.example.reconciliation.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.example.reconciliation.dto.FirstCsvRecordDto;
import com.example.reconciliation.dto.TransactionProcessingResultDto;
import com.example.reconciliation.enums.RecordProcessingStatus;

public class FirstCsvRecordProcessorTest {

	private FirstCsvRecordProcessor processor;

	@BeforeEach
	void setUp() {
		processor = new FirstCsvRecordProcessor();
	}

	private FirstCsvRecordDto validRecord() {
		FirstCsvRecordDto record = new FirstCsvRecordDto();
		record.setRowNumber(2);
		record.setTransactionId("TXN123");
		record.setAccountNumber("ACC12345");
		record.setTransactionDate("2026-10-16");
		record.setAmount("100.50");
		record.setCurrency("INR");
		record.setTransactionType("CREDIT");
		record.setDescription("Salary");
		return record;
	}

	private void assertInvalid(String expectedMessage, FirstCsvRecordDto record) {
		TransactionProcessingResultDto result = processor.process(record);

		assertEquals(RecordProcessingStatus.INVALID, result.getStatus());
		assertEquals(expectedMessage, result.getMessage());
		assertSame(record, result.getOriginalRecord());
		assertNull(result.getTransaction());
	}

	@Test
	void shouldProcessValidRecord() {
		FirstCsvRecordDto record = validRecord();

		TransactionProcessingResultDto result = processor.process(record);

		assertEquals(RecordProcessingStatus.VALID, result.getStatus());
		assertEquals("Record is valid", result.getMessage());
		assertSame(record, result.getOriginalRecord());

		assertNotNull(result.getTransaction());
		assertEquals("TXN123", result.getTransaction().getTransactionId());
		assertEquals("ACC12345", result.getTransaction().getAccountNumber());
		assertEquals(LocalDate.of(2026, 10, 16), result.getTransaction().getTransactionDate());
		assertEquals(new BigDecimal("100.50"), result.getTransaction().getAmount());
		assertEquals("INR", result.getTransaction().getCurrency());
		assertEquals("CREDIT", result.getTransaction().getTransactionType());
		assertEquals("Salary", result.getTransaction().getDescription());
	}

	@Test
	void shouldTrimFieldsThatAreTrimmedByProcessor() {
		FirstCsvRecordDto record = validRecord();
		record.setTransactionId(" TXN123 ");
		record.setAccountNumber(" ACC12345 ");
		record.setTransactionDate(" 2026-10-16 ");
		record.setAmount(" 100.50 ");
		record.setCurrency(" INR ");
		record.setTransactionType(" CREDIT ");

		var result = processor.process(record);

		assertEquals(RecordProcessingStatus.VALID, result.getStatus());
		assertEquals("TXN123", result.getTransaction().getTransactionId());
		assertEquals("ACC12345", result.getTransaction().getAccountNumber());
		assertEquals("INR", result.getTransaction().getCurrency());
		assertEquals("CREDIT", result.getTransaction().getTransactionType());
	}

	@ParameterizedTest
	@NullSource
	@ValueSource(strings = { "   " })
	void shouldRejectNullOrBlankTransactionId(String transactionId) {
		var record = validRecord();
		record.setTransactionId(transactionId);

		assertInvalid("Transaction ID is required", record);
	}

	@ParameterizedTest
	@ValueSource(strings = { "TXN-123", "TXN@123", "TXN 123", "TXN#123" })
	void shouldRejectSpecialCharactersInTransactionId(String transactionId) {
		var record = validRecord();
		record.setTransactionId(transactionId);
		assertInvalid("Transaction ID must contain only letters and numbers", record);
	}

	@ParameterizedTest
	@NullSource
	@ValueSource(strings = { " " })
	void shouldRejectNullOrBlankAccountNumber(String accountNumber) {
		var record = validRecord();
		record.setAccountNumber(accountNumber);

		assertInvalid("Account number is required", record);
	}

	@ParameterizedTest
	@ValueSource(strings = { "ABC1234", "ABC123456", "ABC12@45" })
	void shouldRejectInvalidAccountNumber(String accountNumber) {
		var record = validRecord();
		record.setAccountNumber(accountNumber);
		assertInvalid("Account number must contain exactly 8 letters or digits", record);
	}

	@ParameterizedTest
	@NullSource
	@ValueSource(strings = { " " })
	void shouldRejectNullOrBlankTransactionDate(String transactionDate) {
		var record = validRecord();
		record.setTransactionDate(transactionDate);

		assertInvalid("Transaction date is required", record);
	}

	@ParameterizedTest
	@ValueSource(strings = { "16-10-2026", "2026-02-30" })
	void shouldRejectMalformedOrImpossibleTransactionDate(String transactionDate) {
		var record = validRecord();
		record.setTransactionDate(transactionDate);

		assertInvalid("Transaction date must use yyyy-MM-dd format", record);
	}

	@ParameterizedTest
	@NullSource
	@ValueSource(strings = { " " })
	void shouldRejectNullOrBlankAmount(String amount) {
		var record = validRecord();
		record.setAmount(amount);

		assertInvalid("Amount is required", record);
	}

	@ParameterizedTest
	@CsvSource({ "abc, Amount must be a valid number", "-0.01, Amount cannot be negative" })
	void shouldRejectInvalidAmount(String amount, String expectedMessage) {
		var record = validRecord();
		record.setAmount(amount);

		assertInvalid(expectedMessage, record);
	}

	@Test
	void shouldAcceptZeroAmount() {
		var record = validRecord();
		record.setAmount("0");

		var result = processor.process(record);

		assertEquals(RecordProcessingStatus.VALID, result.getStatus());
		assertEquals(BigDecimal.ZERO, result.getTransaction().getAmount());
	}

	@ParameterizedTest
	@NullSource
	@ValueSource(strings = { " " })
	void shouldRejectNullOrBlankCurrency(String currency) {
		var record = validRecord();
		record.setCurrency(currency);

		assertInvalid("Currency is required", record);
	}

	@ParameterizedTest
	@ValueSource(strings = { "US", "IN1" })
	void shouldRejectInvalidCurrency(String currency) {
		var record = validRecord();
		record.setCurrency(currency);

		assertInvalid("Currency must contain exactly 3 letters", record);
	}

	@ParameterizedTest
	@NullSource
	@ValueSource(strings = { " " })
	void shouldRejectNullOrBlankTransactionType(String transactionType) {
		var record = validRecord();
		record.setTransactionType(transactionType);

		assertInvalid("Transaction type is required", record);
	}

	@Test
	void shouldPreserveNullDescription() {
		var record = validRecord();
		record.setDescription(null);

		var result = processor.process(record);

		assertEquals(RecordProcessingStatus.VALID, result.getStatus());
		assertNull(result.getTransaction().getDescription());
	}
}
