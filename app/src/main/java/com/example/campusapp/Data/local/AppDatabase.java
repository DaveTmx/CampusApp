package com.example.campusapp.Data.local;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.campusapp.Data.local.dao.StudentDao;
import com.example.campusapp.Data.local.entities.StudentEntity;

/**
 * Version 1 because no release has shipped. Once data exists on devices,
 * bump the version and add a Migration with a migration test. Never use
 * destructive migration: pending offline work would be lost.
 */
@Database(entities = {StudentEntity.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;

    public abstract StudentDao studentDao();

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "campus_database").build();
                }
            }
        }
        return INSTANCE;
    }
}
