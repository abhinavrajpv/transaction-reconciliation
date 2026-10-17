
package com.example.reconciliation.processor;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.reconciliation.dto.FirstCsvRecordDto;
import com.example.reconciliation.dto.TransactionProcessingResultDto;
import com.example.reconciliation.enums.RecordProcessingStatus;

public class FirstCsvRecordProcessorTest {

    private FirstCsvRecordProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new FirstCsvRecordProcessor();
    }

    private FirstCsvRecordDto validRecord() {
        FirstCsvRecordDto record = new FirstCsvRecordDto();
        record.setRowNumber(2);
        record.setTransactionId("TXN123");
        record.setAccountNumber("ACC12345");
        record.setTransactionDate("2026-10-16");
        record.setAmount("100.50");
        record.setCurrency("INR");
        record.setTransactionType("CREDIT");
        record.setDescription("Salary");
        return record;
    }

    private void assertInvalid(String expectedMessage,
                               FirstCsvRecordDto record) {
        TransactionProcessingResultDto result = processor.process(record);

        assertEquals(RecordProcessingStatus.INVALID, result.getStatus());
        assertEquals(expectedMessage, result.getMessage());
        assertSame(record, result.getOriginalRecord());
        assertNull(result.getTransaction());
    }

    @Test
    void shouldProcessValidRecord() {
        FirstCsvRecordDto record = validRecord();

        TransactionProcessingResultDto result = processor.process(record);

        assertEquals(RecordProcessingStatus.VALID, result.getStatus());
        assertEquals("Record is valid", result.getMessage());
        assertSame(record, result.getOriginalRecord());

        assertNotNull(result.getTransaction());
        assertEquals("TXN123", result.getTransaction().getTransactionId());
        assertEquals("ACC12345", result.getTransaction().getAccountNumber());
        assertEquals(LocalDate.of(2026, 10, 16),
                result.getTransaction().getTransactionDate());
        assertEquals(new BigDecimal("100.50"),
                result.getTransaction().getAmount());
        assertEquals("INR", result.getTransaction().getCurrency());
        assertEquals("CREDIT", result.getTransaction().getTransactionType());
        assertEquals("Salary", result.getTransaction().getDescription());
    }

    @Test
    void shouldTrimFieldsThatAreTrimmedByProcessor() {
        FirstCsvRecordDto record = validRecord();
        record.setTransactionId(" TXN123 ");
        record.setAccountNumber(" ACC12345 ");
        record.setTransactionDate(" 2026-10-16 ");
        record.setAmount(" 100.50 ");
        record.setCurrency(" INR ");
        record.setTransactionType(" CREDIT ");

        var result = processor.process(record);

        assertEquals(RecordProcessingStatus.VALID, result.getStatus());
        assertEquals("TXN123", result.getTransaction().getTransactionId());
        assertEquals("ACC12345", result.getTransaction().getAccountNumber());
        assertEquals("INR", result.getTransaction().getCurrency());
        assertEquals("CREDIT", result.getTransaction().getTransactionType());
    }

    @Test
    void shouldRejectNullTransactionId() {
        var record = validRecord();
        record.setTransactionId(null);
        assertInvalid("Transaction ID is required", record);
    }

    @Test
    void shouldRejectBlankTransactionId() {
        var record = validRecord();
        record.setTransactionId("   ");
        assertInvalid("Transaction ID is required", record);
    }

    @Test
    void shouldRejectSpecialCharactersInTransactionId() {
        var record = validRecord();
        record.setTransactionId("TXN-123");
        assertInvalid(
                "Transaction ID must contain only letters and numbers", record);
    }

    @Test
    void shouldRejectNullAccountNumber() {
        var record = validRecord();
        record.setAccountNumber(null);
        assertInvalid("Account number is required", record);
    }

    @Test
    void shouldRejectBlankAccountNumber() {
        var record = validRecord();
        record.setAccountNumber(" ");
        assertInvalid("Account number is required", record);
    }

    @Test
    void shouldRejectAccountNumberWithWrongLength() {
        var record = validRecord();
        record.setAccountNumber("ABC1234");
        assertInvalid(
                "Account number must contain exactly 8 letters or digits",
                record);
    }

    @Test
    void shouldRejectAccountNumberWithSpecialCharacters() {
        var record = validRecord();
        record.setAccountNumber("ABC12@45");
        assertInvalid(
                "Account number must contain exactly 8 letters or digits",
                record);
    }

    @Test
    void shouldRejectNullTransactionDate() {
        var record = validRecord();
        record.setTransactionDate(null);
        assertInvalid("Transaction date is required", record);
    }

    @Test
    void shouldRejectBlankTransactionDate() {
        var record = validRecord();
        record.setTransactionDate(" ");
        assertInvalid("Transaction date is required", record);
    }

    @Test
    void shouldRejectMalformedTransactionDate() {
        var record = validRecord();
        record.setTransactionDate("16-10-2026");
        assertInvalid(
                "Transaction date must use yyyy-MM-dd format", record);
    }

    @Test
    void shouldRejectImpossibleTransactionDate() {
        var record = validRecord();
        record.setTransactionDate("2026-02-30");
        assertInvalid(
                "Transaction date must use yyyy-MM-dd format", record);
    }

    @Test
    void shouldRejectNullAmount() {
        var record = validRecord();
        record.setAmount(null);
        assertInvalid("Amount is required", record);
    }

    @Test
    void shouldRejectBlankAmount() {
        var record = validRecord();
        record.setAmount(" ");
        assertInvalid("Amount is required", record);
    }

    @Test
    void shouldRejectNonNumericAmount() {
        var record = validRecord();
        record.setAmount("abc");
        assertInvalid("Amount must be a valid number", record);
    }

    @Test
    void shouldRejectNegativeAmount() {
        var record = validRecord();
        record.setAmount("-0.01");
        assertInvalid("Amount cannot be negative", record);
    }

    @Test
    void shouldAcceptZeroAmount() {
        var record = validRecord();
        record.setAmount("0");

        var result = processor.process(record);

        assertEquals(RecordProcessingStatus.VALID, result.getStatus());
        assertEquals(BigDecimal.ZERO, result.getTransaction().getAmount());
    }

    @Test
    void shouldRejectNullCurrency() {
        var record = validRecord();
        record.setCurrency(null);
        assertInvalid("Currency is required", record);
    }

    @Test
    void shouldRejectBlankCurrency() {
        var record = validRecord();
        record.setCurrency(" ");
        assertInvalid("Currency is required", record);
    }

    @Test
    void shouldRejectCurrencyWithWrongLength() {
        var record = validRecord();
        record.setCurrency("US");
        assertInvalid("Currency must contain exactly 3 letters", record);
    }

    @Test
    void shouldRejectCurrencyContainingDigits() {
        var record = validRecord();
        record.setCurrency("IN1");
        assertInvalid("Currency must contain exactly 3 letters", record);
    }

    @Test
    void shouldRejectNullTransactionType() {
        var record = validRecord();
        record.setTransactionType(null);
        assertInvalid("Transaction type is required", record);
    }

    @Test
    void shouldRejectBlankTransactionType() {
        var record = validRecord();
        record.setTransactionType(" ");
        assertInvalid("Transaction type is required", record);
    }

    @Test
    void shouldPreserveNullDescription() {
        var record = validRecord();
        record.setDescription(null);

        var result = processor.process(record);

        assertEquals(RecordProcessingStatus.VALID, result.getStatus());
        assertNull(result.getTransaction().getDescription());
    }
}
