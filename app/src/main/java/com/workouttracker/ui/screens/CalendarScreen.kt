package com.workouttracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.workouttracker.R
import com.workouttracker.data.model.CardioSession
import com.workouttracker.data.model.WorkoutExercise
import com.workouttracker.ui.theme.SuccessGreen
import com.workouttracker.ui.util.WorkoutSuggestion
import com.workouttracker.ui.util.WorkoutSuggestionType
import com.workouttracker.ui.util.buildWorkoutSuggestion
import com.workouttracker.ui.viewmodel.WorkoutViewModel
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
    val allCardio    by viewModel.allCardioSessions.collectAsState()
    val today        = remember { LocalDate.now() }

    val workoutDateSet by remember { derivedStateOf { workoutDates.toSet() } }
    val cardioDateSet  by remember { derivedStateOf { cardioDates.toSet() } }

    var currentMonth by remember { mutableStateOf(YearMonth.now()) }

    // Month range strings — used to load exercises and filter cardio for the recap
    val monthStart = remember(currentMonth) { currentMonth.atDay(1).toString() }
    val monthEnd   = remember(currentMonth) { currentMonth.atEndOfMonth().toString() }

    val monthExercises by remember(monthStart, monthEnd) {
        viewModel.getExercisesInRange(monthStart, monthEnd)
    }.collectAsState(initial = emptyList())

    val exercisesByDate = remember(monthExercises) {
        monthExercises.groupBy { it.workoutDate }
    }
    val monthCardioByDate = remember(allCardio, monthStart, monthEnd) {
        allCardio
            .filter { it.date >= monthStart && it.date <= monthEnd }
            .groupBy { it.date }
    }
    val activeDatesInMonth = remember(exercisesByDate, monthCardioByDate) {
        (exercisesByDate.keys + monthCardioByDate.keys).toSortedSet()
    }

    var selectedDate by remember { mutableStateOf<String?>(null) }
    val effectiveSelected = selectedDate ?: today.toString()

    // Collect workout content for the selected day
    val selectedExercises by remember(effectiveSelected) {
        viewModel.getExercisesForDate(effectiveSelected)
    }.collectAsState(initial = emptyList())

    val selectedCardio by remember(effectiveSelected) {
        viewModel.getCardioForDate(effectiveSelected)
    }.collectAsState(initial = emptyList())

    val suggestion = remember(workoutDates) { buildWorkoutSuggestion(workoutDates) }

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
            item { SmartSuggestionCard(suggestion = suggestion) }
        }

        // ── Calendar grid ─────────────────────────────────────────────────────
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
                            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous month",
                                tint = MaterialTheme.colorScheme.primary)
                        }
                        Text(
                            text = "${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${currentMonth.year}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "Next month",
                                tint = MaterialTheme.colorScheme.primary)
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
                                    val date       = currentMonth.atDay(dayNum)
                                    val dateStr    = date.toString()
                                    val isToday    = date == today
                                    val hasWorkout = dateStr in workoutDateSet
                                    val hasCardio  = dateStr in cardioDateSet
                                    val isFuture   = date.isAfter(today)
                                    val isSelected = dateStr == effectiveSelected && !isToday

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .padding(2.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when {
                                                    isToday    -> MaterialTheme.colorScheme.primary
                                                    isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                                                    hasWorkout -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                                    hasCardio  -> SuccessGreen.copy(alpha = 0.12f)
                                                    else       -> Color.Transparent
                                                }
                                            )
                                            .clickable(enabled = !isFuture) {
                                                selectedDate = dateStr
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = dayNum.toString(),
                                                style = MaterialTheme.typography.bodySmall,
                                                fontSize = 13.sp,
                                                color = when {
                                                    isToday    -> Color.White
                                                    isFuture   -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                                    isSelected -> MaterialTheme.colorScheme.primary
                                                    hasWorkout -> MaterialTheme.colorScheme.primary
                                                    else       -> MaterialTheme.colorScheme.onSurface
                                                },
                                                fontWeight = if (isToday || hasWorkout || isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                            if ((hasWorkout || hasCardio) && !isToday) {
                                                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                                    if (hasWorkout) Box(
                                                        modifier = Modifier.size(5.dp).clip(CircleShape)
                                                            .background(MaterialTheme.colorScheme.primary)
                                                    )
                                                    if (hasCardio) Box(
                                                        modifier = Modifier.size(5.dp).clip(CircleShape)
                                                            .background(SuccessGreen)
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
            }
        }

        // ── Selected day detail ───────────────────────────────────────────────
        item {
            DayDetailCard(
                dateStr        = effectiveSelected,
                exercises      = selectedExercises,
                cardioSessions = selectedCardio,
                onOpenWorkout  = { onDayClick(effectiveSelected) }
            )
        }

        // ── Month recap list ──────────────────────────────────────────────────
        if (activeDatesInMonth.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "${currentMonth.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())} recap",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "${activeDatesInMonth.size} sessions",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(Modifier.height(10.dp))

                        activeDatesInMonth.forEachIndexed { index, dateStr ->
                            key(dateStr) {
                                val date = runCatching { LocalDate.parse(dateStr) }.getOrNull()
                                if (date != null) {
                                    MonthActivityRow(
                                        date           = date,
                                        exercises      = exercisesByDate[dateStr] ?: emptyList(),
                                        cardioSessions = monthCardioByDate[dateStr] ?: emptyList(),
                                        isSelected     = dateStr == effectiveSelected,
                                        onClick        = { selectedDate = dateStr }
                                    )
                                    if (index < activeDatesInMonth.size - 1) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(vertical = 6.dp),
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(8.dp)) }
    }
}

// ── Month activity row ────────────────────────────────────────────────────────

@Composable
private fun MonthActivityRow(
    date: LocalDate,
    exercises: List<WorkoutExercise>,
    cardioSessions: List<CardioSession>,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                else Color.Transparent
            )
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Date badge
        Column(
            modifier = Modifier.width(44.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(3),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp
            )
            Text(
                date.dayOfMonth.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface
            )
        }

        Box(
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .width(1.dp)
                .height(32.dp)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
        )

        // Activity summary
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            if (exercises.isNotEmpty()) {
                val names = exercises.take(3).joinToString(", ") { it.exerciseName }
                val overflow = if (exercises.size > 3) " +${exercises.size - 3}" else ""
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.FitnessCenter,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        "$names$overflow",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            cardioSessions.forEach { session ->
                key(session.id) {
                    val detail = buildList {
                        session.distanceKm?.let { add("${"%.1f".format(it)} km") }
                        session.durationMinutes?.let { add("${it} min") }
                    }.joinToString(" · ")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.AutoMirrored.Filled.DirectionsRun,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = SuccessGreen
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            if (detail.isNotEmpty()) "${session.type} · $detail" else session.type,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

// ── Day Detail Card ───────────────────────────────────────────────────────────

@Composable
fun DayDetailCard(
    dateStr: String,
    exercises: List<WorkoutExercise>,
    cardioSessions: List<CardioSession>,
    onOpenWorkout: () -> Unit
) {
    val today = remember { LocalDate.now() }
    val date  = remember(dateStr) { runCatching { LocalDate.parse(dateStr) }.getOrNull() }

    val isToday   = date == today
    val isFuture  = date?.isAfter(today) == true
    val hasContent = exercises.isNotEmpty() || cardioSessions.isNotEmpty()

    val formattedDate = remember(date) {
        date?.let {
            "${it.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())}, " +
            "${it.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${it.dayOfMonth}"
        } ?: dateStr
    }

    Card(
        modifier  = Modifier.fillMaxWidth(),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape     = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (isToday) {
                        Text(
                            "Today",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                if (!isFuture) {
                    FilledTonalButton(
                        onClick = onOpenWorkout,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(
                            Icons.Default.FitnessCenter,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            if (hasContent) "Open" else "Log workout",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }

            when {
                isFuture -> Text(
                    "Future date",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                !hasContent -> Text(
                    "No workout logged",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                else -> {
                    // ── Strength exercises ────────────────────────────────────
                    if (exercises.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.FitnessCenter,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(5.dp))
                                Text(
                                    "Strength",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            exercises.forEach { exercise ->
                                key(exercise.id) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            exercise.exerciseName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ── Cardio sessions ───────────────────────────────────────
                    if (cardioSessions.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.AutoMirrored.Filled.DirectionsRun,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp),
                                    tint = SuccessGreen
                                )
                                Spacer(Modifier.width(5.dp))
                                Text(
                                    "Cardio",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = SuccessGreen,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            cardioSessions.forEach { session ->
                                key(session.id) {
                                val detail = buildList {
                                    session.durationMinutes?.let { add("${it}min") }
                                    session.distanceKm?.let { add("${"%.1f".format(it)}km") }
                                    session.calories?.let { add("${it}kcal") }
                                }.joinToString(" · ")

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(SuccessGreen.copy(alpha = 0.5f))
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        if (detail.isNotEmpty()) "${session.type}  ·  $detail" else session.type,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
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
}

// ── Smart Suggestion Card ─────────────────────────────────────────────────────

@Composable
fun SmartSuggestionCard(suggestion: WorkoutSuggestion) {
    val bgColor = when (suggestion.type) {
        WorkoutSuggestionType.STREAK        -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        WorkoutSuggestionType.REST_REMINDER -> Color(0xFFFF9800).copy(alpha = 0.1f)
        WorkoutSuggestionType.MUSCLE_GROUP  -> Color(0xFF4CAF50).copy(alpha = 0.1f)
        WorkoutSuggestionType.GENERAL       -> MaterialTheme.colorScheme.surface
    }
    val textColor = when (suggestion.type) {
        WorkoutSuggestionType.STREAK        -> MaterialTheme.colorScheme.primary
        WorkoutSuggestionType.REST_REMINDER -> Color(0xFFFF9800)
        else                                -> MaterialTheme.colorScheme.onSurface
    }

    Card(
        colors    = CardDefaults.cardColors(containerColor = bgColor),
        shape     = RoundedCornerShape(14.dp),
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
