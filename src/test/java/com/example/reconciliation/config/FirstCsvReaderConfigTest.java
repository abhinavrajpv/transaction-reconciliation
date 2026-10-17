package com.example.reconciliation.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;

import com.example.reconciliation.dto.FirstCsvRecordDto;

public class FirstCsvReaderConfigTest {

	@TempDir
	Path tempDir;

	private static final String HEADER = "transactionId,accountNumber,transactionDate,amount,currency,transactionType,description";

	private FlatFileItemReader<FirstCsvRecordDto> createReader(Path file) throws Exception {
		return new FirstCsvReaderConfig().firstCsvReader(file.toString());
	}

	@Test
	void shouldReadAndMapAllCsvFields() throws Exception {
		Path file = tempDir.resolve("first.csv");

		Files.writeString(file, HEADER + "\n" + "TXN001,ACC12345,2026-10-01,100.50,INR,CREDIT,Salary");

		FlatFileItemReader<FirstCsvRecordDto> reader = createReader(file);

		try {
			reader.open(new ExecutionContext());

			FirstCsvRecordDto record = reader.read();

			assertNotNull(record);
			assertEquals("TXN001", record.getTransactionId());
			assertEquals("ACC12345", record.getAccountNumber());
			assertEquals("2026-10-01", record.getTransactionDate());
			assertEquals("100.50", record.getAmount());
			assertEquals("INR", record.getCurrency());
			assertEquals("CREDIT", record.getTransactionType());
			assertEquals("Salary", record.getDescription());
		} finally {
			reader.close();
		}
	}

	@Test
	void shouldSkipHeaderAndAssignCorrectRowNumber() throws Exception {
		Path file = tempDir.resolve("first.csv");

		Files.writeString(file, HEADER + "\n" + "TXN001,ACC12345,2026-10-01,100,INR,CREDIT,Salary\n"
				+ "TXN002,ACC67890,2026-10-02,200,USD,DEBIT,Shopping");

		FlatFileItemReader<FirstCsvRecordDto> reader = createReader(file);

		try {
			reader.open(new ExecutionContext());

			FirstCsvRecordDto first = reader.read();
			FirstCsvRecordDto second = reader.read();

			assertNotNull(first);
			assertNotNull(second);
			assertEquals(2, first.getRowNumber());
			assertEquals(3, second.getRowNumber());
			assertEquals("TXN002", second.getTransactionId());
		} finally {
			reader.close();
		}
	}

	@Test
	void shouldReturnNullWhenNoDataRowsExist() throws Exception {
		Path file = tempDir.resolve("first.csv");
		Files.writeString(file, HEADER);

		FlatFileItemReader<FirstCsvRecordDto> reader = createReader(file);

		try {
			reader.open(new ExecutionContext());

			assertNull(reader.read());
		} finally {
			reader.close();
		}
	}
}