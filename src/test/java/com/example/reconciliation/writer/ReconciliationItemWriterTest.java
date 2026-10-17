
package com.example.reconciliation.writer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.infrastructure.item.Chunk;

import com.example.reconciliation.dto.ReconciliationResultDto;
import com.example.reconciliation.entity.TransactionRecord;
import com.example.reconciliation.enums.ReconciliationAction;
import com.example.reconciliation.enums.ReconciliationStatus;
import com.example.reconciliation.repository.BatchAuditRepository;
import com.example.reconciliation.repository.TransactionRepository;

@ExtendWith(MockitoExtension.class)
public class ReconciliationItemWriterTest {

	@Mock
	TransactionRepository transactionRepository;

	@Mock
	ReconciliationResultsCsvWriter resultsWriter;

	@Mock
	BatchAuditRepository auditRepository;

	@Mock
	StepExecution stepExecution;

	@Mock
	JobExecution jobExecution;

	@Mock
	JobInstance jobInstance;

	private ReconciliationItemWriter writer;

	@BeforeEach
	void setUp() {
		writer = new ReconciliationItemWriter(transactionRepository, resultsWriter, auditRepository);

		when(stepExecution.getJobExecution()).thenReturn(jobExecution);
		when(jobExecution.getJobInstance()).thenReturn(jobInstance);
		when(jobInstance.getInstanceId()).thenReturn(42L);
		when(jobExecution.getId()).thenReturn(101L);

		writer.beforeStep(stepExecution);
	}

	private ReconciliationResultDto result(String status) {
		ReconciliationResultDto result = new ReconciliationResultDto();
		result.setRowNumber(2);
		result.setTransactionId("TXN001");
		result.setStatus(ReconciliationStatus.valueOf(status));
		result.setAction(ReconciliationAction.NO_CHANGE);
		result.setMessage("test-message");
		return result;
	}

	@Test
	void shouldInitializeCsvWriterWithJobIds() throws Exception {
		verify(resultsWriter).initialize(101L, 42L);
	}

	@Test
	void shouldCountMatchedRecordsWithoutUpdatingOrWritingCsv() throws Exception {
		var matched = result("MATCHED");

		writer.write(new Chunk<>(List.of(matched)));

		verifyNoInteractions(transactionRepository);
		verify(resultsWriter, never()).write(any(ReconciliationResultDto.class));
		verify(auditRepository).incrementCounts(42L, 0, 1, 0, 0, 0, 0);
	}

	@Test
	void shouldUpdateAmountAndWriteMismatchedRecord() throws Exception {
		var mismatched = result("MISMATCHED");

		TransactionRecord existing = new TransactionRecord();
		existing.setTransactionId("TXN001");
		existing.setAmount(new BigDecimal("100.00"));

		mismatched.setExistingTransaction(existing);
		mismatched.setActualAmount(new BigDecimal("125.50"));

		writer.write(new Chunk<>(List.of(mismatched)));

		assertEquals(new BigDecimal("125.50"), existing.getAmount());

		verify(transactionRepository).save(existing);
		verify(resultsWriter).write(mismatched);
		verify(auditRepository).incrementCounts(42L, 0, 0, 1, 0, 0, 0);
	}

	@Test
	void shouldWriteMissingRecordWithoutInsertingIntoDatabase() throws Exception {
		var missing = result("MISSING_FROM_DATABASE");

		writer.write(new Chunk<>(List.of(missing)));

		verify(resultsWriter).write(missing);
		verifyNoInteractions(transactionRepository);
		verify(auditRepository).incrementCounts(42L, 0, 0, 0, 0, 0, 1);
	}

	@Test
	void shouldProcessMixedChunkAndIncrementAllCounters() throws Exception {
		var matched = result("MATCHED");

		var mismatched = result("MISMATCHED");
		var existing = new TransactionRecord();
		existing.setAmount(new BigDecimal("10.00"));
		mismatched.setExistingTransaction(existing);
		mismatched.setActualAmount(new BigDecimal("20.00"));

		var missing = result("MISSING_FROM_DATABASE");

		writer.write(new Chunk<>(List.of(matched, mismatched, missing)));

		verify(transactionRepository).save(existing);
		verify(resultsWriter).write(mismatched);
		verify(resultsWriter).write(missing);

		verify(auditRepository).incrementCounts(42L, 0, 1, 1, 0, 0, 1);
	}

	@Test
	void shouldRejectUnexpectedStatus() {
		var invalid = result("INVALID");

		assertThrows(IllegalStateException.class, () -> writer.write(new Chunk<>(List.of(invalid))));

		verify(auditRepository, never()).incrementCounts(any(), anyLong(), anyLong(), anyLong(), anyLong(), anyLong(),
				anyLong());
	}

	@Test
	void shouldAllowEmptyChunkAndIncrementZeroCounts() throws Exception {
		writer.write(new Chunk<>());

		verify(auditRepository).incrementCounts(42L, 0, 0, 0, 0, 0, 0);
		verifyNoInteractions(transactionRepository);
		verify(resultsWriter, never()).write(any(ReconciliationResultDto.class));
	}
}
