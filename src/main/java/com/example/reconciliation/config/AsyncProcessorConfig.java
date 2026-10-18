package com.example.reconciliation.config;

import org.springframework.batch.integration.async.AsyncItemProcessor;
import org.springframework.batch.integration.async.AsyncItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.example.reconciliation.dto.FirstCsvRecordDto;
import com.example.reconciliation.dto.ReconciliationResultDto;
import com.example.reconciliation.dto.SecondCsvRecordDto;
import com.example.reconciliation.dto.TransactionProcessingResultDto;
import com.example.reconciliation.processor.FirstCsvRecordProcessor;
import com.example.reconciliation.processor.SecondCsvRecordProcessor;
import com.example.reconciliation.writer.FirstCsvItemWriter;
import com.example.reconciliation.writer.ReconciliationItemWriter;

@Configuration
public class AsyncProcessorConfig {

	@Bean
	AsyncItemProcessor<FirstCsvRecordDto, TransactionProcessingResultDto> asyncFirstCsvProcessor(
			FirstCsvRecordProcessor delegate, ThreadPoolTaskExecutor firstCsvTaskExecutor) {
		AsyncItemProcessor<FirstCsvRecordDto, TransactionProcessingResultDto> processor = new AsyncItemProcessor<>(
				delegate);
		processor.setTaskExecutor(firstCsvTaskExecutor);
		return processor;
	}

	@Bean
	AsyncItemWriter<TransactionProcessingResultDto> asyncFirstCsvWriter(FirstCsvItemWriter delegate) {
		return new AsyncItemWriter<>(delegate);
	}

	@Bean
	AsyncItemProcessor<SecondCsvRecordDto, ReconciliationResultDto> asyncSecondCsvProcessor(
			SecondCsvRecordProcessor delegate, ThreadPoolTaskExecutor secondCsvTaskExecutor) {
		AsyncItemProcessor<SecondCsvRecordDto, ReconciliationResultDto> processor = new AsyncItemProcessor<>(delegate);
		processor.setTaskExecutor(secondCsvTaskExecutor);
		return processor;
	}

	@Bean
	AsyncItemWriter<ReconciliationResultDto> asyncSecondCsvWriter(ReconciliationItemWriter delegate) {
		return new AsyncItemWriter<>(delegate);
	}  
}