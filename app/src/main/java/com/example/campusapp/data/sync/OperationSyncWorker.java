package com.example.campusapp.data.sync;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.campusapp.data.local.AppDatabase;
import com.example.campusapp.data.local.dao.OperationDao;
import com.example.campusapp.data.local.entities.OperationEntity;
import com.example.campusapp.data.remote.ApiService;
import com.example.campusapp.data.remote.StudentDtos;
import com.example.campusapp.util.ServiceLocator;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;

import java.io.IOException;
import java.util.List;

import retrofit2.Response;

/**
 * Sends queued lecturer actions to POST /api/operations, oldest first.
 *  - Confirmed (200): removed from the queue.
 *  - Refused by the server (409 GROUP_FULL, 404, 410, 400): marked FAILED for the lecturer to act on.
 *  - No connection, 5xx or 429: left PENDING and retried later.
 *  - 401: stops. The lecturer has to sign in again; nothing is lost.
 */
public class OperationSyncWorker extends Worker {

    private final Gson gson = new Gson();

    public OperationSyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context app = getApplicationContext();
        OperationDao dao = AppDatabase.getInstance(app).operationDao();
        ApiService api = ServiceLocator.apiService(app);
        dao.resetInterrupted();

        while (true) {
            List<OperationEntity> waiting = dao.pending();
            if (waiting.isEmpty()) {
                return Result.success();
            }
            OperationEntity op = waiting.get(0);
            dao.setStatus(op.operationId, OperationEntity.SYNCING, null);

            try {
                StudentDtos.OperationRequest body = new StudentDtos.OperationRequest(
                        op.operationId, op.opType, new StudentDtos.Payload(op.studentId, op.groupCode));
                Response<StudentDtos.OperationResponse> reply = api.submitOperation(body).execute();

                if (reply.isSuccessful()) {
                    dao.delete(op.operationId);
                } else if (reply.code() == 401) {
                    dao.setStatus(op.operationId, OperationEntity.PENDING, null);
                    return Result.failure();
                } else if (reply.code() >= 500 || reply.code() == 429) {
                    dao.setStatus(op.operationId, OperationEntity.PENDING, null);
                    return Result.retry();
                } else {
                    dao.setStatus(op.operationId, OperationEntity.FAILED, errorCodeOf(reply));
                }
            } catch (IOException e) {
                dao.setStatus(op.operationId, OperationEntity.PENDING, null);
                return Result.retry();
            }
        }
    }

    private String errorCodeOf(Response<?> reply) {
        try {
            if (reply.errorBody() != null) {
                StudentDtos.OperationResponse err = gson.fromJson(
                        reply.errorBody().string(), StudentDtos.OperationResponse.class);
                if (err != null && err.error != null) {
                    return err.error;
                }
            }
        } catch (IOException | JsonParseException e) {
            // fall through to the HTTP status
        }
        return "HTTP_" + reply.code();
    }
}
