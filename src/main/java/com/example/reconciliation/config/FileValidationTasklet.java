package com.example.reconciliation.config;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import com.example.reconciliation.service.FileValidationService;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class FileValidationTasklet implements Tasklet {
	private final FileValidationService validationService;
	private final String filePath;

	@Override
	public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
		String error = validationService.validateFile(filePath);
		if (error != null) {
			contribution.setExitStatus(ExitStatus.FAILED.addExitDescription(error));
		}
		return RepeatStatus.FINISHED;
	}
}