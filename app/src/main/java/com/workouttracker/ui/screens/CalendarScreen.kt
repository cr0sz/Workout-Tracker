package com.workouttracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.workouttracker.R
import com.workouttracker.ui.viewmodel.WorkoutViewModel
import com.workouttracker.ui.util.buildWorkoutSuggestion
import com.workouttracker.ui.util.WorkoutSuggestionType
import com.workouttracker.ui.util.WorkoutSuggestion
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun CalendarScreen(
    viewModel: WorkoutViewModel,
    onDayClick: (String) -> Unit
) {
    val workoutDates by viewModel.allWorkoutDates.collectAsState()
    val cardioDates  by viewModel.allCardioDates.collectAsState()
    val allWorkouts  by viewModel.allWorkouts.collectAsState()
    val today        = remember { LocalDate.now() }

    // derivedStateOf means these only recalculate when workoutDates/cardioDates actually change
    val workoutDateSet by remember { derivedStateOf { workoutDates.toSet() } }
    val cardioDateSet  by remember { derivedStateOf { cardioDates.toSet() } }

    var currentMonth by remember { mutableStateOf(YearMonth.now()) }

    // Smart workout suggestion card
    val suggestion = remember(workoutDates) {
        buildWorkoutSuggestion(workoutDates)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                stringResource(R.string.calendar_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.ExtraBold
            )
        }

        if (suggestion != null) {
            item {
                SmartSuggestionCard(suggestion = suggestion)
            }
        }

        // ── Calendar card ────────────────────────────────────────────────────
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {

                    // Month navigation
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
                            Icon(
                                Icons.Default.ChevronLeft,
                                contentDescription = "Previous month",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = "${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${currentMonth.year}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) {
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = "Next month",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Day-of-week headers (Mon … Sun)
                    Row(modifier = Modifier.fillMaxWidth()) {
                        listOf("M", "T", "W", "T", "F", "S", "S").forEach { label ->
                            Text(
                                text = label,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    // Grid — only recalculates when currentMonth changes
                    val firstDayOfMonth = remember(currentMonth) { currentMonth.atDay(1) }
                    val startOffset     = remember(currentMonth) { firstDayOfMonth.dayOfWeek.value - 1 }
                    val daysInMonth     = remember(currentMonth) { currentMonth.lengthOfMonth() }
                    val rows            = remember(currentMonth) { ((startOffset + daysInMonth) + 6) / 7 }

                    for (row in 0 until rows) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            for (col in 0 until 7) {
                                val dayNum = row * 7 + col - startOffset + 1
                                if (dayNum < 1 || dayNum > daysInMonth) {
                                    Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                                } else {
                                    val date = currentMonth.atDay(dayNum)
                                    val dateStr = date.toString()
                                    val isToday = date == today
                                    val hasWorkout = dateStr in workoutDateSet
                                    val hasCardio = dateStr in cardioDateSet
                                    val isFuture = date.isAfter(today)

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .padding(2.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when {
                                                    isToday -> MaterialTheme.colorScheme.primary
                                                    hasWorkout -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                                    else -> Color.Transparent
                                                }
                                            )
                                            .clickable(enabled = !isFuture) { onDayClick(dateStr) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = dayNum.toString(),
                                                style = MaterialTheme.typography.bodySmall,
                                                fontSize = 13.sp,
                                                color = when {
                                                    isToday -> Color.White
                                                    isFuture -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                                    hasWorkout -> MaterialTheme.colorScheme.primary
                                                    else -> MaterialTheme.colorScheme.onSurface
                                                },
                                                fontWeight = if (isToday || hasWorkout) FontWeight.Bold else FontWeight.Normal
                                            )
                                            if ((hasWorkout || hasCardio) && !isToday) {
                                                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                                    if (hasWorkout) {
                                                        Box(modifier = Modifier.size(4.dp).clip(CircleShape)
                                                            .background(MaterialTheme.colorScheme.primary))
                                                    }
                                                    if (hasCardio) {
                                                        Box(modifier = Modifier.size(4.dp).clip(CircleShape)
                                                            .background(com.workouttracker.ui.theme.SuccessGreen))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── Recent workouts ──────────────────────────────────────────────────
        item {
            Text(
                stringResource(R.string.recent_workouts),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        if (allWorkouts.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.FitnessCenter,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            stringResource(R.string.no_workouts_yet),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            stringResource(R.string.tap_to_log),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(allWorkouts.take(7)) { workout ->
                val localDate = remember(workout.date) {
                    runCatching { LocalDate.parse(workout.date) }.getOrNull()
                }
                val displayDate = localDate?.let {
                    "${it.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())}, " +
                    "${it.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())} ${it.dayOfMonth}"
                } ?: workout.date

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onDayClick(workout.date) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                localDate?.dayOfMonth?.toString() ?: "-",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                displayDate,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (workout.notes.isNotBlank()) {
                                Text(
                                    workout.notes,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item { Spacer(Modifier.height(8.dp)) }
    }
}

// ── Smart Suggestion Card ─────────────────────────────────────────────────────

@Composable
fun SmartSuggestionCard(suggestion: WorkoutSuggestion) {
    val bgColor = when (suggestion.type) {
        WorkoutSuggestionType.STREAK       -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        WorkoutSuggestionType.REST_REMINDER-> androidx.compose.ui.graphics.Color(0xFFFF9800).copy(alpha = 0.1f)
        WorkoutSuggestionType.MUSCLE_GROUP -> androidx.compose.ui.graphics.Color(0xFF4CAF50).copy(alpha = 0.1f)
        WorkoutSuggestionType.GENERAL      -> MaterialTheme.colorScheme.surface
    }
    val textColor = when (suggestion.type) {
        WorkoutSuggestionType.STREAK       -> MaterialTheme.colorScheme.primary
        WorkoutSuggestionType.REST_REMINDER-> androidx.compose.ui.graphics.Color(0xFFFF9800)
        else                               -> MaterialTheme.colorScheme.onSurface
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape  = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(suggestion.emoji, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    suggestion.title,
                    style      = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color      = textColor
                )
                Text(
                    suggestion.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
