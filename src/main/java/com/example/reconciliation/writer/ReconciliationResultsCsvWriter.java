package com.example.reconciliation.writer;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.example.reconciliation.dto.ReconciliationResultDto;
import com.example.reconciliation.dto.TransactionProcessingResultDto;

import lombok.Getter;

@Getter
@Component
public class ReconciliationResultsCsvWriter {

	private final Path outputPath;
	private Long jobExecutionId;
	private Long jobInstanceId;

	public ReconciliationResultsCsvWriter(@Value("${reconciliation.results-file}") String filePath) {
		this.outputPath = Path.of(filePath);
	}

	public synchronized void initialize(Long jobExecutionId, Long jobInstanceId) throws IOException {

		this.jobExecutionId = jobExecutionId;
		this.jobInstanceId = jobInstanceId;

		Path parent = outputPath.getParent();

		if (parent != null) {
			Files.createDirectories(parent);
		}

		if (Files.exists(outputPath) && Files.size(outputPath) > 0) {
			return;
		}

		try (BufferedWriter writer = Files.newBufferedWriter(outputPath, StandardCharsets.UTF_8,
				StandardOpenOption.CREATE, StandardOpenOption.WRITE);
				CSVPrinter csvPrinter = new CSVPrinter(writer,
						CSVFormat.DEFAULT.builder().setHeader("job_execution_id", "job_instance_id", "row_number",
								"transaction_id", "status", "action", "message", "expected_amount", "actual_amount")
								.get())) {
		}
	}

	public synchronized void write(TransactionProcessingResultDto result) throws IOException {

		var record = result.getOriginalRecord();

		try (BufferedWriter writer = Files.newBufferedWriter(outputPath, StandardCharsets.UTF_8,
				StandardOpenOption.CREATE, StandardOpenOption.APPEND);
				CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT)) {

			csvPrinter.printRecord(jobExecutionId, jobInstanceId, result.getOriginalRecord().getRowNumber(),
					record.getTransactionId(), result.getStatus().name(),
					result.getStatus().name().equals("INVALID") ? "REJECTED" : "DUPLICATE", result.getMessage(), "",
					"");
		}
	}

	public synchronized void write(ReconciliationResultDto result) throws IOException {

		try (BufferedWriter writer = Files.newBufferedWriter(outputPath, StandardCharsets.UTF_8,
				StandardOpenOption.CREATE, StandardOpenOption.APPEND);
				CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT)) {

			printer.printRecord(jobExecutionId, jobInstanceId, result.getRowNumber(), result.getTransactionId(),
					result.getStatus().name(), result.getAction().name(), result.getMessage(), result.getExpectedAmount(),
					result.getActualAmount());
		}
	}
}