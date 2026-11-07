package com.example.campusapp.data.sync;

import android.content.Context;

import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.example.campusapp.data.local.AppDatabase;
import com.example.campusapp.data.local.dao.OperationDao;
import com.example.campusapp.data.local.entities.OperationEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The phone's queue of lecturer actions that have not reached the server yet. Call add() and
 * statusByStudent() off the main thread.
 */
public final class OutboxQueue {

    static final String UNIQUE_WORK = "sync-operations";

    private final Context appContext;
    private final OperationDao dao;

    public OutboxQueue(Context context) {
        this.appContext = context.getApplicationContext();
        this.dao = AppDatabase.getInstance(appContext).operationDao();
    }

    /** Stores an action and starts a sync as soon as the phone has a connection. */
    public void add(OperationEntity op) {
        dao.insert(op);
        schedule(appContext);
    }

    /** Starts a sync if anything is waiting. Cheap to call often. */
    public void flushIfNeeded() {
        if (!dao.all().isEmpty()) {
            schedule(appContext);
        }
    }

    /**
     * The status to show for each student with a queued action. FAILED wins over PENDING,
     * because the lecturer has to act on it.
     */
    public Map<String, String> statusByStudent() {
        Map<String, String> out = new HashMap<>();
        List<OperationEntity> rows = dao.all();
        for (OperationEntity op : rows) {
            String current = out.get(op.studentId);
            if (current == null || OperationEntity.FAILED.equals(op.status)) {
                out.put(op.studentId, op.status);
            }
        }
        return out;
    }

    /** Removes this student's refused changes. The next roster load shows the server's version. */
    public void discardFailed(String studentId) {
        dao.deleteFailedFor(studentId);
    }

    static void schedule(Context app) {
        Constraints connected = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(OperationSyncWorker.class)
                .setConstraints(connected)
                .build();
        WorkManager.getInstance(app).enqueueUniqueWork(UNIQUE_WORK, ExistingWorkPolicy.APPEND_OR_REPLACE, request);
    }
}
