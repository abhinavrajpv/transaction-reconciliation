package com.example.reconciliation.job;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.example.reconciliation.listener.BatchAuditJobListener;

@Configuration
public class JobConfig {

	@Bean
	public Job transactionReconciliationJob(JobRepository jobRepository, Step validateFirstFileStep,
			Step ingestFirstFileStep, Step validateSecondFileStep, Step reconcileSecondFileStep,
			BatchAuditJobListener auditJobListener) {

		return new JobBuilder("transactionReconciliationJob", jobRepository).listener(auditJobListener).start(validateFirstFileStep).on("FAILED")
				.fail().from(validateFirstFileStep).on("*").to(ingestFirstFileStep).from(ingestFirstFileStep)
				.next(validateSecondFileStep).on("FAILED").fail().from(validateSecondFileStep).on("*")
				.to(reconcileSecondFileStep).end().build();
	}
}