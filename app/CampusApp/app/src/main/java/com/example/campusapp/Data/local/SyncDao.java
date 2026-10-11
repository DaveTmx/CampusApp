package com.example.campusapp.Data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;

@Dao
public interface SyncDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOperation(PendingOperation operation);

    @Query("SELECT * FROM pending_operations ORDER BY timestamp ASC")
    List<PendingOperation> getAllPendingOperations();

    @Query("DELETE FROM pending_operations WHERE operationId = :operationId")
    void deleteOperationById(String operationId);

    @Query("DELETE FROM pending_operations WHERE accountId = :accountId")
    void clearQueueForAccount(String accountId);
}