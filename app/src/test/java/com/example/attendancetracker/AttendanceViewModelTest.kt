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
import kotlinx.coroutines.test.runTest
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

    // ── Project / Member persistence ──────────────────────────────────────────

    @Test
    fun testAddProjectPersistsToDao() = runTest {
        viewModel.addProject("Sprint Team")
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, fakeProjectDao.projects.value.size)
        assertEquals("Sprint Team", fakeProjectDao.projects.value[0].name)
    }

    @Test
    fun testBlankProjectNameNotAdded() = runTest {
        viewModel.addProject("   ")
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(fakeProjectDao.projects.value.isEmpty())
    }

    @Test
    fun testDeleteProjectRemovesFromDao() = runTest {
        fakeProjectDao.insert(Project(name = "Old Project"))
        val projectId = fakeProjectDao.projects.value[0].id

        viewModel.deleteProject(projectId)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(fakeProjectDao.projects.value.isEmpty())
    }

    @Test
    fun testAddMemberPersistsToDao() = runTest {
        fakeProjectDao.insert(Project(id = 1L, name = "Team A"))
        viewModel.selectProject(1L)

        viewModel.addMember("Alice")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, fakeMemberDao.members.value.size)
        assertEquals("Alice", fakeMemberDao.members.value[0].name)
        assertEquals(1L, fakeMemberDao.members.value[0].projectId)
    }

    @Test
    fun testBlankMemberNameNotAdded() = runTest {
        fakeProjectDao.insert(Project(id = 1L, name = "Team A"))
        viewModel.selectProject(1L)
        viewModel.addMember("")
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(fakeMemberDao.members.value.isEmpty())
    }

    // ── Date selection ────────────────────────────────────────────────────────

    @Test
    fun testSetSelectedDateClearsPending() {
        viewModel.selectProject(1L)
        viewModel.togglePending(5L, true)
        assertEquals(1, viewModel.pendingAttendance.value.size)

        viewModel.setSelectedDate(LocalDate.now().plusDays(1))

        assertTrue(viewModel.pendingAttendance.value.isEmpty())
        assertFalse(viewModel.submitted.value)
    }

    @Test
    fun testSetSelectedDateSameDateDoesNotClearPending() {
        viewModel.togglePending(5L, true)
        val today = LocalDate.now()

        viewModel.setSelectedDate(today)  // same date — should be a no-op

        assertEquals(1, viewModel.pendingAttendance.value.size)
    }

    // ── Pending / initFromSaved ───────────────────────────────────────────────

    @Test
    fun testInitPendingFromSavedPopulatesMap() {
        val saved = listOf(
            AttendanceRecord(memberId = 1L, dateEpochDay = 100L),
            AttendanceRecord(memberId = 2L, dateEpochDay = 100L),
        )
        viewModel.initPendingFromSaved(saved)

        assertEquals(true, viewModel.pendingAttendance.value[1L])
        assertEquals(true, viewModel.pendingAttendance.value[2L])
    }

    @Test
    fun testInitPendingFromSavedIsNoOpWhenNonEmpty() {
        viewModel.togglePending(1L, false)  // pending is now non-empty

        // Calling initPendingFromSaved should NOT overwrite the user's explicit toggle
        viewModel.initPendingFromSaved(listOf(AttendanceRecord(memberId = 1L, dateEpochDay = 100L)))

        assertEquals(false, viewModel.pendingAttendance.value[1L])
    }

    // ── clearSelectedProject ──────────────────────────────────────────────────

    @Test
    fun testClearSelectedProjectResetsState() {
        viewModel.selectProject(1L)
        viewModel.togglePending(3L, true)
        assertFalse(viewModel.pendingAttendance.value.isEmpty())

        viewModel.clearSelectedProject()

        assertTrue(viewModel.pendingAttendance.value.isEmpty())
        assertNull(viewModel.selectedProject.value)
    }

    // ── submitted flag ────────────────────────────────────────────────────────

    @Test
    fun testTogglePendingKeepsSubmittedFalse() {
        viewModel.selectProject(1L)
        viewModel.togglePending(1L, true)
        assertFalse(viewModel.submitted.value)
        viewModel.togglePending(1L, false)
        assertFalse(viewModel.submitted.value)
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
