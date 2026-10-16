package com.example.reconciliation.config;

import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.infrastructure.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.infrastructure.item.file.transform.DelimitedLineTokenizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;

import com.example.reconciliation.dto.SecondCsvRecordDto;

@Configuration
public class SecondCsvReaderConfig {

	@Bean
	public FlatFileItemReader<SecondCsvRecordDto> secondCsvReader(
			@Value("${reconciliation.second-file}") String filePath) {

		DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();
		tokenizer.setDelimiter(",");
		tokenizer.setNames("transactionId", "accountNumber", "transactionDate", "amount", "currency", "transactionType",
				"description");

		DefaultLineMapper<SecondCsvRecordDto> lineMapper = new DefaultLineMapper<>();

		lineMapper.setLineTokenizer(tokenizer);

		lineMapper.setFieldSetMapper(fieldSet -> {
			SecondCsvRecordDto dto = new SecondCsvRecordDto();

			dto.setTransactionId(fieldSet.readString("transactionId"));
			dto.setAccountNumber(fieldSet.readString("accountNumber"));
			dto.setTransactionDate(fieldSet.readString("transactionDate"));
			dto.setAmount(fieldSet.readString("amount"));
			dto.setCurrency(fieldSet.readString("currency"));
			dto.setTransactionType(fieldSet.readString("transactionType"));
			dto.setDescription(fieldSet.readString("description"));

			return dto;
		});

		return new FlatFileItemReaderBuilder<SecondCsvRecordDto>().name("secondCsvReader")
				.resource(new FileSystemResource(filePath)).encoding("UTF-8").linesToSkip(1)
				.lineMapper((line, lineNumber) -> {
					SecondCsvRecordDto dto = lineMapper.mapLine(line, lineNumber);

					dto.setRowNumber(lineNumber);
					return dto;
				}).build();
	}
}