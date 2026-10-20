package com.example.reconciliation.constants;

public final class FileValidationMessages {
	private FileValidationMessages() {
	}

	public static final String FILE_DOES_NOT_EXIST = "File does not exist";
	public static final String PATH_NOT_REGULAR_FILE = "Path is not a regular file";
	public static final String FILE_NOT_READABLE = "File is not readable";
	public static final String INVALID_FILE_EXTENSION = "File must have a .csv extension";
	public static final String FILE_SIZE_EXCEEDED = "File exceeds the configured size limit";
	public static final String FILE_EMPTY = "File is empty";
	public static final String CSV_HEADER_MISSING = "CSV header is missing";
	public static final String CSV_HEADERS_INVALID = "CSV headers are missing, incorrect, or in the wrong order";
	public static final String CSV_NO_DATA_RECORDS = "CSV contains no data records";
	public static final String FILE_READ_ERROR_PREFIX = "Unable to read file: ";

	public static final String RECORD_VALID = "Record is valid";

	public static final String TRANSACTION_ID_REQUIRED = "Transaction ID is required";
	public static final String INVALID_TRANSACTION_ID_FORMAT = "Transaction ID must contain only letters and numbers";

	public static final String ACCOUNT_NUMBER_REQUIRED = "Account number is required";
	public static final String INVALID_ACCOUNT_NUMBER_FORMAT = "Account number must contain exactly 8 letters or digits";

	public static final String TRANSACTION_DATE_REQUIRED = "Transaction date is required";
	public static final String INVALID_TRANSACTION_DATE_FORMAT = "Transaction date must use yyyy-MM-dd format";

	public static final String AMOUNT_REQUIRED = "Amount is required";
	public static final String INVALID_AMOUNT = "Amount must be a valid number";
	public static final String NEGATIVE_AMOUNT = "Amount cannot be negative";

	public static final String CURRENCY_REQUIRED = "Currency is required";
	public static final String INVALID_CURRENCY_FORMAT = "Currency must contain exactly 3 letters";

	public static final String TRANSACTION_TYPE_REQUIRED = "Transaction type is required";
}
