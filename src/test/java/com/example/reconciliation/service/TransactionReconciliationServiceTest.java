
package com.example.reconciliation.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.reconciliation.dto.SecondCsvRecordDto;
import com.example.reconciliation.entity.TransactionRecord;
import com.example.reconciliation.repository.TransactionRepository;

@ExtendWith(MockitoExtension.class)
public class TransactionReconciliationServiceTest {

	@Mock
	private TransactionRepository transactionRepository;

	private TransactionReconciliationService service;

	@BeforeEach
	void setUp() {
		service = new TransactionReconciliationService(transactionRepository);
	}

	private SecondCsvRecordDto record(String transactionId, String amount) {
		SecondCsvRecordDto record = new SecondCsvRecordDto();
		record.setRowNumber(7);
		record.setTransactionId(transactionId);
		record.setAmount(amount);
		return record;
	}

	private TransactionRecord existing(String id, String amount) {
		TransactionRecord transaction = new TransactionRecord();
		transaction.setTransactionId(id);
		transaction.setAmount(new BigDecimal(amount));
		transaction.setJobInstanceId(42L);
		return transaction;
	}

	@Test
	void shouldReturnMatchedWhenAmountsAreEqual() {
		var input = record("TXN001", "100.00");
		var transaction = existing("TXN001", "100.00");

		when(transactionRepository.findByTransactionIdAndJobInstanceId("TXN001", 42L))
				.thenReturn(Optional.of(transaction));

		var result = service.reconcile(input, 42L);

		assertEquals(7, result.getRowNumber());
		assertEquals("TXN001", result.getTransactionId());
		assertEquals("MATCHED", result.getStatus());
		assertEquals("NO_CHANGE", result.getAction());
		assertEquals("Transaction amount matches", result.getMessage());
		assertEquals(new BigDecimal("100.00"), result.getExpectedAmount());
		assertEquals(new BigDecimal("100.00"), result.getActualAmount());
		assertSame(transaction, result.getExistingTransaction());

		verify(transactionRepository).findByTransactionIdAndJobInstanceId("TXN001", 42L);
		verifyNoMoreInteractions(transactionRepository);
	}

	@Test
	void shouldReturnMatchedWhenBigDecimalScaleDiffers() {
		var input = record("TXN001", "100.0");
		var transaction = existing("TXN001", "100.00");

		when(transactionRepository.findByTransactionIdAndJobInstanceId("TXN001", 42L))
				.thenReturn(Optional.of(transaction));

		var result = service.reconcile(input, 42L);

		assertEquals("MATCHED", result.getStatus());
		assertEquals("NO_CHANGE", result.getAction());
	}

	@Test
	void shouldReturnMismatchedWhenAmountsDiffer() {
		var input = record("TXN001", "125.50");
		var transaction = existing("TXN001", "100.00");

		when(transactionRepository.findByTransactionIdAndJobInstanceId("TXN001", 42L))
				.thenReturn(Optional.of(transaction));

		var result = service.reconcile(input, 42L);

		assertEquals("MISMATCHED", result.getStatus());
		assertEquals("DATABASE_UPDATED", result.getAction());
		assertEquals("Transaction amount differs from source", result.getMessage());
		assertEquals(new BigDecimal("100.00"), result.getExpectedAmount());
		assertEquals(new BigDecimal("125.50"), result.getActualAmount());
		assertSame(transaction, result.getExistingTransaction());
		assertEquals(new BigDecimal("100.00"), transaction.getAmount());
	}

	@Test
	void shouldReturnMissingWhenTransactionDoesNotExist() {
		var input = record("TXN999", "55.25");

		when(transactionRepository.findByTransactionIdAndJobInstanceId("TXN999", 42L)).thenReturn(Optional.empty());

		var result = service.reconcile(input, 42L);

		assertEquals(7, result.getRowNumber());
		assertEquals("TXN999", result.getTransactionId());
		assertEquals("MISSING_FROM_DATABASE", result.getStatus());
		assertEquals("NOT_INSERTED", result.getAction());
		assertEquals("Transaction ID not found in database", result.getMessage());
		assertNull(result.getExpectedAmount());
		assertEquals(new BigDecimal("55.25"), result.getActualAmount());
		assertNull(result.getExistingTransaction());

		verify(transactionRepository).findByTransactionIdAndJobInstanceId("TXN999", 42L);
	}

	@Test
	void shouldTrimTransactionIdBeforeRepositoryLookup() {
		var input = record(" TXN001 ", "100");
		var transaction = existing("TXN001", "100");

		when(transactionRepository.findByTransactionIdAndJobInstanceId("TXN001", 42L))
				.thenReturn(Optional.of(transaction));

		var result = service.reconcile(input, 42L);

		assertEquals("MATCHED", result.getStatus());
		verify(transactionRepository).findByTransactionIdAndJobInstanceId("TXN001", 42L);
	}

	@Test
	void shouldPropagateInvalidAmountException() {
		var input = record("TXN001", "not-a-number");

		when(transactionRepository.findByTransactionIdAndJobInstanceId("TXN001", 42L)).thenReturn(Optional.empty());

		assertThrows(NumberFormatException.class, () -> service.reconcile(input, 42L));
	}

	@Test
	void shouldPropagateInvalidAmountWhenTransactionExists() {
		var input = record("TXN001", "not-a-number");
		var transaction = existing("TXN001", "100");

		when(transactionRepository.findByTransactionIdAndJobInstanceId("TXN001", 42L))
				.thenReturn(Optional.of(transaction));

		assertThrows(NumberFormatException.class, () -> service.reconcile(input, 42L));
	}
}
