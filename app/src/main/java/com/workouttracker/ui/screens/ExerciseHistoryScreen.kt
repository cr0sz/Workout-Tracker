package com.workouttracker.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.workouttracker.data.model.ExerciseHistoryEntry
import com.workouttracker.data.model.ExerciseSetEntry
import com.workouttracker.ui.components.EmptyPlaceholder
import com.workouttracker.ui.util.WeightUnit
import com.workouttracker.ui.viewmodel.WorkoutViewModel
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.IsoFields
import java.util.Locale

// ── Data helpers ──────────────────────────────────────────────────────────────

private data class WeekGroup(
    val weekKey: String,               // "2024-W03" — for sorting
    val label: String,                 // "Jan 15" — Monday of that week
    val maxWeight: Float,
    val sessions: List<ExerciseHistoryEntry>
)

private data class SessionGroup(
    val date: String,
    val displayDate: String,
    val sets: List<ExerciseSetEntry>,
    val maxWeight: Float,
    val minWeight: Float
)

private fun buildWeekGroups(history: List<ExerciseHistoryEntry>): List<WeekGroup> =
    history.groupBy { entry ->
        val d = runCatching { LocalDate.parse(entry.workoutDate) }.getOrNull() ?: return@groupBy "?"
        val yr = d.get(IsoFields.WEEK_BASED_YEAR)
        val wk = d.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
        "$yr-${wk.toString().padStart(2, '0')}"
    }.map { (key, entries) ->
        val monday = runCatching {
            val d = LocalDate.parse(entries.first().workoutDate)
            val wk = d.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
            val yr = d.get(IsoFields.WEEK_BASED_YEAR)
            LocalDate.ofYearDay(yr, 1)
                .with(IsoFields.WEEK_OF_WEEK_BASED_YEAR, wk.toLong())
                .with(java.time.DayOfWeek.MONDAY)
        }.getOrNull()
        val label = monday?.let {
            "${it.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())} ${it.dayOfMonth}"
        } ?: key
        WeekGroup(
            weekKey    = key,
            label      = label,
            maxWeight  = entries.maxOf { it.maxWeight },
            sessions   = entries.sortedBy { it.workoutDate }
        )
    }.sortedBy { it.weekKey }

private fun buildSessionGroups(sets: List<ExerciseSetEntry>, useLbs: Boolean): List<SessionGroup> =
    sets.groupBy { it.workoutDate }.map { (date, entries) ->
        val d = runCatching { LocalDate.parse(date) }.getOrNull()
        val display = d?.let {
            "${it.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())}, " +
            "${it.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())} ${it.dayOfMonth} ${it.year}"
        } ?: date
        val weights = entries.map { if (useLbs) it.weight * 2.20462f else it.weight }
        SessionGroup(
            date        = date,
            displayDate = display,
            sets        = entries.sortedBy { it.setNumber },
            maxWeight   = weights.max(),
            minWeight   = weights.min()
        )
    }.sortedBy { it.date }

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseHistoryScreen(
    exerciseName: String,
    viewModel: WorkoutViewModel,
    onBack: () -> Unit
) {
    val useLbs  by viewModel.useLbs.collectAsState()
    val history by viewModel.getExerciseHistory(exerciseName).collectAsState(initial = emptyList())
    val allSets by viewModel.getExerciseSetHistory(exerciseName).collectAsState(initial = emptyList())

    val bestEver      = history.maxByOrNull { it.maxWeight }
    val totalSessions = history.size
    val totalSets     = history.sumOf { it.totalSets }

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Weekly", "All Sets")

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(exerciseName, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Stats row ─────────────────────────────────────────────────────
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ExHistStatCard(Modifier.weight(1f), Icons.Default.CalendarToday, "Sessions", totalSessions.toString())
                    ExHistStatCard(Modifier.weight(1f), Icons.Default.Layers,        "Total Sets", totalSets.toString())
                    if (bestEver != null) {
                        Card(
                            modifier = Modifier.weight(1f),
                            colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape    = RoundedCornerShape(18.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Icon(Icons.Default.EmojiEvents, null,
                                    tint = Color(0xFFFFD700), modifier = Modifier.size(20.dp))
                                Spacer(Modifier.height(6.dp))
                                Text(WeightUnit.display(bestEver.maxWeight, useLbs),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.ExtraBold)
                                Text("Best", style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            // ── Tabs ──────────────────────────────────────────────────────────
            item {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor   = MaterialTheme.colorScheme.surface,
                    contentColor     = MaterialTheme.colorScheme.primary,
                    modifier         = Modifier.clip(RoundedCornerShape(14.dp))
                ) {
                    tabs.forEachIndexed { i, title ->
                        Tab(
                            selected = selectedTab == i,
                            onClick  = { selectedTab = i },
                            text     = { Text(title, fontWeight = if (selectedTab == i) FontWeight.Bold else FontWeight.Normal) }
                        )
                    }
                }
            }

            if (history.isEmpty()) {
                item {
                    EmptyPlaceholder(Icons.Default.FitnessCenter,
                        "No weighted sets logged for this exercise yet")
                }
                return@LazyColumn
            }

            // ── Weekly tab ────────────────────────────────────────────────────
            if (selectedTab == 0) {
                val weeks = buildWeekGroups(history)

                item {
                    WeeklyChartCard(weeks = weeks, useLbs = useLbs,
                        lineColor = MaterialTheme.colorScheme.primary,
                        gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f))
                }

                item {
                    Text("Week by Week",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground)
                }

                items(weeks.reversed()) { week ->
                    WeekCard(week = week, useLbs = useLbs, bestWeight = bestEver?.maxWeight ?: 0f)
                }
            }

            // ── All Sets tab ──────────────────────────────────────────────────
            if (selectedTab == 1) {
                val sessions = buildSessionGroups(allSets, useLbs)

                if (sessions.isNotEmpty()) {
                    item {
                        AllSetsChartCard(sessions = sessions, useLbs = useLbs,
                            primaryColor = MaterialTheme.colorScheme.primary,
                            gridColor    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f))
                    }
                }

                item {
                    Text("Session Breakdown",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground)
                }

                items(sessions.reversed()) { session ->
                    SessionSetCard(session = session, useLbs = useLbs, bestWeight = bestEver?.maxWeight ?: 0f)
                }
            }
        }
    }
}

// ── Weekly chart ──────────────────────────────────────────────────────────────

@Composable
private fun WeeklyChartCard(
    weeks: List<WeekGroup>,
    useLbs: Boolean,
    lineColor: Color,
    gridColor: Color
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape  = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Weekly Best Weight",
                style      = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.onSurface)
            Text("Max weight lifted each week",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))

            if (weeks.size < 2) {
                Text("Log at least 2 weeks to see the chart",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp).fillMaxWidth(),
                    textAlign = TextAlign.Center)
                return@Column
            }

            val colWidthDp: Dp = 56.dp
            val chartWidth    = (colWidthDp * weeks.size).coerceAtLeast(300.dp)

            val weights = weeks.map { if (useLbs) it.maxWeight * 2.20462f else it.maxWeight }
            val minW    = weights.min()
            val maxW    = weights.max()
            val bestIdx = weights.indexOf(maxW)

            Box(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                Column {
                    // Y-axis labels + chart
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Y axis
                        Column(
                            modifier = Modifier.height(140.dp).width(40.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(WeightUnit.format(if (useLbs) maxW else maxW, useLbs),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp)
                            Text(WeightUnit.format(if (useLbs) minW else minW, useLbs),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 9.sp)
                        }

                        Canvas(modifier = Modifier.width(chartWidth).height(140.dp)) {
                            val w = size.width; val h = size.height
                            val padT = 12f; val padB = 12f
                            val chartH = h - padT - padB
                            val range  = (maxW - minW).coerceAtLeast(1f)
                            val step   = w / weights.size

                            // Grid lines
                            for (i in 0..2) {
                                val y = padT + chartH * i / 2
                                drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
                            }

                            // Line
                            val path = Path()
                            weights.forEachIndexed { idx, wt ->
                                val x = step * idx + step / 2
                                val y = padT + chartH * (1f - (wt - minW) / range)
                                if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
                            }
                            drawPath(path, lineColor, style = Stroke(width = 3f))

                            // Fill
                            if (weights.size >= 2) {
                                val fill = Path().apply {
                                    addPath(path)
                                    lineTo(step * (weights.size - 1) + step / 2, padT + chartH)
                                    lineTo(step / 2, padT + chartH)
                                    close()
                                }
                                drawPath(fill, lineColor.copy(alpha = 0.1f))
                            }

                            // Dots + highlight best
                            weights.forEachIndexed { idx, wt ->
                                val x  = step * idx + step / 2
                                val y  = padT + chartH * (1f - (wt - minW) / range)
                                val r  = if (idx == bestIdx) 8f else 5f
                                drawCircle(lineColor, radius = r, center = Offset(x, y))
                                drawCircle(Color.White, radius = r - 3f, center = Offset(x, y))
                            }
                        }
                    }

                    // X axis week labels
                    Row(modifier = Modifier.padding(start = 40.dp)) {
                        weeks.forEach { week ->
                            Box(modifier = Modifier.width(colWidthDp),
                                contentAlignment = Alignment.TopCenter) {
                                Text(week.label,
                                    style    = MaterialTheme.typography.labelSmall,
                                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 9.sp,
                                    maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── All-sets range chart ──────────────────────────────────────────────────────

@Composable
private fun AllSetsChartCard(
    sessions: List<SessionGroup>,
    useLbs: Boolean,
    primaryColor: Color,
    gridColor: Color
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape  = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Set-by-Set Progression",
                style      = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.onSurface)
            Text("Each column = one session · dots = individual sets",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))

            // Legend
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LegendDot(primaryColor, "Max weight")
                LegendDot(primaryColor.copy(alpha = 0.35f), "Other sets")
            }
            Spacer(Modifier.height(8.dp))

            if (sessions.isEmpty()) return@Column

            val allWeights = sessions.flatMap { s ->
                s.sets.map { if (useLbs) it.weight * 2.20462f else it.weight }
            }
            val globalMin = allWeights.min()
            val globalMax = allWeights.max()
            val range     = (globalMax - globalMin).coerceAtLeast(1f)
            val bestMax   = sessions.maxOf { it.maxWeight }

            val colWidthDp: Dp = 56.dp
            val chartWidth     = (colWidthDp * sessions.size).coerceAtLeast(300.dp)

            Box(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Y axis
                        Column(
                            modifier = Modifier.height(180.dp).width(40.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(WeightUnit.format(if (useLbs) globalMax else globalMax, useLbs),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp)
                            Text(WeightUnit.format(if (useLbs) globalMin else globalMin, useLbs),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 9.sp)
                        }

                        Canvas(modifier = Modifier.width(chartWidth).height(180.dp)) {
                            val w     = size.width; val h = size.height
                            val padT  = 12f; val padB = 12f
                            val chartH = h - padT - padB
                            val step  = w / sessions.size

                            // Grid lines
                            for (i in 0..3) {
                                val y = padT + chartH * i / 3
                                drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
                            }

                            // Line connecting max weights across sessions
                            val maxPath = Path()
                            sessions.forEachIndexed { idx, session ->
                                val cx  = step * idx + step / 2
                                val maxW = if (useLbs) session.maxWeight else session.maxWeight
                                val y   = padT + chartH * (1f - (maxW - globalMin) / range)
                                if (idx == 0) maxPath.moveTo(cx, y) else maxPath.lineTo(cx, y)
                            }
                            drawPath(maxPath, primaryColor.copy(alpha = 0.6f), style = Stroke(width = 2f))

                            // Per-session: range bar + set dots
                            sessions.forEachIndexed { idx, session ->
                                val cx    = step * idx + step / 2
                                val maxW  = if (useLbs) session.maxWeight else session.maxWeight
                                val minW  = if (useLbs) session.minWeight else session.minWeight
                                val yMax  = padT + chartH * (1f - (maxW - globalMin) / range)
                                val yMin  = padT + chartH * (1f - (minW - globalMin) / range)

                                // Range line (min → max)
                                if (maxW != minW) {
                                    drawLine(
                                        color       = primaryColor.copy(alpha = 0.25f),
                                        start       = Offset(cx, yMax),
                                        end         = Offset(cx, yMin),
                                        strokeWidth = 4f
                                    )
                                }

                                // Individual set dots
                                session.sets.forEach { set ->
                                    val w2 = if (useLbs) set.weight * 2.20462f else set.weight
                                    val y  = padT + chartH * (1f - (w2 - globalMin) / range)
                                    val isMax = w2 == maxW && session.maxWeight == bestMax
                                    drawCircle(
                                        color  = if (w2 == maxW) primaryColor else primaryColor.copy(alpha = 0.35f),
                                        radius = if (w2 == maxW) 7f else 5f,
                                        center = Offset(cx, y)
                                    )
                                    if (w2 == maxW) drawCircle(Color.White, 3.5f, Offset(cx, y))
                                }
                            }
                        }
                    }

                    // X axis labels
                    Row(modifier = Modifier.padding(start = 40.dp)) {
                        sessions.forEach { session ->
                            val d   = runCatching { LocalDate.parse(session.date) }.getOrNull()
                            val lbl = d?.let {
                                "${it.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())} ${it.dayOfMonth}"
                            } ?: session.date.takeLast(5)
                            Box(modifier = Modifier.width(colWidthDp),
                                contentAlignment = Alignment.TopCenter) {
                                Text(lbl,
                                    style    = MaterialTheme.typography.labelSmall,
                                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 9.sp,
                                    maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Cards ─────────────────────────────────────────────────────────────────────

@Composable
private fun WeekCard(week: WeekGroup, useLbs: Boolean, bestWeight: Float) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape  = RoundedCornerShape(14.dp)
    ) {
        Column {
            // Header row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Week of ${week.label}",
                            style      = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color      = MaterialTheme.colorScheme.onSurface)
                        if (week.maxWeight == bestWeight && bestWeight > 0f) {
                            PrBadge()
                        }
                    }
                    Text("${week.sessions.size} session${if (week.sessions.size > 1) "s" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(WeightUnit.display(week.maxWeight, useLbs),
                        style      = MaterialTheme.typography.titleMedium,
                        color      = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.ExtraBold)
                    Text("week best", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = { expanded = !expanded }, modifier = Modifier.size(28.dp)) {
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint   = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Expanded sessions
            if (expanded) {
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                week.sessions.forEach { session ->
                    val d = runCatching { LocalDate.parse(session.workoutDate) }.getOrNull()
                    val dayLabel = d?.dayOfWeek?.getDisplayName(TextStyle.FULL, Locale.getDefault()) ?: session.workoutDate
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(dayLabel,
                                style  = MaterialTheme.typography.bodySmall,
                                color  = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium)
                            Text("${session.totalSets} sets · ${session.totalReps} reps",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(WeightUnit.display(session.maxWeight, useLbs),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun SessionSetCard(session: SessionGroup, useLbs: Boolean, bestWeight: Float) {
    val isBestSession = session.maxWeight == bestWeight && bestWeight > 0f

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isBestSession)
                MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
            else
                MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            // Date + PR badge
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(session.displayDate,
                    style      = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color      = MaterialTheme.colorScheme.onSurface,
                    modifier   = Modifier.weight(1f))
                if (isBestSession) PrBadge()
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))

            // Individual sets
            session.sets.forEachIndexed { idx, set ->
                val w        = if (useLbs) set.weight * 2.20462f else set.weight
                val isTopSet = w == session.maxWeight
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Set number indicator
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (isTopSet) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("${idx + 1}",
                            style      = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color      = if (isTopSet) MaterialTheme.colorScheme.primary
                                         else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize   = 10.sp)
                    }
                    Spacer(Modifier.width(10.dp))
                    Text("${set.reps} reps",
                        style    = MaterialTheme.typography.bodySmall,
                        color    = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f))
                    Text(WeightUnit.display(set.weight, useLbs),
                        style      = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isTopSet) FontWeight.ExtraBold else FontWeight.Normal,
                        color      = if (isTopSet) MaterialTheme.colorScheme.primary
                                     else MaterialTheme.colorScheme.onSurface)
                    if (isTopSet) {
                        Spacer(Modifier.width(4.dp))
                        Text("top", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
                    }
                }
            }
        }
    }
}

// ── Small reusable composables ────────────────────────────────────────────────

@Composable
private fun ExHistStatCard(modifier: Modifier, icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, value: String) {
    Card(modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape  = RoundedCornerShape(18.dp)) {
        Column(modifier = Modifier.padding(14.dp)) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(6.dp))
            Text(value, style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold)
            Text(title, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PrBadge() {
    Box(modifier = Modifier
        .clip(RoundedCornerShape(4.dp))
        .background(Color(0xFFFFD700).copy(alpha = 0.2f))
        .padding(horizontal = 5.dp, vertical = 2.dp)) {
        Text("PR", style = MaterialTheme.typography.labelSmall,
            color = Color(0xFFB8860B), fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(modifier = Modifier
            .size(8.dp)
            .clip(RoundedCornerShape(50))
            .background(color))
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
