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

import com.example.reconciliation.constants.FileValidationMessages;

@Service
public class FileValidationService {

	@Value("${reconciliation.max-file-size}")
	private long maxFileSize;

	@Value("${reconciliation.first-file.headers}")
	private String firstfileHeaders;

	public String validateFile(String filePath) {
		Path path = Path.of(filePath);
		if (!Files.exists(path)) {
			return FileValidationMessages.FILE_DOES_NOT_EXIST;
		}
		if (!Files.isRegularFile(path)) {
			return FileValidationMessages.PATH_NOT_REGULAR_FILE;
		}
		if (!Files.isReadable(path)) {
			return FileValidationMessages.FILE_NOT_READABLE;
		}
		if (!path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".csv")) {
			return FileValidationMessages.INVALID_FILE_EXTENSION;
		}
		try {
			long size = Files.size(path);
			if (size > maxFileSize) {
				return FileValidationMessages.FILE_SIZE_EXCEEDED;
			}
			if (size == 0) {
				return FileValidationMessages.FILE_EMPTY;
			}
			try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
				String header = reader.readLine();
				/*
				 * if (header == null) { return "File is empty"; }
				 */
				if (header.isBlank()) {
					return FileValidationMessages.CSV_HEADER_MISSING;
				}
				header = header.replace("\uFEFF", "").trim();
				List<String> actualHeaders = Arrays.stream(header.split(",", -1)).map(String::trim).toList();
				List<String> expected = Arrays.stream(firstfileHeaders.split(",", -1)).map(String::trim).toList();
				if (!actualHeaders.equals(expected)) {
					return FileValidationMessages.CSV_HEADERS_INVALID;
				}
				String firstRecord;
				do {
					firstRecord = reader.readLine();
				} while (firstRecord != null && firstRecord.isBlank());

				if (firstRecord == null) {
					return FileValidationMessages.CSV_NO_DATA_RECORDS;
				}
			}

		} catch (IOException ex) {
			return FileValidationMessages.FILE_READ_ERROR_PREFIX + ex.getMessage();
		}
		return null;
	}
}