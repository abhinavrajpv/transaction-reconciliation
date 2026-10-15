package com.example.reconciliation.config;

import org.springframework.batch.integration.async.AsyncItemProcessor;
import org.springframework.batch.integration.async.AsyncItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.example.reconciliation.dto.FirstCsvRecordDto;
import com.example.reconciliation.dto.TransactionProcessingResultDto;
import com.example.reconciliation.processor.FirstCsvRecordProcessor;
import com.example.reconciliation.writer.FirstCsvItemWriter;

@Configuration
public class AsyncProcessorConfig {

	@Bean
	public AsyncItemProcessor<FirstCsvRecordDto, TransactionProcessingResultDto> asyncFirstCsvProcessor(
			FirstCsvRecordProcessor delegate, ThreadPoolTaskExecutor processorTaskExecutor) {

		AsyncItemProcessor<FirstCsvRecordDto, TransactionProcessingResultDto> processor = new AsyncItemProcessor<>(
				delegate);

		processor.setTaskExecutor(processorTaskExecutor);

		return processor;
	}

	@Bean
	public AsyncItemWriter<TransactionProcessingResultDto> asyncFirstCsvWriter(FirstCsvItemWriter delegate) {

		return new AsyncItemWriter<>(delegate);
	}
}