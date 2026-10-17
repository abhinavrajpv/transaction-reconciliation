
package com.example.reconciliation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

public class FileValidationServiceTest {

	@TempDir
	Path tempDir;

	private FileValidationService service;

	private static final String HEADERS = "transaction_id,account_number,transaction_date,amount,currency,transaction_type,description";

	@BeforeEach
	void setUp() {
		service = new FileValidationService();
		ReflectionTestUtils.setField(service, "maxFileSize", 1024L);
		ReflectionTestUtils.setField(service, "firstfileHeaders", HEADERS);
	}

	private Path createFile(String name, String content) throws IOException {
		Path file = tempDir.resolve(name);
		Files.writeString(file, content);
		return file;
	}

	@Test
	void shouldRejectNonexistentFile() {
		assertEquals("File does not exist", service.validateFile(tempDir.resolve("missing.csv").toString()));
	}

	@Test
	void shouldRejectDirectory() {
		assertEquals("Path is not a regular file", service.validateFile(tempDir.toString()));
	}

	@ParameterizedTest
	@ValueSource(strings = { "transactions.txt", "transactions.pdf", "transactions.json" })
	void shouldRejectWrongExtension(String fileName) throws IOException {
		Path file = createFile(fileName, HEADERS + "\nrow\n");

		assertEquals("File must have a .csv extension", service.validateFile(file.toString()));
	}

	@ParameterizedTest
	@CsvSource({ "'', File is empty", "'\n', CSV header is missing" })
	void shouldRejectEmptyFileOrMissingHeader(String content, String expectedMessage) throws IOException {
		Path file = createFile("test.csv", content);

		assertEquals(expectedMessage, service.validateFile(file.toString()));
	}

	@Test
	void shouldRejectFileExceedingSizeLimit() throws IOException {
		ReflectionTestUtils.setField(service, "maxFileSize", 10L);
		Path file = createFile("large.csv", "this file is larger than ten bytes");

		assertEquals("File exceeds the configured size limit", service.validateFile(file.toString()));
	}

	@ParameterizedTest
	@ValueSource(strings = { "transaction_id,amount,account_number",
			"account_number,transaction_id,transaction_date,amount,currency,transaction_type,description",
			"transaction_id,account_number,transaction_date,amount,currency,transaction_type,description,extra" })
	void shouldRejectIncorrectHeaders(String header) throws IOException {
		Path file = createFile("wrong-header.csv", header + "\n");

		assertEquals("CSV headers are missing, incorrect, or in the wrong order",
				service.validateFile(file.toString()));
	}

	@Test
	void shouldRejectFileWithoutDataRecords() throws IOException {
		Path file = createFile("header-only.csv", HEADERS + "\n\n   \n");

		assertEquals("CSV contains no data records", service.validateFile(file.toString()));
	}

	@Test
	void shouldAcceptValidCsv() throws IOException {
		Path file = createFile("valid.csv", HEADERS + "\nTXN1,ACC12345,2026-10-16,10,INR,CREDIT,Test\n");

		assertNull(service.validateFile(file.toString()));
	}

	@Test
	void shouldAcceptHeaderWithUtf8Bom() throws IOException {
		Path file = createFile("bom.csv", "\uFEFF" + HEADERS + "\nTXN1,ACC12345,2026-10-16,10,INR,CREDIT,Test\n");

		assertNull(service.validateFile(file.toString()));
	}

	@Test
	void shouldIgnoreBlankLinesBeforeFirstRecord() throws IOException {
		Path file = createFile("blank-lines.csv", HEADERS + "\n\n   \nTXN1,ACC12345,2026-10-16,10,INR,CREDIT,Test\n");

		assertNull(service.validateFile(file.toString()));
	}

	@Test
	void shouldAcceptUppercaseCsvExtension() throws IOException {
		Path file = createFile("valid.CSV", HEADERS + "\nTXN1,ACC12345,2026-10-16,10,INR,CREDIT,Test\n");

		assertNull(service.validateFile(file.toString()));
	}

	@Test
	void shouldAcceptFileExactlyAtSizeLimit() throws Exception {
		String content = HEADERS + "\n" + "TXN001,ACC12345,2026-10-01,100,INR,CREDIT,Salary";

		Path file = tempDir.resolve("exact-limit.csv");
		Files.writeString(file, content);

		ReflectionTestUtils.setField(service, "maxFileSize", Files.size(file));

		assertNull(service.validateFile(file.toString()));
	}

	@Test
	void shouldRejectFileOneByteOverSizeLimit() throws Exception {
		Path file = tempDir.resolve("over-limit.csv");

		Files.writeString(file, HEADERS + "\n" + "TXN001,ACC12345,2026-10-01,100,INR,CREDIT,Salary");

		ReflectionTestUtils.setField(service, "maxFileSize", Files.size(file) - 1);

		assertEquals("File exceeds the configured size limit", service.validateFile(file.toString()));
	}

	@ParameterizedTest
	@CsvSource({ "'   \n', CSV header is missing",
			"'transaction_id,account_number,transaction_date,amount,currency,transaction_type,description\n\n   \n', CSV contains no data records" })
	void shouldRejectBlankHeaderOrHeaderOnlyFile(String content, String expectedMessage) throws Exception {
		Path file = tempDir.resolve("test.csv");
		Files.writeString(file, content);

		assertEquals(expectedMessage, service.validateFile(file.toString()));
	}

	@Test
	void shouldAcceptUtf8BomAndWhitespaceAroundHeaders() throws Exception {
		Path file = tempDir.resolve("bom.csv");

		Files.writeString(file,
				"\uFEFF " + HEADERS.replace(",", " , ") + " \n" + "TXN001,ACC12345,2026-10-01,100,INR,CREDIT,Salary");

		assertNull(service.validateFile(file.toString()));
	}

	@Test
	void shouldRejectUnreadableFile() throws Exception {
		Path file = tempDir.resolve("unreadable.csv");
		Files.writeString(file, HEADERS + "\n" + "TXN001,ACC12345,2026-10-01,100,INR,CREDIT,Salary");

		try (var mockedFiles = Mockito.mockStatic(Files.class, Mockito.CALLS_REAL_METHODS)) {

			mockedFiles.when(() -> Files.isReadable(file)).thenReturn(false);

			assertEquals("File is not readable", service.validateFile(file.toString()));
		}
	}

	@Test
	void shouldReturnErrorWhenFileCannotBeRead() throws Exception {
		Path file = tempDir.resolve("unreadable-content.csv");

		Files.writeString(file, HEADERS + "\n" + "TXN001,ACC12345,2026-10-01,100,INR,CREDIT,Salary");

		try (var mockedFiles = Mockito.mockStatic(Files.class, Mockito.CALLS_REAL_METHODS)) {

			mockedFiles.when(() -> Files.newBufferedReader(file, StandardCharsets.UTF_8))
					.thenThrow(new IOException("Simulated read failure"));

			assertEquals("Unable to read file: Simulated read failure", service.validateFile(file.toString()));
		}
	}

	@Test
	void shouldCoverNullHeaderBranch() throws Exception {
		Path file = tempDir.resolve("empty.csv");
		Files.createFile(file);

		assertEquals("File is empty", service.validateFile(file.toString()));
	}

	@Test
	void shouldCoverBlankHeaderBranch() throws Exception {
		Path file = tempDir.resolve("blank-header.csv");

		Files.writeString(file, "   \n" + HEADERS + "\n" + "TXN001,ACC12345,2026-10-01,100,INR,CREDIT,Salary");

		assertEquals("CSV header is missing", service.validateFile(file.toString()));
	}

	@Test
	void shouldCoverNonblankHeaderBranch() throws Exception {
		Path file = tempDir.resolve("valid.csv");

		Files.writeString(file, HEADERS + "\n" + "TXN001,ACC12345,2026-10-01,100,INR,CREDIT,Salary");

		assertNull(service.validateFile(file.toString()));
	}
}
