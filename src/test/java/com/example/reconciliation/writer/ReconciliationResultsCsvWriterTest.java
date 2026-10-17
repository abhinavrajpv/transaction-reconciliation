
package com.example.reconciliation.writer;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.example.reconciliation.dto.FirstCsvRecordDto;
import com.example.reconciliation.dto.ReconciliationResultDto;
import com.example.reconciliation.dto.TransactionProcessingResultDto;
import com.example.reconciliation.enums.RecordProcessingStatus;

public class ReconciliationResultsCsvWriterTest {

	@TempDir
	Path tempDir;

	private Path output;
	private ReconciliationResultsCsvWriter writer;

	@BeforeEach
	void setUp() {
		output = tempDir.resolve("results.csv");
		writer = new ReconciliationResultsCsvWriter(output.toString());
	}

	@Test
	void shouldCreateFileAndHeaderDuringInitialization() throws Exception {
		writer.initialize(101L, 22L);

		assertTrue(Files.exists(output));

		String content = Files.readString(output);

		assertTrue(content.startsWith(
				"job_execution_id,job_instance_id,row_number,transaction_id,status,action,message,expected_amount,actual_amount"));

		assertEquals(101L, writer.getJobExecutionId());
		assertEquals(22L, writer.getJobInstanceId());
	}

	@Test
	void shouldCreateParentDirectories() throws Exception {
		Path nestedOutput = tempDir.resolve("nested/folder/results.csv");
		var nestedWriter = new ReconciliationResultsCsvWriter(nestedOutput.toString());

		nestedWriter.initialize(1L, 2L);

		assertTrue(Files.exists(nestedOutput));
		assertTrue(Files.isDirectory(nestedOutput.getParent()));
	}

	@Test
	void shouldNotWriteAnotherHeaderForExistingNonemptyFile() throws Exception {
		Files.writeString(output, "existing-header\nexisting-row\n");

		writer.initialize(101L, 22L);

		assertEquals("existing-header\nexisting-row\n", Files.readString(output));
		assertEquals(101L, writer.getJobExecutionId());
		assertEquals(22L, writer.getJobInstanceId());
	}

	@Test
	void shouldWriteInvalidTransactionProcessingResult() throws Exception {
		writer.initialize(101L, 22L);

		FirstCsvRecordDto original = new FirstCsvRecordDto();
		original.setRowNumber(3);
		original.setTransactionId("TXN001");

		TransactionProcessingResultDto result = new TransactionProcessingResultDto();
		result.setOriginalRecord(original);
		result.setStatus(RecordProcessingStatus.INVALID);
		result.setMessage("Invalid amount");

		writer.write(result);

		String[] lines = Files.readString(output).split("\\R");

		assertEquals(2, lines.length);
		assertTrue(lines[1].startsWith("101,22,3,TXN001,INVALID,REJECTED,Invalid amount"));
	}

	@Test
	void shouldWriteDuplicateTransactionProcessingResult() throws Exception {
		writer.initialize(101L, 22L);

		FirstCsvRecordDto original = new FirstCsvRecordDto();
		original.setRowNumber(4);
		original.setTransactionId("TXN002");

		TransactionProcessingResultDto result = new TransactionProcessingResultDto();
		result.setOriginalRecord(original);
		result.setStatus(RecordProcessingStatus.DUPLICATE);
		result.setMessage("Already exists");

		writer.write(result);

		String content = Files.readString(output);

		assertTrue(content.contains("101,22,4,TXN002,DUPLICATE,DUPLICATE,Already exists"));
	}

	@Test
	void shouldWriteReconciliationResultWithAmounts() throws Exception {
		writer.initialize(101L, 22L);

		ReconciliationResultDto result = new ReconciliationResultDto();
		result.setRowNumber(8);
		result.setTransactionId("TXN003");
		result.setStatus("MISMATCHED");
		result.setAction("DATABASE_UPDATED");
		result.setMessage("Amounts differ");
		result.setExpectedAmount(new BigDecimal("100.00"));
		result.setActualAmount(new BigDecimal("125.50"));

		writer.write(result);

		String content = Files.readString(output);

		assertTrue(content.contains("101,22,8,TXN003,MISMATCHED,DATABASE_UPDATED,Amounts differ,100.00,125.50"));
	}

	@Test
	void shouldAppendMultipleRecordsWithoutReplacingEarlierRecords() throws Exception {
		writer.initialize(101L, 22L);

		ReconciliationResultDto first = new ReconciliationResultDto();
		first.setRowNumber(2);
		first.setTransactionId("TXN001");
		first.setStatus("MISSING_FROM_DATABASE");
		first.setAction("NOT_INSERTED");
		first.setMessage("Not found");

		ReconciliationResultDto second = new ReconciliationResultDto();
		second.setRowNumber(3);
		second.setTransactionId("TXN002");
		second.setStatus("MISMATCHED");
		second.setAction("DATABASE_UPDATED");
		second.setMessage("Different amount");

		writer.write(first);
		writer.write(second);

		String[] lines = Files.readString(output).split("\\R");

		assertEquals(3, lines.length);
		assertTrue(lines[1].contains("TXN001"));
		assertTrue(lines[2].contains("TXN002"));
	}

	@Test
	void shouldUpdateStoredIdsEvenWhenOutputAlreadyExists() throws Exception {
		Files.writeString(output, "existing content\n");

		writer.initialize(200L, 300L);

		assertEquals(200L, writer.getJobExecutionId());
		assertEquals(300L, writer.getJobInstanceId());
		assertEquals("existing content\n", Files.readString(output));
	}
}
