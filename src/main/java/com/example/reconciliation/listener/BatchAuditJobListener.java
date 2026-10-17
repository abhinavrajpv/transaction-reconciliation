package com.example.reconciliation.listener;

import java.nio.file.Path;
import java.time.LocalDateTime;

import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.reconciliation.entity.BatchAudit;
import com.example.reconciliation.repository.BatchAuditRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class BatchAuditJobListener implements JobExecutionListener {

	private final BatchAuditRepository auditRepository;
	@Value("${reconciliation.first-file}")
	private String firstFile;

	@Value("${reconciliation.second-file}")
	private String secondFile;

	@Override
	@Transactional
	public void beforeJob(JobExecution jobExecution) {

		Long jobInstanceId = jobExecution.getJobInstance().getInstanceId();

		if (auditRepository.existsByJobInstanceId(jobInstanceId)) {
			return;
		}

		BatchAudit audit = new BatchAudit();

		audit.setJobInstanceId(jobInstanceId);
		audit.setFirstFileName(Path.of(firstFile).getFileName().toString());
		audit.setSecondFileName(Path.of(secondFile).getFileName().toString());

		audit.setTotalFirstFileRecords(0);
		audit.setMatchedRecords(0);
		audit.setMismatchedRecords(0);
		audit.setInvalidRecords(0);
		audit.setDuplicateRecords(0);
		audit.setMissingFromFirstFileRecords(0);

		audit.setStartedAt(jobExecution.getStartTime() != null ? jobExecution.getStartTime() : LocalDateTime.now());

		audit.setStatus("STARTED");

		auditRepository.save(audit);
	}

	@Override
	@Transactional
	public void afterJob(JobExecution jobExecution) {

		Long jobInstanceId = jobExecution.getJobInstance().getInstanceId();

		auditRepository.findByJobInstanceId(jobInstanceId).ifPresent(audit -> {
			audit.setStatus(jobExecution.getStatus().name());

			if (jobExecution.getEndTime() != null) {
				audit.setCompletedAt(jobExecution.getEndTime());
			}

			auditRepository.save(audit);
		});
	}
}