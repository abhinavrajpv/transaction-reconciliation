package com.example.reconciliation.config;

import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;

import com.example.reconciliation.dto.FirstCsvRecordDto;

@Configuration
public class FirstCsvReaderConfig {

	@Bean
	public FlatFileItemReader<FirstCsvRecordDto> firstCsvReader(
			@Value("${reconciliation.first-file}") String filePath) {

		return new FlatFileItemReaderBuilder<FirstCsvRecordDto>().name("firstCsvReader")
				.resource(new FileSystemResource(filePath)).encoding("UTF-8").linesToSkip(1).delimited().delimiter(",")
				.names("transactionId", "accountNumber", "transactionDate", "amount", "currency", "transactionType",
						"description")
				.targetType(FirstCsvRecordDto.class).build();
	}
}