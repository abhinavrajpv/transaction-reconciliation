package com.example.reconciliation.writer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.batch.core.listener.StepExecutionListener;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.batch.infrastructure.item.database.JdbcBatchItemWriter;
import org.springframework.stereotype.Component;

import com.example.reconciliation.dto.TransactionProcessingResultDto;
import com.example.reconciliation.entity.TransactionRecord;
import com.example.reconciliation.enums.RecordProcessingStatus;
import com.example.reconciliation.repository.TransactionRepository;
import com.example.reconciliation.utility.ProcessingCounters;

@Component
public class FirstCsvItemWriter implements ItemWriter<TransactionProcessingResultDto>, StepExecutionListener {

	private final TransactionRepository transactionRepository;
	private final JdbcBatchItemWriter<TransactionRecord> jdbcBatchWriter;
	private final ReconciliationResultsCsvWriter resultsWriter;
	private final ProcessingCounters counters;
	private Long jobInstanceId;

	public FirstCsvItemWriter(TransactionRepository transactionRepository,
			JdbcBatchItemWriter<TransactionRecord> jdbcBatchWriter, ReconciliationResultsCsvWriter resultsWriter,
			ProcessingCounters counters) {
		this.transactionRepository = transactionRepository;
		this.jdbcBatchWriter = jdbcBatchWriter;
		this.resultsWriter = resultsWriter;
		this.counters = counters;
	}

	private final Set<String> seenTransactionIds = new HashSet<>();

	@Override
	public void beforeStep(StepExecution stepExecution) {
		seenTransactionIds.clear();
		counters.reset();
		this.jobInstanceId = stepExecution.getJobExecution().getJobInstance().getInstanceId();

		try {
			resultsWriter.initialize();
		} catch (IOException ex) {
			throw new IllegalStateException("Unable to initialize reconciliation results CSV", ex);
		}
	}

	@Override
	public void write(Chunk<? extends TransactionProcessingResultDto> chunk) throws Exception {

		List<TransactionRecord> validTransactions = new ArrayList<>();

		Set<String> ids = chunk.getItems().stream().filter(
				result -> result.getStatus() != RecordProcessingStatus.INVALID && result.getTransaction() != null)
				.map(result -> result.getTransaction().getTransactionId()).collect(Collectors.toSet());

		Set<String> existingIds = ids.isEmpty() ? Set.of()
				: transactionRepository.findByTransactionIdInAndJobInstanceId(ids, jobInstanceId).stream()
						.map(TransactionRecord::getTransactionId).collect(Collectors.toSet());

		for (TransactionProcessingResultDto result : chunk) {
			counters.recordFirstFileRecord();

			if (result.getStatus() == RecordProcessingStatus.INVALID) {
				counters.recordInvalid();
				resultsWriter.write(result);
				continue;
			}

			TransactionRecord transaction = result.getTransaction();
			String transactionId = transaction.getTransactionId();
			
			/*
			 * if ("TXN005912".equals(transactionId)) { throw new
			 * RuntimeException("TEST FAILURE: Step 2"); }
			 */
			 
			if (!seenTransactionIds.add(transactionId) || existingIds.contains(transactionId)) {

				result.setStatus(RecordProcessingStatus.DUPLICATE);
				result.setMessage("Transaction ID already exists");
				counters.recordDuplicate();
				resultsWriter.write(result);
				continue;
			}

			transaction.setJobInstanceId(jobInstanceId);
			validTransactions.add(transaction);
		}

		if (!validTransactions.isEmpty()) {
			jdbcBatchWriter.write(new Chunk<>(validTransactions));
		}
	}
}