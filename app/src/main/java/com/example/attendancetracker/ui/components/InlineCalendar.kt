package com.example.attendancetracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Composable
fun InlineCalendar(
    selectedDate: LocalDate,
    markedDates: Set<Long> = emptySet(), // epochDays that have attendance records
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    var displayMonth by remember { mutableStateOf(YearMonth.from(selectedDate)) }
    val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")
    val dayHeaders = listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa")

    // Build grid: pad with nulls before the first day
    val firstDay = displayMonth.atDay(1)
    val startOffset = firstDay.dayOfWeek.value % 7 // Sunday=0
    val daysInMonth = displayMonth.lengthOfMonth()
    val totalCells = startOffset + daysInMonth
    val rows = (totalCells + 6) / 7

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp)
    ) {
        // Month navigation header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = { displayMonth = displayMonth.minusMonths(1) }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Previous month", tint = MaterialTheme.colorScheme.onSurface)
            }
            Text(
                text = displayMonth.format(monthFormatter),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = { displayMonth = displayMonth.plusMonths(1) }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Next month", tint = MaterialTheme.colorScheme.onSurface)
            }
        }

        Spacer(Modifier.height(8.dp))

        // Day-of-week headers
        Row(Modifier.fillMaxWidth()) {
            dayHeaders.forEach { header ->
                Text(
                    text = header,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        // Calendar grid
        for (row in 0 until rows) {
            Row(Modifier.fillMaxWidth()) {
                for (col in 0 until 7) {
                    val cellIndex = row * 7 + col
                    val dayNum = cellIndex - startOffset + 1
                    val isValidDay = dayNum in 1..daysInMonth
                    val cellDate = if (isValidDay) displayMonth.atDay(dayNum) else null
                    val isSelected = cellDate == selectedDate
                    val isToday = cellDate == LocalDate.now()
                    val hasAttendance = cellDate?.let { markedDates.contains(it.toEpochDay()) } ?: false

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .padding(2.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isSelected -> MaterialTheme.colorScheme.primary
                                    isToday -> MaterialTheme.colorScheme.primaryContainer
                                    else -> MaterialTheme.colorScheme.surface
                                }
                            )
                            .then(
                                if (isValidDay) Modifier.clickable { onDateSelected(cellDate!!) }
                                else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isValidDay) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = dayNum.toString(),
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                    color = when {
                                        isSelected -> MaterialTheme.colorScheme.onPrimary
                                        isToday -> MaterialTheme.colorScheme.onPrimaryContainer
                                        else -> MaterialTheme.colorScheme.onSurface
                                    }
                                )
                                // Tiny dot if there are attendance records for this day
                                if (hasAttendance && !isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(4.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
