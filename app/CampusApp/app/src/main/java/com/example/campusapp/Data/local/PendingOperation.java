package com.example.campusapp.Data.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "pending_operations")
public class PendingOperation {

    @PrimaryKey
    @NonNull
    private String operationId;

    private String studentId;
    private String accountId;
    private String operationType;
    private String payload;
    private int baseVersion;
    private long timestamp;

    public PendingOperation(@NonNull String operationId, String studentId, String accountId,
                            String operationType, String payload, int baseVersion, long timestamp) {
        this.operationId = operationId;
        this.studentId = studentId;
        this.accountId = accountId;
        this.operationType = operationType;
        this.payload = payload;
        this.baseVersion = baseVersion;
        this.timestamp = timestamp;
    }

    @NonNull
    public String getOperationId() { return operationId; }
    public String getStudentId() { return studentId; }
    public String getAccountId() { return accountId; }
    public String getOperationType() { return operationType; }
    public String getPayload() { return payload; }
    public int getBaseVersion() { return baseVersion; }
    public long getTimestamp() { return timestamp; }

    public void setOperationId(@NonNull String operationId) { this.operationId = operationId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }
    public void setOperationType(String operationType) { this.operationType = operationType; }
    public void setPayload(String payload) { this.payload = payload; }
    public void setBaseVersion(int baseVersion) { this.baseVersion = baseVersion; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}