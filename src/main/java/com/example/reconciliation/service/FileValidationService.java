package com.example.reconciliation.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class FileValidationService {

	@Value("${reconciliation.max-file-size}")
	private long maxFileSize;

	@Value("${reconciliation.first-file.headers}")
	private String expectedHeaders;

	public String validateFile(String filePath) {

		Path path = Path.of(filePath);

		if (!Files.exists(path)) {
			return "File does not exist";
		}

		if (!Files.isRegularFile(path)) {
			return "Path is not a regular file";
		}

		if (!Files.isReadable(path)) {
			return "File is not readable";
		}

		if (!path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".csv")) {
			return "File must have a .csv extension";
		}

		try {
			long size = Files.size(path);

			if (size > maxFileSize) {
				return "File exceeds the configured size limit";
			}

			if (size == 0) {
				return "File is empty";
			}

			try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {

				String header = reader.readLine();

				if (header == null || header.isBlank()) {
					return "CSV header is missing";
				}

				header = header.replace("\uFEFF", "").trim();

				List<String> actualHeaders = Arrays.stream(header.split(",", -1)).map(String::trim).toList();

				List<String> expected = Arrays.stream(expectedHeaders.split(",", -1)).map(String::trim).toList();

				if (!actualHeaders.equals(expected)) {
					return "CSV headers are missing, incorrect, or in the wrong order";
				}

				String firstRecord;

				do {
					firstRecord = reader.readLine();
				} while (firstRecord != null && firstRecord.isBlank());

				if (firstRecord == null) {
					return "CSV contains no data records";
				}
			}

		} catch (IOException ex) {
			return "Unable to read file: " + ex.getMessage();
		}

		return null;
	}
}