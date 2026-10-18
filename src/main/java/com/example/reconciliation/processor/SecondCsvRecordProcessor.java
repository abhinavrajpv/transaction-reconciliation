package com.example.reconciliation.processor;

import org.springframework.batch.core.listener.StepExecutionListener;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;

import com.example.reconciliation.dto.ReconciliationResultDto;
import com.example.reconciliation.dto.SecondCsvRecordDto;
import com.example.reconciliation.service.TransactionReconciliationService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SecondCsvRecordProcessor
		implements ItemProcessor<SecondCsvRecordDto, ReconciliationResultDto>, StepExecutionListener {
	private final TransactionReconciliationService reconciliationService;
	private volatile Long jobInstanceId;

	@Override
	public void beforeStep(StepExecution stepExecution) {
		jobInstanceId = stepExecution.getJobExecution().getJobInstance().getInstanceId();
	}

	@Override
	public ReconciliationResultDto process(SecondCsvRecordDto item) {
		Long currentJobInstanceId = jobInstanceId;
		if (currentJobInstanceId == null) {
			throw new IllegalStateException("Job instance ID has not been initialized");
		}
		return reconciliationService.reconcile(item, currentJobInstanceId);
	}
}