package com.example.campusapp.data.local;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.campusapp.data.local.dao.OperationDao;
import com.example.campusapp.data.local.dao.StudentDao;
import com.example.campusapp.data.local.entities.OperationEntity;
import com.example.campusapp.data.local.entities.StudentEntity;

/**
 * Version 2 adds the queue of lecturer actions waiting to reach the server. Never use destructive
 * migration: pending offline work would be lost.
 */
// exportSchema = true writes app/schemas/<version>.json on every build. Commit those files:
// the migration test needs the old schema to start from.
@Database(entities = {StudentEntity.class, OperationEntity.class}, version = 2, exportSchema = true)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;

    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `pending_operations` ("
                    + "`operationId` TEXT NOT NULL, "
                    + "`opType` TEXT NOT NULL, "
                    + "`studentId` TEXT NOT NULL, "
                    + "`groupCode` TEXT, "
                    + "`status` TEXT NOT NULL, "
                    + "`errorCode` TEXT, "
                    + "`createdAt` INTEGER NOT NULL, "
                    + "PRIMARY KEY(`operationId`))");
        }
    };

    public abstract StudentDao studentDao();

    public abstract OperationDao operationDao();

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "campus_database")
                            .addMigrations(MIGRATION_1_2)
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
