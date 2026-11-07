package com.example.campusapp.data.local;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.database.Cursor;

import androidx.room.testing.MigrationTestHelper;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

/**
 * Version 1 to 2 must keep the roster cache and add the queue table. Needs app/schemas/1.json and 2.json,
 * which Room writes on each build.
 */
@RunWith(AndroidJUnit4.class)
public class AppDatabaseMigrationTest {

    private static final String DB = "migration-test";

    @Rule
    public MigrationTestHelper helper = new MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(), AppDatabase.class);

    @Test
    public void migrationKeepsStudentsAndAddsTheQueue() throws IOException {
        SupportSQLiteDatabase v1 = helper.createDatabase(DB, 1);
        v1.execSQL("INSERT INTO students (studentId, studentNumber, fullName, programme, confirmedGroup, version, cachedAt)"
                + " VALUES ('s-1', '900000001', 'Kept Student', 'CS', 'G01', 3, 0)");
        v1.close();

        SupportSQLiteDatabase v2 = helper.runMigrationsAndValidate(DB, 2, true, AppDatabase.MIGRATION_1_2);

        try (Cursor c = v2.query("SELECT fullName, version FROM students WHERE studentId = 's-1'")) {
            assertTrue(c.moveToFirst());
            assertEquals("Kept Student", c.getString(0));
            assertEquals(3, c.getInt(1));
        }

        v2.execSQL("INSERT INTO pending_operations (operationId, opType, studentId, groupCode, status, errorCode, createdAt)"
                + " VALUES ('op-1', 'ASSIGN_GROUP', 's-1', 'G02', 'PENDING', NULL, 1)");
        try (Cursor c = v2.query("SELECT COUNT(*) FROM pending_operations")) {
            assertTrue(c.moveToFirst());
            assertEquals(1, c.getInt(0));
        }
    }
}
