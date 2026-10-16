package com.example.reconciliation.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SecondCsvRecordDto {

	private int rowNumber;
	private String transactionId;
	private String accountNumber;
	private String transactionDate;
	private String amount;
	private String currency;
	private String transactionType;
	private String description;
}