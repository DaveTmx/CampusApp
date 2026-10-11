package com.example.campusapp.sync;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.example.campusapp.Data.local.AppDatabase;
import com.example.campusapp.Data.local.PendingOperation;
import com.example.campusapp.Data.local.SyncDao;
import com.example.campusapp.network.ApiClient;
import com.example.campusapp.network.ApiService;
import java.io.IOException;
import java.util.List;
import retrofit2.Response;

public class SyncWorker extends Worker {

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        SyncDao syncDao = AppDatabase.getInstance(getApplicationContext()).syncDao();
        List<PendingOperation> pendingList = syncDao.getAllPendingOperations();

        if (pendingList == null || pendingList.isEmpty()) {
            return Result.success();
        }

        ApiService apiService = ApiClient.getApiService();

        for (PendingOperation op : pendingList) {
            try {
                // Execute network call synchronously within background worker thread
                Response<Void> response = apiService.syncPendingOperation(op).execute();

                if (response.isSuccessful()) {
                    // Sync succeeded, remove operation from Room database queue
                    syncDao.deleteOperationById(op.getOperationId());
                } else {
                    // Server returned error status code, trigger retry
                    return Result.retry();
                }
            } catch (IOException e) {
                // Network unavailable or connection failed, schedule retry
                return Result.retry();
            }
        }

        return Result.success();
    }
}