package com.example.campusapp.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.campusapp.data.local.entities.OperationEntity;

import java.util.List;

@Dao
public interface OperationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(OperationEntity op);

    /** Operations still to send, oldest first. */
    @Query("SELECT * FROM pending_operations WHERE status = 'PENDING' ORDER BY createdAt")
    List<OperationEntity> pending();

    /** Everything not yet confirmed, including refused ones. Used to show status on the roster. */
    @Query("SELECT * FROM pending_operations")
    List<OperationEntity> all();

    @Query("UPDATE pending_operations SET status = :status, errorCode = :errorCode WHERE operationId = :operationId")
    void setStatus(String operationId, String status, String errorCode);

    /** After an interrupted sync, anything marked SYNCING goes back to PENDING. */
    @Query("UPDATE pending_operations SET status = 'PENDING' WHERE status = 'SYNCING'")
    void resetInterrupted();

    @Query("DELETE FROM pending_operations WHERE studentId = :studentId AND status = 'FAILED'")
    void deleteFailedFor(String studentId);

    @Query("DELETE FROM pending_operations WHERE operationId = :operationId")
    void delete(String operationId);
}
