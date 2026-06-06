package com.example.attendancetracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceRecordDao {
    @Query("SELECT * FROM attendance_records WHERE dateEpochDay = :dateEpochDay")
    fun getAttendanceForDate(dateEpochDay: Long): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance_records WHERE memberId = :memberId ORDER BY dateEpochDay DESC")
    fun getAttendanceForMember(memberId: Long): Flow<List<AttendanceRecord>>


    @Query("""
        SELECT ar.* FROM attendance_records ar
        INNER JOIN members m ON ar.memberId = m.id
        WHERE m.projectId = :projectId
        ORDER BY ar.dateEpochDay DESC
    """)
    fun getAllAttendanceForProject(projectId: Long): Flow<List<AttendanceRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: AttendanceRecord)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<AttendanceRecord>)


    @Query("DELETE FROM attendance_records WHERE memberId IN (:memberIds) AND dateEpochDay = :dateEpochDay")
    suspend fun deleteAttendanceForDate(memberIds: List<Long>, dateEpochDay: Long)

    @Query("DELETE FROM attendance_records WHERE memberId = :memberId")
    suspend fun deleteAttendanceForMember(memberId: Long)

    @Query("DELETE FROM attendance_records WHERE memberId IN (SELECT id FROM members WHERE projectId = :projectId)")
    suspend fun deleteAttendanceForProject(projectId: Long)
}
