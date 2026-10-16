
package com.example.reconciliation.dto;

import java.math.BigDecimal;

import com.example.reconciliation.entity.TransactionRecord;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReconciliationResultDto {

    private int rowNumber;
    private String transactionId;
    private String status;
    private String action;
    private String message;
    private BigDecimal expectedAmount;
    private BigDecimal actualAmount;
    private TransactionRecord existingTransaction;
}
