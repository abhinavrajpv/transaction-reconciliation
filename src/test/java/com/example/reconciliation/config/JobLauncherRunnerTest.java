
package com.example.reconciliation.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class JobLauncherRunnerTest {

	@Mock
	private JobOperator jobOperator;

	@Mock
	private Job reconciliationJob;

	private JobLauncherRunner runner;

	private static final String FIRST_FILE = "C:/batch/input/input_transactions.csv";

	private static final String SECOND_FILE = "C:/batch/input/latest_transactions.csv";

	@BeforeEach
	void setUp() {
		runner = new JobLauncherRunner(jobOperator, reconciliationJob);

		ReflectionTestUtils.setField(runner, "firstFile", FIRST_FILE);
		ReflectionTestUtils.setField(runner, "secondFile", SECOND_FILE);
	}

	@Test
	void shouldRunJobWithExpectedParameters() throws Exception {
		runner.run(new String[0]);

		ArgumentCaptor<JobParameters> parametersCaptor = ArgumentCaptor.forClass(JobParameters.class);

		verify(jobOperator).run(ArgumentMatchers.eq(reconciliationJob), parametersCaptor.capture());

		JobParameters parameters = parametersCaptor.getValue();

		assertEquals(FIRST_FILE, parameters.getString("firstFile"));

		assertEquals(SECOND_FILE, parameters.getString("secondFile"));
	}

	@Test
	void shouldPropagateExceptionWhenJobRunFails() throws Exception {
		when(jobOperator.run(ArgumentMatchers.eq(reconciliationJob), ArgumentMatchers.any(JobParameters.class)))    
				.thenThrow(new IllegalStateException("Job launch failed"));

		assertThrows(IllegalStateException.class, () -> runner.run(new String[0]));
	}
}
