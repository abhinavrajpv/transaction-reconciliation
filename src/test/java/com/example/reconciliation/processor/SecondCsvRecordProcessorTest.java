
package com.example.reconciliation.processor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.step.StepExecution;

import com.example.reconciliation.dto.ReconciliationResultDto;
import com.example.reconciliation.dto.SecondCsvRecordDto;
import com.example.reconciliation.service.TransactionReconciliationService;

@ExtendWith(MockitoExtension.class)
public class SecondCsvRecordProcessorTest {

	@Mock
	private TransactionReconciliationService reconciliationService;

	private SecondCsvRecordProcessor processor;

	@BeforeEach
	void setUp() {
		processor = new SecondCsvRecordProcessor(reconciliationService);
	}

	@Test
	void shouldRejectProcessingBeforeStepInitialization() {
		var record = new SecondCsvRecordDto();
		record.setTransactionId("TXN001");

		assertThrows(IllegalStateException.class, () -> processor.process(record));

		verifyNoInteractions(reconciliationService);
	}

	@Test
	void shouldInitializeJobInstanceIdAndDelegate() {
		StepExecution stepExecution = mock(StepExecution.class);
		JobExecution jobExecution = mock(JobExecution.class);
		JobInstance jobInstance = mock(JobInstance.class);

		when(stepExecution.getJobExecution()).thenReturn(jobExecution);
		when(jobExecution.getJobInstance()).thenReturn(jobInstance);
		when(jobInstance.getInstanceId()).thenReturn(42L);

		processor.beforeStep(stepExecution);

		var input = new SecondCsvRecordDto();
		input.setTransactionId("TXN001");

		var expected = new ReconciliationResultDto();
		expected.setTransactionId("TXN001");
		expected.setStatus("MATCHED");

		when(reconciliationService.reconcile(input, 42L)).thenReturn(expected);

		assertSame(expected, processor.process(input));

		verify(reconciliationService).reconcile(input, 42L);
	}
}
