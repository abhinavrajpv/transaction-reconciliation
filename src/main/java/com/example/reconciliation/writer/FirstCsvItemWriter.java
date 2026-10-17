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
import org.springframework.transaction.annotation.Transactional;

import com.example.reconciliation.dto.TransactionProcessingResultDto;
import com.example.reconciliation.entity.TransactionRecord;
import com.example.reconciliation.enums.RecordProcessingStatus;
import com.example.reconciliation.repository.BatchAuditRepository;
import com.example.reconciliation.repository.TransactionRepository;

@Component
public class FirstCsvItemWriter implements ItemWriter<TransactionProcessingResultDto>, StepExecutionListener {

	private final TransactionRepository transactionRepository;
	private final JdbcBatchItemWriter<TransactionRecord> jdbcBatchWriter;
	private final ReconciliationResultsCsvWriter resultsWriter;
	private Long jobInstanceId;
	private Long jobExecutionId;
	private final BatchAuditRepository auditRepository;

	public FirstCsvItemWriter(TransactionRepository transactionRepository,
			JdbcBatchItemWriter<TransactionRecord> jdbcBatchWriter, ReconciliationResultsCsvWriter resultsWriter,
			BatchAuditRepository auditRepository) {
		this.transactionRepository = transactionRepository;
		this.jdbcBatchWriter = jdbcBatchWriter;
		this.resultsWriter = resultsWriter;
		this.auditRepository = auditRepository;
	}

	private final Set<String> seenTransactionIds = new HashSet<>();

	@Override
	public void beforeStep(StepExecution stepExecution) {
		seenTransactionIds.clear();
		jobInstanceId = stepExecution.getJobExecution().getJobInstance().getInstanceId();
		jobExecutionId = stepExecution.getJobExecution().getId();

		try {
			resultsWriter.initialize(jobExecutionId, jobInstanceId);
		} catch (IOException ex) {
			throw new IllegalStateException("Unable to initialize reconciliation results CSV", ex);
		}
	}

	@Override
	@Transactional
	public void write(Chunk<? extends TransactionProcessingResultDto> chunk) throws Exception {
		long total = 0;
		long invalid = 0;
		long duplicate = 0;

		List<TransactionRecord> validTransactions = new ArrayList<>();

		Set<String> ids = chunk.getItems().stream().filter(
				result -> result.getStatus() != RecordProcessingStatus.INVALID && result.getTransaction() != null)
				.map(result -> result.getTransaction().getTransactionId()).collect(Collectors.toSet());

		Set<String> existingIds = ids.isEmpty() ? Set.of()
				: transactionRepository.findByTransactionIdInAndJobInstanceId(ids, jobInstanceId).stream()
						.map(TransactionRecord::getTransactionId).collect(Collectors.toSet());

		for (TransactionProcessingResultDto result : chunk) {
			total++;

			if (result.getStatus() == RecordProcessingStatus.INVALID) {
				invalid++;
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
				duplicate++;    
				resultsWriter.write(result);
				continue;
			}

			transaction.setJobInstanceId(jobInstanceId);
			validTransactions.add(transaction);
		}

		if (!validTransactions.isEmpty()) {
			jdbcBatchWriter.write(new Chunk<>(validTransactions));
		}
		auditRepository.incrementCounts(jobInstanceId, total, 0, 0, invalid, duplicate, 0);
	}
}