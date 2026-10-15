package com.example.reconciliation.dto;

import com.example.reconciliation.entity.TransactionRecord;
import com.example.reconciliation.enums.RecordProcessingStatus;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TransactionProcessingResultDto {

    private FirstCsvRecordDto originalRecord;
    private TransactionRecord transaction;
    private RecordProcessingStatus status;
    private String message;
}