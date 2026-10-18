
package com.example.reconciliation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.reconciliation.dto.ReconciliationResultDto;
import com.example.reconciliation.dto.SecondCsvRecordDto;
import com.example.reconciliation.entity.TransactionRecord;
import com.example.reconciliation.enums.ReconciliationAction;
import com.example.reconciliation.enums.ReconciliationStatus;
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
		SecondCsvRecordDto input = record("TXN001", "100.00");
		TransactionRecord transaction = existing("TXN001", "100.00");

		when(transactionRepository.findByTransactionIdAndJobInstanceId("TXN001", 42L))
				.thenReturn(Optional.of(transaction));

		ReconciliationResultDto result = service.reconcile(input, 42L);

		assertEquals(7, result.getRowNumber());
		assertEquals("TXN001", result.getTransactionId());
		assertEquals(ReconciliationStatus.MATCHED, result.getStatus());
		assertEquals(ReconciliationAction.NO_CHANGE, result.getAction());
		assertEquals("Transaction amount matches", result.getMessage());
		assertEquals(new BigDecimal("100.00"), result.getExpectedAmount());
		assertEquals(new BigDecimal("100.00"), result.getActualAmount());
		assertSame(transaction, result.getExistingTransaction());

		verify(transactionRepository).findByTransactionIdAndJobInstanceId("TXN001", 42L);
		verifyNoMoreInteractions(transactionRepository);
	}

	@Test
	void shouldReturnMatchedWhenBigDecimalScaleDiffers() {
		SecondCsvRecordDto input = record("TXN001", "100.0");
		TransactionRecord transaction = existing("TXN001", "100.00");

		when(transactionRepository.findByTransactionIdAndJobInstanceId("TXN001", 42L))
				.thenReturn(Optional.of(transaction));

		ReconciliationResultDto result = service.reconcile(input, 42L);

		assertEquals(ReconciliationStatus.MATCHED, result.getStatus());
		assertEquals(ReconciliationAction.NO_CHANGE, result.getAction());
	}

	@Test
	void shouldReturnMismatchedWhenAmountsDiffer() {
		SecondCsvRecordDto input = record("TXN001", "125.50");
		TransactionRecord transaction = existing("TXN001", "100.00");

		when(transactionRepository.findByTransactionIdAndJobInstanceId("TXN001", 42L))
				.thenReturn(Optional.of(transaction));

		ReconciliationResultDto result = service.reconcile(input, 42L);

		assertEquals(ReconciliationStatus.MISMATCHED, result.getStatus());
		assertEquals(ReconciliationAction.DATABASE_UPDATED, result.getAction());
		assertEquals("Transaction amount differs from source", result.getMessage());
		assertEquals(new BigDecimal("100.00"), result.getExpectedAmount());
		assertEquals(new BigDecimal("125.50"), result.getActualAmount());
		assertSame(transaction, result.getExistingTransaction());
		assertEquals(new BigDecimal("100.00"), transaction.getAmount());
	}

	@Test
	void shouldReturnMissingWhenTransactionDoesNotExist() {
		SecondCsvRecordDto input = record("TXN999", "55.25");

		when(transactionRepository.findByTransactionIdAndJobInstanceId("TXN999", 42L)).thenReturn(Optional.empty());

		ReconciliationResultDto result = service.reconcile(input, 42L);

		assertEquals(7, result.getRowNumber());
		assertEquals("TXN999", result.getTransactionId());
		assertEquals("MISSING_FROM_DATABASE", result.getStatus().name());
		assertEquals("NOT_INSERTED", result.getAction().name());
		assertEquals("Transaction ID not found in database", result.getMessage());
		assertNull(result.getExpectedAmount());
		assertEquals(new BigDecimal("55.25"), result.getActualAmount());
		assertNull(result.getExistingTransaction());

		verify(transactionRepository).findByTransactionIdAndJobInstanceId("TXN999", 42L);
	}

	@Test
	void shouldTrimTransactionIdBeforeRepositoryLookup() {
		SecondCsvRecordDto input = record(" TXN001 ", "100");
		TransactionRecord transaction = existing("TXN001", "100");

		when(transactionRepository.findByTransactionIdAndJobInstanceId("TXN001", 42L))
				.thenReturn(Optional.of(transaction));

		ReconciliationResultDto result = service.reconcile(input, 42L);

		assertEquals(ReconciliationStatus.MATCHED, result.getStatus());
		verify(transactionRepository).findByTransactionIdAndJobInstanceId("TXN001", 42L);
	}

	@Test
	void shouldPropagateInvalidAmountException() {
		SecondCsvRecordDto input = record("TXN001", "not-a-number");

		when(transactionRepository.findByTransactionIdAndJobInstanceId("TXN001", 42L)).thenReturn(Optional.empty());

		assertThrows(NumberFormatException.class, () -> service.reconcile(input, 42L));
	}

	@Test
	void shouldPropagateInvalidAmountWhenTransactionExists() {
		SecondCsvRecordDto input = record("TXN001", "not-a-number");
		TransactionRecord transaction = existing("TXN001", "100");

		when(transactionRepository.findByTransactionIdAndJobInstanceId("TXN001", 42L))
				.thenReturn(Optional.of(transaction));

		assertThrows(NumberFormatException.class, () -> service.reconcile(input, 42L));
	}
}
