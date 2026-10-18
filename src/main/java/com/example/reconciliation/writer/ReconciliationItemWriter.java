package com.example.reconciliation.writer;

import java.io.IOException;
import java.math.BigDecimal;

import org.springframework.batch.core.listener.StepExecutionListener;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.reconciliation.dto.ReconciliationResultDto;
import com.example.reconciliation.entity.TransactionRecord;
import com.example.reconciliation.repository.BatchAuditRepository;
import com.example.reconciliation.repository.TransactionRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ReconciliationItemWriter implements ItemWriter<ReconciliationResultDto>, StepExecutionListener {

	private final TransactionRepository transactionRepository;
	private final ReconciliationResultsCsvWriter resultsWriter;
	private final BatchAuditRepository auditRepository;
	private Long jobInstanceId;
	private Long jobExecutionId;

	@Override
	public void beforeStep(StepExecution stepExecution) {

		jobInstanceId = stepExecution.getJobExecution().getJobInstance().getInstanceId();

		jobExecutionId = stepExecution.getJobExecution().getId();

		try {
			resultsWriter.initialize(jobExecutionId, jobInstanceId);
		} catch (IOException ex) {
			throw new IllegalStateException("Unable to initialize reconciliation results CSV", ex);
		}
	}

	@Override
	public void write(Chunk<? extends ReconciliationResultDto> chunk) throws IOException {
		long matched = 0;
		long mismatched = 0;
		long missingFromDatabase = 0;

		for (ReconciliationResultDto result : chunk) {
			/*
			 * if ("TXN001078".equals(result.getTransactionId())) { throw new
			 * RuntimeException( "TEST FAILURE: Step 4 for TXN1006"); }
			 */	 
			switch (result.getStatus()) {
			case MATCHED ->matched++;
			  
			case MISMATCHED -> {
				TransactionRecord transaction = result.getExistingTransaction();

				BigDecimal sourceAmount = result.getActualAmount();
				transaction.setAmount(sourceAmount);

				transactionRepository.save(transaction);

				mismatched++;
				resultsWriter.write(result);
			}

			case MISSING_FROM_DATABASE -> {
				resultsWriter.write(result);
				missingFromDatabase++;
			}
			default -> throw new IllegalStateException("Unexpected reconciliation status: " + result.getStatus());
			}
		}
		auditRepository.incrementCounts(jobInstanceId, 0, matched, mismatched, 0, 0, missingFromDatabase);
	}
}