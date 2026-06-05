package com.example.attendancetracker.data

import kotlinx.coroutines.flow.Flow

class AttendanceRepository(
    private val projectDao: ProjectDao,
    private val memberDao: MemberDao,
    private val attendanceRecordDao: AttendanceRecordDao,
) {
    // ── Projects ──────────────────────────────────────────────────────────────
    val projectsFlow: Flow<List<Project>> = projectDao.getProjects()

    fun getProjectById(projectId: Long): Flow<Project?> = projectDao.getProjectById(projectId)

    suspend fun addProject(project: Project) {
        if (project.name.isNotBlank()) projectDao.insert(project)
    }

    suspend fun deleteProject(projectId: Long) = projectDao.delete(projectId)

    // ── Members ───────────────────────────────────────────────────────────────
    fun getMembersByProject(projectId: Long): Flow<List<Member>> =
        memberDao.getMembersByProject(projectId)

    fun getMemberById(memberId: Long): Flow<Member?> = memberDao.getMemberById(memberId)

    suspend fun addMember(member: Member) {
        if (member.name.isNotBlank()) memberDao.insert(member)
    }

    // ── Attendance ────────────────────────────────────────────────────────────
    fun getAttendanceForDate(dateEpochDay: Long): Flow<List<AttendanceRecord>> =
        attendanceRecordDao.getAttendanceForDate(dateEpochDay)

    fun getAttendanceForMember(memberId: Long): Flow<List<AttendanceRecord>> =
        attendanceRecordDao.getAttendanceForMember(memberId)

    fun getAllAttendanceForProject(projectId: Long): Flow<List<AttendanceRecord>> =
        attendanceRecordDao.getAllAttendanceForProject(projectId)

    suspend fun markPresent(memberId: Long, dateEpochDay: Long) {
        attendanceRecordDao.insert(AttendanceRecord(memberId = memberId, dateEpochDay = dateEpochDay))
    }

    suspend fun clearAttendance(memberId: Long, dateEpochDay: Long) {
        attendanceRecordDao.deleteAttendance(memberId, dateEpochDay)
    }

    /** Submit a full day's attendance — replace everything for the given date in this project */
    suspend fun submitAttendance(
        presentMemberIds: Set<Long>,
        allMemberIds: List<Long>,
        dateEpochDay: Long,
    ) {
        // Remove all records for these members on this date
        attendanceRecordDao.deleteAttendanceForDate(allMemberIds, dateEpochDay)
        // Re-insert only the present ones
        val records = presentMemberIds.map { AttendanceRecord(memberId = it, dateEpochDay = dateEpochDay) }
        if (records.isNotEmpty()) attendanceRecordDao.insertAll(records)
    }
}
