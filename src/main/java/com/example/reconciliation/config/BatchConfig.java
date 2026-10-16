package com.example.reconciliation.config;

import java.util.concurrent.Future;

import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;
import org.springframework.batch.integration.async.AsyncItemProcessor;
import org.springframework.batch.integration.async.AsyncItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import com.example.reconciliation.dto.FirstCsvRecordDto;
import com.example.reconciliation.dto.ReconciliationResultDto;
import com.example.reconciliation.dto.SecondCsvRecordDto;
import com.example.reconciliation.dto.TransactionProcessingResultDto;
import com.example.reconciliation.processor.SecondCsvRecordProcessor;
import com.example.reconciliation.service.FileValidationService;
import com.example.reconciliation.writer.BatchAuditItemWriter;
import com.example.reconciliation.writer.FirstCsvItemWriter;

@Configuration
public class BatchConfig {

	@Bean
	public FileValidationTasklet firstFileValidationTasklet(FileValidationService validationService,
			@Value("${reconciliation.first-file}") String filePath) {

		return new FileValidationTasklet(validationService, filePath);
	}

	@Bean
	public FileValidationTasklet secondFileValidationTasklet(FileValidationService validationService,
			@Value("${reconciliation.second-file}") String filePath) {

		return new FileValidationTasklet(validationService, filePath);
	}

	@Bean
	public Step validateFirstFileStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
			FileValidationTasklet firstFileValidationTasklet) {

		return new StepBuilder("validateFirstFileStep", jobRepository)
				.tasklet(firstFileValidationTasklet, transactionManager).build();
	}

	@Bean
	public Step ingestFirstFileStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
			FlatFileItemReader<FirstCsvRecordDto> firstCsvReader,
			AsyncItemProcessor<FirstCsvRecordDto, TransactionProcessingResultDto> asyncFirstCsvProcessor,
			AsyncItemWriter<TransactionProcessingResultDto> asyncFirstCsvWriter, FirstCsvItemWriter firstCsvItemWriter,
			@Value("${reconciliation.chunk-size}") int chunkSize) {

		return new StepBuilder("ingestFirstFileStep", jobRepository)
				.<FirstCsvRecordDto, Future<TransactionProcessingResultDto>>chunk(chunkSize)
				.transactionManager(transactionManager).reader(firstCsvReader).processor(asyncFirstCsvProcessor)
				.writer(asyncFirstCsvWriter).listener(firstCsvItemWriter).build();
	}

	@Bean
	public Step validateSecondFileStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
			FileValidationTasklet secondFileValidationTasklet) {

		return new StepBuilder("validateSecondFileStep", jobRepository)
				.tasklet(secondFileValidationTasklet, transactionManager).build();
	}

	@Bean
	public Step reconcileSecondFileStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
			FlatFileItemReader<SecondCsvRecordDto> secondCsvReader,
			AsyncItemProcessor<SecondCsvRecordDto, ReconciliationResultDto> asyncSecondCsvProcessor,
			AsyncItemWriter<ReconciliationResultDto> asyncSecondCsvWriter,SecondCsvRecordProcessor secondCsvRecordProcessor,
			@Value("${reconciliation.chunk-size}") int chunkSize) {

		return new StepBuilder("reconcileSecondFileStep", jobRepository)
				.<SecondCsvRecordDto, Future<ReconciliationResultDto>>chunk(chunkSize)
				.transactionManager(transactionManager).reader(secondCsvReader).processor(asyncSecondCsvProcessor)
				.writer(asyncSecondCsvWriter).listener(secondCsvRecordProcessor).build();
	}

	@Bean
	public Step finalizeResultsAndAuditStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
			BatchAuditItemWriter batchAuditItemWriter) {

		return new StepBuilder("finalizeResultsAndAuditStep", jobRepository)
				.tasklet(batchAuditItemWriter, transactionManager).build();
	}

}