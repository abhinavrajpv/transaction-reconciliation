package com.example.reconciliation.utility;

import org.springframework.stereotype.Component;

import lombok.Getter;

@Getter
@Component
public class ProcessingCounters {

    private long totalFirstFileRecords;
    private long matchedRecords;
    private long mismatchedRecords;
    private long invalidRecords;
    private long duplicateRecords;
    private long missingFromFirstFileRecords;

    public synchronized void recordFirstFileRecord() {
        totalFirstFileRecords++;
    }

    public synchronized void recordMatched() {
        matchedRecords++;
    }

    public synchronized void recordMismatched() {
        mismatchedRecords++;
    }

    public synchronized void recordInvalid() {
        invalidRecords++;
    }
    public synchronized void recordDuplicate() {
        duplicateRecords++;
    }
    public synchronized void recordMissingFromFirstFile() {
        missingFromFirstFileRecords++;
    }
    
    public synchronized void reset() {
        totalFirstFileRecords = 0;
        matchedRecords = 0;
        mismatchedRecords = 0;
        invalidRecords = 0;
        duplicateRecords = 0;
        missingFromFirstFileRecords = 0;
    }
}
