package com.example.reconciliation.writer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.database.JdbcBatchItemWriter;

import com.example.reconciliation.dto.FirstCsvRecordDto;
import com.example.reconciliation.dto.TransactionProcessingResultDto;
import com.example.reconciliation.entity.TransactionRecord;
import com.example.reconciliation.enums.RecordProcessingStatus;
import com.example.reconciliation.repository.BatchAuditRepository;
import com.example.reconciliation.repository.TransactionRepository;

@ExtendWith(MockitoExtension.class)
class FirstCsvItemWriterTest {

	@Mock
	TransactionRepository transactionRepository;

	@Mock
	JdbcBatchItemWriter<TransactionRecord> jdbcBatchWriter;

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

	private FirstCsvItemWriter writer;

	@BeforeEach
	void setUp() {
		writer = new FirstCsvItemWriter(transactionRepository, jdbcBatchWriter, resultsWriter, auditRepository);

		when(stepExecution.getJobExecution()).thenReturn(jobExecution);
		when(jobExecution.getJobInstance()).thenReturn(jobInstance);
		when(jobInstance.getInstanceId()).thenReturn(42L);
		when(jobExecution.getId()).thenReturn(101L);
	}

	private TransactionProcessingResultDto result(String id, RecordProcessingStatus status) {

		FirstCsvRecordDto original = new FirstCsvRecordDto();
		original.setRowNumber(2);
		original.setTransactionId(id);

		TransactionProcessingResultDto result = new TransactionProcessingResultDto();

		result.setOriginalRecord(original);
		result.setStatus(status);

		if (status != RecordProcessingStatus.INVALID) {
			TransactionRecord transaction = new TransactionRecord();
			transaction.setTransactionId(id);
			transaction.setAccountNumber("ACC12345");
			transaction.setTransactionDate(LocalDate.of(2026, 10, 16));
			transaction.setAmount(new BigDecimal("100.00"));
			transaction.setCurrency("INR");
			transaction.setTransactionType("CREDIT");
			transaction.setDescription("Test");

			result.setTransaction(transaction);
		}

		return result;
	}

	@Test
	void shouldInitializeResultsWriterWithExecutionAndInstanceIds() throws IOException {
		writer.beforeStep(stepExecution);

		verify(resultsWriter).initialize(101L, 42L);
	}

	@Test
	void shouldThrowIllegalStateExceptionWhenResultsWriterInitializationFails() throws Exception {

		doThrow(new IOException("File initialization failed")).when(resultsWriter).initialize(101L, 42L);

		IllegalStateException exception = assertThrows(IllegalStateException.class,
				() -> writer.beforeStep(stepExecution));

		assertEquals("Unable to initialize reconciliation results CSV", exception.getMessage());

		assertInstanceOf(IOException.class, exception.getCause());
		assertEquals("File initialization failed", exception.getCause().getMessage());
	}

	@Test
	void shouldInsertNewValidTransactionAndIncrementTotal() throws Exception {

		writer.beforeStep(stepExecution);

		var result = result("TXN001", RecordProcessingStatus.VALID);

		when(transactionRepository.findByTransactionIdInAndJobInstanceId(Set.of("TXN001"), 42L)).thenReturn(List.of());

		writer.write(new Chunk<>(List.of(result)));

		assertEquals(42L, result.getTransaction().getJobInstanceId());

		verify(jdbcBatchWriter).write(
				argThat(chunk -> chunk.getItems().size() == 1 && chunk.getItems().get(0) == result.getTransaction()));

		verify(auditRepository).incrementCounts(42L, 1, 0, 0, 0, 0, 0);

		verify(resultsWriter, never()).write(result);
	}

	@Test
	void shouldWriteInvalidRecordAndIncrementInvalidCount() throws Exception {

		writer.beforeStep(stepExecution);

		var invalid = result("TXN002", RecordProcessingStatus.INVALID);
		invalid.setMessage("Invalid amount");

		writer.write(new Chunk<>(List.of(invalid)));

		verify(resultsWriter).write(invalid);
		verify(jdbcBatchWriter, never()).write(any());

		verify(transactionRepository, never()).findByTransactionIdInAndJobInstanceId(any(), any());

		verify(auditRepository).incrementCounts(42L, 1, 0, 0, 1, 0, 0);
	}

	@Test
	void shouldRejectDuplicateWithinSameChunk() throws Exception {
		writer.beforeStep(stepExecution);

		var first = result("TXN003", RecordProcessingStatus.VALID);
		var second = result("TXN003", RecordProcessingStatus.VALID);

		when(transactionRepository.findByTransactionIdInAndJobInstanceId(Set.of("TXN003"), 42L)).thenReturn(List.of());

		writer.write(new Chunk<>(List.of(first, second)));

		assertEquals(RecordProcessingStatus.VALID, first.getStatus());
		assertEquals(RecordProcessingStatus.DUPLICATE, second.getStatus());
		assertEquals("Transaction ID already exists", second.getMessage());

		verify(jdbcBatchWriter).write(argThat(chunk -> chunk.getItems().size() == 1));

		verify(resultsWriter).write(second);

		verify(auditRepository).incrementCounts(42L, 2, 0, 0, 0, 1, 0);
	}

	@Test
	void shouldRejectRecordAlreadyInDatabase() throws Exception {
		writer.beforeStep(stepExecution);

		var duplicate = result("TXN004", RecordProcessingStatus.VALID);

		TransactionRecord existing = new TransactionRecord();
		existing.setTransactionId("TXN004");

		when(transactionRepository.findByTransactionIdInAndJobInstanceId(Set.of("TXN004"), 42L))
				.thenReturn(List.of(existing));

		writer.write(new Chunk<>(List.of(duplicate)));

		assertEquals(RecordProcessingStatus.DUPLICATE, duplicate.getStatus());

		verify(resultsWriter).write(duplicate);
		verify(jdbcBatchWriter, never()).write(any());

		verify(auditRepository).incrementCounts(42L, 1, 0, 0, 0, 1, 0);
	}

	@Test
	void shouldSkipDatabaseLookupWhenChunkHasOnlyInvalidRecords() throws Exception {

		writer.beforeStep(stepExecution);

		var invalid = result("TXN005", RecordProcessingStatus.INVALID);

		writer.write(new Chunk<>(List.of(invalid)));

		verify(transactionRepository, never()).findByTransactionIdInAndJobInstanceId(any(), any());

		verify(jdbcBatchWriter, never()).write(any());
	}

	@Test
	void shouldSkipBatchInsertWhenNoValidRecordsRemain() throws Exception {

		writer.beforeStep(stepExecution);

		var duplicate = result("TXN006", RecordProcessingStatus.VALID);

		TransactionRecord existing = new TransactionRecord();
		existing.setTransactionId("TXN006");

		when(transactionRepository.findByTransactionIdInAndJobInstanceId(Set.of("TXN006"), 42L))
				.thenReturn(List.of(existing));

		writer.write(new Chunk<>(List.of(duplicate)));

		verify(jdbcBatchWriter, never()).write(any());
	}

	@Test
	void shouldClearSeenIdsWhenStepStartsAgain() throws Exception {
		writer.beforeStep(stepExecution);

		var first = result("TXN007", RecordProcessingStatus.VALID);

		when(transactionRepository.findByTransactionIdInAndJobInstanceId(Set.of("TXN007"), 42L)).thenReturn(List.of());

		writer.write(new Chunk<>(List.of(first)));

		// Simulate a new step execution using the same writer instance.
		writer.beforeStep(stepExecution);

		var second = result("TXN007", RecordProcessingStatus.VALID);

		when(transactionRepository.findByTransactionIdInAndJobInstanceId(Set.of("TXN007"), 42L)).thenReturn(List.of());

		writer.write(new Chunk<>(List.of(second)));

		assertEquals(RecordProcessingStatus.VALID, second.getStatus());

		verify(jdbcBatchWriter, times(2)).write(any());
	}

	@Test
    void shouldSkipDatabaseLookupWhenNonInvalidResultHasNoTransaction()
            throws Exception {

        writer.beforeStep(stepExecution);

        TransactionProcessingResultDto result =
                new TransactionProcessingResultDto();

        result.setStatus(RecordProcessingStatus.VALID);
        result.setTransaction(null);

        assertThrows(
                NullPointerException.class,
                () -> writer.write(new Chunk<>(List.of(result)))
        );

        verify(transactionRepository, never())
                .findByTransactionIdInAndJobInstanceId(any(), any());
    }
}