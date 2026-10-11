package com.example.campusapp.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.campusapp.data.local.entities.StudentEntity;

import java.util.List;

@Dao
public interface StudentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsertAll(List<StudentEntity> rows);

    /**
     * One page of cached students. Pass like = "%" + query + "%". Must NOT be
     * called on the main thread.
     */
    @Query("SELECT * FROM students WHERE fullName LIKE :like OR studentNumber LIKE :like "
            + "ORDER BY fullName LIMIT :limit OFFSET :offset")
    List<StudentEntity> searchCached(String like, int limit, int offset);
}
