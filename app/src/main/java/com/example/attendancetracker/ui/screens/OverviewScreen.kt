package com.example.attendancetracker.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.attendancetracker.AttendanceViewModel
import com.example.attendancetracker.data.Member
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverviewScreen(viewModel: AttendanceViewModel) {
    val members by viewModel.members.collectAsState()
    val allRecords by viewModel.projectAttendanceRecords.collectAsState()

    var displayMonth by remember { mutableStateOf(YearMonth.now()) }
    var expandedDate by remember { mutableStateOf<LocalDate?>(null) }

    val totalMembers = members.size

    // attendanceByDate: epochDay -> list of memberIds present
    val attendanceByDate = remember(allRecords) {
        allRecords.groupBy({ it.dateEpochDay }, { it.memberId })
    }

    // membersById map for quick lookup
    val membersById = remember(members) { members.associateBy { it.id } }

    // Build sorted unique dates for the expandable list (most recent first)
    val sortedDates = remember(allRecords) {
        allRecords.map { LocalDate.ofEpochDay(it.dateEpochDay) }
            .distinct()
            .sortedDescending()
    }

    val dayHeaders = listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa")
    val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")
    val dateFormatter = DateTimeFormatter.ofPattern("EEE, MMM d yyyy")

    val firstDay = displayMonth.atDay(1)
    val startOffset = firstDay.dayOfWeek.value % 7
    val daysInMonth = displayMonth.lengthOfMonth()
    val totalCells = startOffset + daysInMonth
    val rows = (totalCells + 6) / 7

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Overview", fontWeight = FontWeight.Bold) },
                actions = {
                    Icon(Icons.Default.BarChart, null, modifier = Modifier.padding(end = 16.dp), tint = MaterialTheme.colorScheme.primary)
                }
            )
        }
    ) { padding ->
        LazyColumn(
            Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // ── Heatmap Calendar ──────────────────────────────────────────────
            item {
                Card(
                    Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(Modifier.padding(12.dp)) {
                        // Month nav
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(onClick = { displayMonth = displayMonth.minusMonths(1) }, Modifier.size(36.dp)) {
                                Icon(Icons.Default.ChevronLeft, "Prev month")
                            }
                            Text(displayMonth.format(monthFormatter), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                            IconButton(onClick = { displayMonth = displayMonth.plusMonths(1) }, Modifier.size(36.dp)) {
                                Icon(Icons.Default.ChevronRight, "Next month")
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        // Day headers
                        Row(Modifier.fillMaxWidth()) {
                            dayHeaders.forEach { h ->
                                Text(h, Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        // Calendar grid
                        for (row in 0 until rows) {
                            Row(Modifier.fillMaxWidth()) {
                                for (col in 0 until 7) {
                                    val cellIndex = row * 7 + col
                                    val dayNum = cellIndex - startOffset + 1
                                    val isValid = dayNum in 1..daysInMonth
                                    val cellDate = if (isValid) displayMonth.atDay(dayNum) else null
                                    val presentCount = cellDate?.let {
                                        attendanceByDate[it.toEpochDay()]?.size ?: 0
                                    } ?: 0
                                    val fraction = if (totalMembers > 0 && isValid) presentCount.toFloat() / totalMembers else 0f

                                    Box(
                                        Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .padding(2.dp)
                                            .clip(CircleShape)
                                            .background(heatmapColor(fraction, isValid, MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.primary))
                                            .then(if (isValid) Modifier.clickable {
                                                expandedDate = if (expandedDate == cellDate) null else cellDate
                                            } else Modifier),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isValid) {
                                            Text(
                                                dayNum.toString(),
                                                fontSize = 11.sp,
                                                fontWeight = if (fraction > 0f) FontWeight.Bold else FontWeight.Normal,
                                                color = if (fraction > 0.6f) Color.White
                                                else MaterialTheme.colorScheme.onSurface.copy(alpha = if (isValid) 1f else 0f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        // Legend
                        Spacer(Modifier.height(8.dp))
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("No data", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                            for (i in 0..4) {
                                Box(
                                    Modifier
                                        .padding(horizontal = 2.dp)
                                        .size(14.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(heatmapColor(i / 4f, true, MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.primary))
                                )
                            }
                            Text("Full", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                        }
                    }
                }
            }

            // ── Expandable Date List ──────────────────────────────────────────
            item {
                Text(
                    "By Date",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            if (sortedDates.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No attendance records yet", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                }
            } else {
                items(sortedDates, key = { it.toEpochDay() }) { date ->
                    val presentIds = attendanceByDate[date.toEpochDay()] ?: emptyList()
                    val presentMembers = presentIds.mapNotNull { membersById[it] }
                    val isExpanded = expandedDate == date

                    Card(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(if (isExpanded) 3.dp else 1.dp)
                    ) {
                        Column {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { expandedDate = if (isExpanded) null else date }
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(date.format(dateFormatter), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        "${presentIds.size} of $totalMembers present",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Icon(
                                    if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    "Expand",
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                )
                            }
                            AnimatedVisibility(visible = isExpanded, enter = expandVertically(), exit = shrinkVertically()) {
                                Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 12.dp)) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
                                    Spacer(Modifier.height(8.dp))
                                    presentMembers.forEach { member ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            modifier = Modifier.padding(vertical = 3.dp)
                                        ) {
                                            Box(
                                                Modifier
                                                    .size(28.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    member.name.take(1).uppercase(),
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            }
                                            Text(member.name, style = MaterialTheme.typography.bodyMedium)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

private fun heatmapColor(fraction: Float, isValid: Boolean, emptyColor: Color, fullColor: Color): Color {
    if (!isValid || fraction == 0f) return emptyColor
    return lerp(emptyColor, fullColor, fraction.coerceIn(0.1f, 1f))
}
