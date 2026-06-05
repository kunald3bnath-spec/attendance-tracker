package com.example.attendancetracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MemberDao {
    @Query("SELECT * FROM members WHERE projectId = :projectId ORDER BY name ASC")
    fun getMembersByProject(projectId: Long): Flow<List<Member>>

    @Query("SELECT * FROM members WHERE id = :id LIMIT 1")
    fun getMemberById(id: Long): Flow<Member?>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(member: Member)
}
