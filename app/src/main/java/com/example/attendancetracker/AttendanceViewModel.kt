package com.example.attendancetracker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.attendancetracker.data.AttendanceRecord
import com.example.attendancetracker.data.AttendanceRepository
import com.example.attendancetracker.data.Member
import com.example.attendancetracker.data.Project
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class AttendanceViewModel(private val repository: AttendanceRepository) : ViewModel() {

    // ── Dark mode ─────────────────────────────────────────────────────────────
    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode

    fun toggleDarkMode() { _isDarkMode.value = !_isDarkMode.value }

    // ── Projects ──────────────────────────────────────────────────────────────
    val projects: StateFlow<List<Project>> = repository.projectsFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _selectedProjectId = MutableStateFlow<Long?>(null)

    val selectedProject: StateFlow<Project?> = _selectedProjectId
        .flatMapLatest { id -> id?.let { repository.getProjectById(it) } ?: flowOf(null) }
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    fun selectProject(projectId: Long) {
        if (_selectedProjectId.value != projectId) {
            _selectedProjectId.value = projectId
            _selectedMemberId.value = null
            _pendingAttendance.value = emptyMap()
            _submitted.value = false
        }
    }

    fun clearSelectedProject() {
        _selectedProjectId.value = null
        _selectedMemberId.value = null
        _pendingAttendance.value = emptyMap()
    }

    fun addProject(name: String) {
        viewModelScope.launch { repository.addProject(Project(name = name.trim())) }
    }

    fun deleteProject(projectId: Long) {
        viewModelScope.launch { repository.deleteProject(projectId) }
    }

    // ── Members ───────────────────────────────────────────────────────────────
    val members: StateFlow<List<Member>> = _selectedProjectId
        .flatMapLatest { id -> id?.let { repository.getMembersByProject(it) } ?: flowOf(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _selectedMemberId = MutableStateFlow<Long?>(null)

    val selectedMember: StateFlow<Member?> = _selectedMemberId
        .flatMapLatest { id -> id?.let { repository.getMemberById(it) } ?: flowOf(null) }
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    val selectedMemberAttendance: StateFlow<List<AttendanceRecord>> = _selectedMemberId
        .flatMapLatest { id -> id?.let { repository.getAttendanceForMember(it) } ?: flowOf(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun selectMember(memberId: Long) { _selectedMemberId.value = memberId }
    fun clearSelectedMember() { _selectedMemberId.value = null }

    fun addMember(name: String) {
        val projectId = _selectedProjectId.value ?: return
        viewModelScope.launch {
            repository.addMember(Member(projectId = projectId, name = name.trim()))
        }
    }

    // ── Date selection ────────────────────────────────────────────────────────
    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate

    /** Saved attendance records for the selected date */
    val attendanceForDate: StateFlow<List<AttendanceRecord>> = _selectedDate
        .flatMapLatest { repository.getAttendanceForDate(it.toEpochDay()) }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun setSelectedDate(date: LocalDate) {
        if (_selectedDate.value != date) {
            _selectedDate.value = date
            _pendingAttendance.value = emptyMap()
            _submitted.value = false
        }
    }

    // ── Pending attendance (uncommitted selections) ───────────────────────────
    /** memberId -> true (present) / false (absent). null = not yet touched by user */
    private val _pendingAttendance = MutableStateFlow<Map<Long, Boolean>>(emptyMap())
    val pendingAttendance: StateFlow<Map<Long, Boolean>> = _pendingAttendance

    private val _submitted = MutableStateFlow(false)
    val submitted: StateFlow<Boolean> = _submitted

    /** Toggle a member's pending state on the current date */
    fun togglePending(memberId: Long, present: Boolean) {
        _pendingAttendance.value = _pendingAttendance.value.toMutableMap().apply {
            put(memberId, present)
        }
        _submitted.value = false
    }

    /** Initialise pending map from saved DB state when date/project changes */
    fun initPendingFromSaved(savedRecords: List<AttendanceRecord>) {
        if (_pendingAttendance.value.isEmpty()) {
            _pendingAttendance.value = savedRecords.associate { it.memberId to true }
        }
    }

    /** Save all pending changes to Room */
    fun submitAttendance() {
        _selectedProjectId.value ?: return
        val allMembers = members.value
        if (allMembers.isEmpty()) return
        val dateEpochDay = _selectedDate.value.toEpochDay()

        // Merge: start from saved state, overlay pending changes
        val savedIds = attendanceForDate.value.map { it.memberId }.toSet()
        val presentIds = allMembers.map { it.id }.filter { memberId ->
            _pendingAttendance.value[memberId] ?: savedIds.contains(memberId)
        }.toSet()

        viewModelScope.launch {
            repository.submitAttendance(
                presentMemberIds = presentIds,
                allMemberIds = allMembers.map { it.id },
                dateEpochDay = dateEpochDay,
            )
            _submitted.value = true
            _pendingAttendance.value = emptyMap()
        }
    }

    // ── Overview (all attendance for current project) ─────────────────────────
    val projectAttendanceRecords: StateFlow<List<AttendanceRecord>> = _selectedProjectId
        .flatMapLatest { id -> id?.let { repository.getAllAttendanceForProject(it) } ?: flowOf(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
}

class AttendanceViewModelFactory(private val repository: AttendanceRepository) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AttendanceViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AttendanceViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
