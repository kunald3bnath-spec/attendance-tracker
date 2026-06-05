package com.example.attendancetracker.data

import androidx.room.Entity

@Entity(
    tableName = "attendance_records",
    primaryKeys = ["memberId", "dateEpochDay"],
)
data class AttendanceRecord(
    val memberId: Long,
    val dateEpochDay: Long,
)
