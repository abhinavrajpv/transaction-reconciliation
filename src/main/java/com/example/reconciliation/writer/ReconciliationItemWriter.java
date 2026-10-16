package com.example.reconciliation.writer;

import java.io.IOException;
import java.math.BigDecimal;

import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.reconciliation.dto.ReconciliationResultDto;
import com.example.reconciliation.entity.TransactionRecord;
import com.example.reconciliation.repository.TransactionRepository;
import com.example.reconciliation.utility.ProcessingCounters;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ReconciliationItemWriter implements ItemWriter<ReconciliationResultDto> {

	private final TransactionRepository transactionRepository;
	private final ReconciliationResultsCsvWriter resultsWriter;
	private final ProcessingCounters counters;

	@Override
	@Transactional
	public void write(Chunk<? extends ReconciliationResultDto> chunk) throws IOException {

		for (ReconciliationResultDto result : chunk) {  
			/*
			 * if ("TXN001078".equals(result.getTransactionId())) { throw new
			 * RuntimeException( "TEST FAILURE: Step 4 for TXN1006"); }
			 */

			switch (result.getStatus()) {
			case "MATCHED" -> counters.recordMatched();

			case "MISMATCHED" -> {
				TransactionRecord transaction = result.getExistingTransaction();

				BigDecimal sourceAmount = result.getActualAmount();
				transaction.setAmount(sourceAmount);

				transactionRepository.save(transaction);

				counters.recordMismatched();
				resultsWriter.write(result);
			}

			case "MISSING_FROM_DATABASE" -> {resultsWriter.write(result);
			counters.recordMissingFromFirstFile();
			}
			default -> throw new IllegalStateException("Unexpected reconciliation status: " + result.getStatus());
			}
		}
	}
}