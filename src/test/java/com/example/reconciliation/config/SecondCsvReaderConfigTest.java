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

import com.example.reconciliation.dto.SecondCsvRecordDto;

public class SecondCsvReaderConfigTest {

	@TempDir
	Path tempDir;

	private static final String HEADER = "transactionId,accountNumber,transactionDate,amount,currency,transactionType,description";

	private FlatFileItemReader<SecondCsvRecordDto> createReader(Path file) throws Exception {
		return new SecondCsvReaderConfig().secondCsvReader(file.toString());
	}

	@Test
	void shouldReadAndMapAllCsvFields() throws Exception {
		Path file = tempDir.resolve("second.csv");

		Files.writeString(file, HEADER + "\n" + "TXN101,ACC12345,2026-10-05,250.75,INR,DEBIT,Purchase");

		FlatFileItemReader<SecondCsvRecordDto> reader = createReader(file);

		try {
			reader.open(new ExecutionContext());

			SecondCsvRecordDto record = reader.read();

			assertNotNull(record);
			assertEquals("TXN101", record.getTransactionId());
			assertEquals("ACC12345", record.getAccountNumber());
			assertEquals("2026-10-05", record.getTransactionDate());
			assertEquals("250.75", record.getAmount());
			assertEquals("INR", record.getCurrency());
			assertEquals("DEBIT", record.getTransactionType());
			assertEquals("Purchase", record.getDescription());
		} finally {
			reader.close();
		}
	}

	@Test
	void shouldSkipHeaderAndAssignCorrectRowNumber() throws Exception {
		Path file = tempDir.resolve("second.csv");

		Files.writeString(file, HEADER + "\n" + "TXN101,ACC12345,2026-10-05,250,INR,DEBIT,Purchase\n"
				+ "TXN102,ACC67890,2026-10-06,300,USD,CREDIT,Refund");

		FlatFileItemReader<SecondCsvRecordDto> reader = createReader(file);

		try {
			reader.open(new ExecutionContext());

			SecondCsvRecordDto first = reader.read();
			SecondCsvRecordDto second = reader.read();

			assertNotNull(first);
			assertNotNull(second);
			assertEquals(2, first.getRowNumber());
			assertEquals(3, second.getRowNumber());
			assertEquals("TXN102", second.getTransactionId());
		} finally {
			reader.close();
		}
	}

	@Test
	void shouldReturnNullWhenNoDataRowsExist() throws Exception {
		Path file = tempDir.resolve("second.csv");
		Files.writeString(file, HEADER);

		FlatFileItemReader<SecondCsvRecordDto> reader = createReader(file);

		try {
			reader.open(new ExecutionContext());

			assertNull(reader.read());
		} finally {
			reader.close();
		}
	}
}