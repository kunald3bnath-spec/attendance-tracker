package com.example.attendancetracker

import com.example.attendancetracker.data.AttendanceRecord
import com.example.attendancetracker.data.AttendanceRepository
import com.example.attendancetracker.data.Member
import com.example.attendancetracker.data.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class AttendanceViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    // Fakes
    private lateinit var fakeProjectDao: FakeProjectDao
    private lateinit var fakeMemberDao: FakeMemberDao
    private lateinit var fakeAttendanceRecordDao: FakeAttendanceRecordDao
    private lateinit var repository: AttendanceRepository
    private lateinit var viewModel: AttendanceViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeProjectDao = FakeProjectDao()
        fakeMemberDao = FakeMemberDao()
        fakeAttendanceRecordDao = FakeAttendanceRecordDao()
        repository = AttendanceRepository(fakeProjectDao, fakeMemberDao, fakeAttendanceRecordDao)
        viewModel = AttendanceViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testDarkModeToggle() {
        assertFalse(viewModel.isDarkMode.value)
        viewModel.toggleDarkMode()
        assertTrue(viewModel.isDarkMode.value)
        viewModel.toggleDarkMode()
        assertFalse(viewModel.isDarkMode.value)
    }

    @Test
    fun testSelectProjectClearsPending() {
        viewModel.selectProject(1L)
        viewModel.togglePending(10L, true)
        assertEquals(true, viewModel.pendingAttendance.value[10L])

        // Selecting a different project clears pending attendance state
        viewModel.selectProject(2L)
        assertTrue(viewModel.pendingAttendance.value.isEmpty())
        assertFalse(viewModel.submitted.value)
    }

    @Test
    fun testTogglePendingState() {
        viewModel.selectProject(1L)
        assertNull(viewModel.pendingAttendance.value[10L])

        viewModel.togglePending(10L, true)
        assertEquals(true, viewModel.pendingAttendance.value[10L])

        viewModel.togglePending(10L, false)
        assertEquals(false, viewModel.pendingAttendance.value[10L])
    }

    // Fake DAO implementations storing state in local memory Flow objects
    private class FakeProjectDao : com.example.attendancetracker.data.ProjectDao {
        val projects = MutableStateFlow<List<Project>>(emptyList())

        override fun getProjects(): Flow<List<Project>> = projects
        override fun getProjectById(id: Long): Flow<Project?> = projects.map { list -> list.find { it.id == id } }
        override suspend fun insert(project: Project): Long {
            val newList = projects.value.toMutableList()
            val newId = (newList.size + 1).toLong()
            newList.add(project.copy(id = newId))
            projects.value = newList
            return newId
        }
        override suspend fun delete(id: Long) {
            projects.value = projects.value.filter { it.id != id }
        }
    }

    private class FakeMemberDao : com.example.attendancetracker.data.MemberDao {
        val members = MutableStateFlow<List<Member>>(emptyList())

        override fun getMembersByProject(projectId: Long): Flow<List<Member>> =
            members.map { list -> list.filter { it.projectId == projectId } }
        override fun getMemberById(id: Long): Flow<Member?> = members.map { list -> list.find { it.id == id } }
        override suspend fun insert(member: Member) {
            val newList = members.value.toMutableList()
            val newId = (newList.size + 1).toLong()
            newList.add(member.copy(id = newId))
            members.value = newList
        }
    }

    private class FakeAttendanceRecordDao : com.example.attendancetracker.data.AttendanceRecordDao {
        val records = MutableStateFlow<List<AttendanceRecord>>(emptyList())

        override fun getAttendanceForDate(dateEpochDay: Long): Flow<List<AttendanceRecord>> =
            records.map { list -> list.filter { it.dateEpochDay == dateEpochDay } }
        override fun getAttendanceForMember(memberId: Long): Flow<List<AttendanceRecord>> =
            records.map { list -> list.filter { it.memberId == memberId } }
        override fun getAllAttendance(): Flow<List<AttendanceRecord>> = records
        override fun getAllAttendanceForProject(projectId: Long): Flow<List<AttendanceRecord>> = records

        override suspend fun insert(record: AttendanceRecord) {
            val newList = records.value.toMutableList()
            newList.add(record)
            records.value = newList
        }

        override suspend fun insertAll(records: List<AttendanceRecord>) {
            val newList = this.records.value.toMutableList()
            newList.addAll(records)
            this.records.value = newList
        }

        override suspend fun deleteAttendance(memberId: Long, dateEpochDay: Long) {
            records.value = records.value.filterNot { it.memberId == memberId && it.dateEpochDay == dateEpochDay }
        }

        override suspend fun deleteAttendanceForDate(memberIds: List<Long>, dateEpochDay: Long) {
            records.value = records.value.filterNot { memberIds.contains(it.memberId) && it.dateEpochDay == dateEpochDay }
        }
    }
}
