package com.example.campusapp.data.local.entities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * A lecturer action waiting to reach the server. operationId is made once, when the action is first
 * sent, and kept until the server confirms it. Sending the same operationId again is safe: the server
 * replays its answer instead of applying the change twice.
 */
@Entity(tableName = "pending_operations")
public class OperationEntity {

    /** Waiting to be sent. */
    public static final String PENDING = "PENDING";
    /** Being sent right now. Reset to PENDING if the app stops in the middle. */
    public static final String SYNCING = "SYNCING";
    /** The server refused it (for example GROUP_FULL). The lecturer must act. */
    public static final String FAILED = "FAILED";

    public static final String ASSIGN_GROUP = "ASSIGN_GROUP";
    public static final String UNASSIGN_GROUP = "UNASSIGN_GROUP";
    public static final String DELETE_STUDENT = "DELETE_STUDENT";

    @PrimaryKey
    @NonNull
    public String operationId;

    @NonNull
    public String opType;

    @NonNull
    public String studentId;

    @Nullable
    public String groupCode;

    @NonNull
    public String status;

    /** The server's error code, when status is FAILED. */
    @Nullable
    public String errorCode;

    /** Epoch millis. Used to send in the order the lecturer acted. */
    public long createdAt;

    public static OperationEntity create(String operationId, String opType, String studentId, String groupCode) {
        OperationEntity op = new OperationEntity();
        op.operationId = operationId;
        op.opType = opType;
        op.studentId = studentId;
        op.groupCode = groupCode;
        op.status = PENDING;
        op.createdAt = System.currentTimeMillis();
        return op;
    }
}
