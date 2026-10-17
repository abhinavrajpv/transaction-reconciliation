
package com.example.reconciliation.dto;

import java.math.BigDecimal;

import com.example.reconciliation.entity.TransactionRecord;
import com.example.reconciliation.enums.ReconciliationAction;
import com.example.reconciliation.enums.ReconciliationStatus;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReconciliationResultDto {

    private int rowNumber;
    private String transactionId;
    private ReconciliationStatus status;
    private ReconciliationAction action;
    private String message;
    private BigDecimal expectedAmount;
    private BigDecimal actualAmount;
    private TransactionRecord existingTransaction;
}
