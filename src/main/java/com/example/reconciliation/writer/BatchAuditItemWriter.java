package com.example.reconciliation.writer;

import java.nio.file.Path;
import java.time.LocalDateTime;

import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.example.reconciliation.entity.BatchAudit;
import com.example.reconciliation.repository.BatchAuditRepository;
import com.example.reconciliation.utility.ProcessingCounters;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class BatchAuditItemWriter implements Tasklet {

	private final BatchAuditRepository auditRepository;
	private final ProcessingCounters counters;

	@Value("${reconciliation.first-file}")
	private String firstFile;

	@Value("${reconciliation.second-file}")
	private String secondFile;

	@Override
	public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
	

		StepExecution stepExecution = chunkContext.getStepContext().getStepExecution();

		JobExecution jobExecution = stepExecution.getJobExecution();

		Long jobInstanceId = jobExecution.getJobInstance().getInstanceId();
		
		

		if (auditRepository.existsByJobInstanceId(jobInstanceId)) {
			return RepeatStatus.FINISHED;
		}
		
		/*
		 * if (1 == 1) { throw new RuntimeException("TEST FAILURE: Final audit step"); }
		 */
		BatchAudit audit = new BatchAudit();

		audit.setJobInstanceId(jobInstanceId);
		audit.setFirstFileName(Path.of(firstFile).getFileName().toString());
		audit.setSecondFileName(Path.of(secondFile).getFileName().toString());

		audit.setTotalFirstFileRecords(counters.getTotalFirstFileRecords());
		audit.setMatchedRecords(counters.getMatchedRecords());
		audit.setMismatchedRecords(counters.getMismatchedRecords());
		audit.setInvalidRecords(counters.getInvalidRecords());
		audit.setDuplicateRecords(counters.getDuplicateRecords());
		audit.setMissingFromFirstFileRecords(counters.getMissingFromFirstFileRecords());

		if (jobExecution.getStartTime() != null) {
			audit.setStartedAt(jobExecution.getStartTime());
		}

		audit.setCompletedAt(LocalDateTime.now());
		audit.setStatus("COMPLETED");

		auditRepository.save(audit);

		return RepeatStatus.FINISHED;
	}
}