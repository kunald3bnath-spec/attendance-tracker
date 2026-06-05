package com.example.attendancetracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY createdAt ASC")
    fun getProjects(): Flow<List<Project>>

    @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
    fun getProjectById(id: Long): Flow<Project?>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(project: Project): Long

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun delete(id: Long)
}
