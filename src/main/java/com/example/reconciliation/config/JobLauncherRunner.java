package com.example.reconciliation.config;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.batch.launch-on-startup", havingValue = "true", matchIfMissing = true)
public class JobLauncherRunner implements CommandLineRunner {

	private final JobOperator jobOperator;
	private final Job reconciliationJob;

	public JobLauncherRunner(JobOperator jobOperator, Job reconciliationJob) {
		this.jobOperator = jobOperator;
		this.reconciliationJob = reconciliationJob;
	}

	@Value("${reconciliation.first-file}")
	private String firstFile;

	@Value("${reconciliation.second-file}")
	private String secondFile;

	@Override
	public void run(String... args) throws Exception {

		JobParameters parameters = new JobParametersBuilder().addString("firstFile", firstFile) 
				.addString("secondFile", secondFile).toJobParameters();

		jobOperator.run(reconciliationJob, parameters);
	}
}