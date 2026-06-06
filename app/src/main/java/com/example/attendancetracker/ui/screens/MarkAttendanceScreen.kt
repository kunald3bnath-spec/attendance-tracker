package com.example.attendancetracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.attendancetracker.AttendanceViewModel
import com.example.attendancetracker.ui.components.InlineCalendar
import com.example.attendancetracker.ui.components.MemberAvatarCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarkAttendanceScreen(
    viewModel: AttendanceViewModel,
) {
    val members by viewModel.members.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val attendanceForDate by viewModel.attendanceForDate.collectAsState()
    val pendingAttendance by viewModel.pendingAttendance.collectAsState()
    val submitted by viewModel.submitted.collectAsState()
    val projectAttendance by viewModel.projectAttendanceRecords.collectAsState()
    val selectedProject by viewModel.selectedProject.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var showAddMember by rememberSaveable { mutableStateOf(false) }
    var newMemberName by rememberSaveable { mutableStateOf("") }
    var memberToDelete by remember { mutableStateOf<Member?>(null) }

    // Compute set of dates that have any attendance record (for calendar dots)
    val markedDates = remember(projectAttendance) {
        projectAttendance.map { it.dateEpochDay }.toSet()
    }

    // Initialise pending from saved when date changes
    LaunchedEffect(selectedDate, attendanceForDate) {
        viewModel.initPendingFromSaved(attendanceForDate)
    }

    LaunchedEffect(submitted) {
        if (submitted) snackbarHostState.showSnackbar("Attendance saved ✓")
    }

    // Effective present state: pending overrides saved
    val savedPresentIds = remember(attendanceForDate) {
        attendanceForDate.map { it.memberId }.toSet()
    }

    fun isPresent(memberId: Long): Boolean =
        pendingAttendance[memberId] ?: savedPresentIds.contains(memberId)

    val presentCount = members.count { isPresent(it.id) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(selectedProject?.name ?: "Mark Attendance", fontWeight = FontWeight.Bold)
                        Text(
                            "$presentCount / ${members.size} present",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showAddMember = true }
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Add member",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // Inline calendar — always visible, compact
            InlineCalendar(
                selectedDate = selectedDate,
                markedDates = markedDates,
                onDateSelected = { viewModel.setSelectedDate(it) },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )

            Spacer(Modifier.height(4.dp))

            if (members.isEmpty()) {
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Group, "No members",
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text("No members yet", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Tap + to add members",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            } else {
                // Member avatar grid — scrollable, weight fills remaining space
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(members, key = { it.id }) { member ->
                        MemberAvatarCard(
                            name = member.name,
                            isPresent = isPresent(member.id),
                            onToggle = { present -> viewModel.togglePending(member.id, present) },
                            onLongClick = { memberToDelete = member }
                        )
                    }
                }
            }

            // Submit bar — pinned above bottom nav
            if (members.isNotEmpty()) {
                Button(
                    onClick = { viewModel.submitAttendance() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(
                        text = "Submit Attendance  ($presentCount marked present)",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }

    if (showAddMember) {
        AlertDialog(
            onDismissRequest = { showAddMember = false; newMemberName = "" },
            confirmButton = {
                Button(
                    onClick = { viewModel.addMember(newMemberName); newMemberName = ""; showAddMember = false },
                    enabled = newMemberName.isNotBlank()
                ) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showAddMember = false; newMemberName = "" }) { Text("Cancel") }
            },
            title = { Text("Add Member", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newMemberName,
                    onValueChange = { newMemberName = it },
                    label = { Text("Member name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        )
    }

    memberToDelete?.let { member ->
        AlertDialog(
            onDismissRequest = { memberToDelete = null },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteMember(member.id)
                        memberToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { memberToDelete = null }) {
                    Text("Cancel")
                }
            },
            title = { Text("Delete Member?", fontWeight = FontWeight.Bold) },
            text = {
                Text("Are you sure you want to delete member \"${member.name}\"? This will permanently delete all their attendance history. This action cannot be undone.")
            }
        )
    }
}
