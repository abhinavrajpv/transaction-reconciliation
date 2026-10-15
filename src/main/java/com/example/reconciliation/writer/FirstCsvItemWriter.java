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

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Component
public class FirstCsvItemWriter implements ItemWriter<TransactionProcessingResultDto>, StepExecutionListener {

	private final TransactionRepository transactionRepository;
	private final JdbcBatchItemWriter<TransactionRecord> jdbcBatchWriter;
	private final ReconciliationResultsCsvWriter resultsWriter;

	private final Set<String> seenTransactionIds = new HashSet<>();

	@Override
	public void beforeStep(StepExecution stepExecution) {
		seenTransactionIds.clear();

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
				: transactionRepository.findByTransactionIdIn(ids).stream().map(TransactionRecord::getTransactionId)
						.collect(Collectors.toSet());

		for (TransactionProcessingResultDto result : chunk) {

			if (result.getStatus() == RecordProcessingStatus.INVALID) {
				resultsWriter.write(result);
				continue;
			}

			TransactionRecord transaction = result.getTransaction();
			String transactionId = transaction.getTransactionId();

			if (!seenTransactionIds.add(transactionId) || existingIds.contains(transactionId)) {

				result.setStatus(RecordProcessingStatus.DUPLICATE);
				result.setMessage("Transaction ID already exists");
				resultsWriter.write(result);
				continue;
			}

			validTransactions.add(transaction);
		}

		if (!validTransactions.isEmpty()) {
			jdbcBatchWriter.write(new Chunk<>(validTransactions));
		}
	}
}