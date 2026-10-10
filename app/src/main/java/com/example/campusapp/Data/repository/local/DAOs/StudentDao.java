package com.example.campusapp.Data.repository.local.remote.DAOs;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.campusapp.Data.local.entities.Student;

import java.util.List;

@Dao
public interface StudentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertStudents(List<Student> students);

    @Query("SELECT * FROM students WHERE name LIKE :query OR id LIKE :query")
    List<Student> getStudentsByQuery(String query);
}