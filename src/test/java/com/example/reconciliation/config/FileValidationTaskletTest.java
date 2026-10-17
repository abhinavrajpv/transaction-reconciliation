
package com.example.reconciliation.config;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;

import com.example.reconciliation.service.FileValidationService;

@ExtendWith(MockitoExtension.class)
public class FileValidationTaskletTest {

	@Mock
	private FileValidationService validationService;

	@Mock
	private StepContribution contribution;

	@Mock
	private ChunkContext chunkContext;

	private FileValidationTasklet tasklet;

	@BeforeEach
	void setUp() {
		tasklet = new FileValidationTasklet(validationService, "input.csv");
	}

	@Test
	void shouldFinishWhenFileIsValid() throws Exception {
		when(validationService.validateFile("input.csv")).thenReturn(null);

		RepeatStatus status = tasklet.execute(contribution, chunkContext);

		assertEquals(RepeatStatus.FINISHED, status);
		verify(validationService).validateFile("input.csv");
		verify(contribution, never()).setExitStatus(any());
	}

	@Test
	void shouldSetFailedExitStatusWhenValidationFails() throws Exception {
		when(validationService.validateFile("input.csv")).thenReturn("File does not exist");

		RepeatStatus status = tasklet.execute(contribution, chunkContext);

		assertEquals(RepeatStatus.FINISHED, status);
		verify(contribution).setExitStatus(argThat(exitStatus -> exitStatus.getExitCode().equals("FAILED")
				&& exitStatus.getExitDescription().contains("File does not exist")));
	}
}
