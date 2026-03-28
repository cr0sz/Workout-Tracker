package com.workouttracker.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.workouttracker.data.model.ExerciseHistoryEntry
import com.workouttracker.ui.components.EmptyPlaceholder
import com.workouttracker.ui.util.WeightUnit
import com.workouttracker.ui.viewmodel.WorkoutViewModel
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseHistoryScreen(
    exerciseName: String,
    viewModel: WorkoutViewModel,
    onBack: () -> Unit
) {
    val useLbs by viewModel.useLbs.collectAsState()
    val history by viewModel.getExerciseHistory(exerciseName).collectAsState(initial = emptyList())

    val bestEver = history.maxByOrNull { it.maxWeight }
    val totalSessions = history.size
    val totalSets = history.sumOf { it.totalSets }

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
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Stats row
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    BodyweightStatCard(modifier = Modifier.weight(1f), icon = Icons.Default.CalendarToday,
                        title = "Sessions", value = totalSessions.toString())
                    BodyweightStatCard(modifier = Modifier.weight(1f), icon = Icons.Default.Layers,
                        title = "Total Sets", value = totalSets.toString())
                    if (bestEver != null) {
                        Card(modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(18.dp)) {
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

            // Progression chart
            if (history.size >= 2) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(18.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Weight Progression",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface)
                            Spacer(Modifier.height(4.dp))
                            Text("Max weight per session",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(12.dp))
                            ExerciseProgressionChart(
                                history   = history,
                                useLbs    = useLbs,
                                lineColor = MaterialTheme.colorScheme.primary,
                                gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)
                            )
                        }
                    }
                }
            }

            // Session list
            item {
                Text("Session History",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground)
            }

            if (history.isEmpty()) {
                item {
                    EmptyPlaceholder(Icons.Default.FitnessCenter,
                        "No weighted sets logged for this exercise yet")
                }
            } else {
                items(history.reversed()) { entry ->
                    val localDate = runCatching { LocalDate.parse(entry.workoutDate) }.getOrNull()
                    val displayDate = localDate?.let {
                        "${it.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())}, " +
                        "${it.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())} ${it.dayOfMonth} ${it.year}"
                    } ?: entry.workoutDate

                    val isBest = entry == bestEver

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isBest)
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                            else MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(displayDate,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface)
                                    if (isBest) {
                                        Box(modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0xFFFFD700).copy(alpha = 0.2f))
                                            .padding(horizontal = 5.dp, vertical = 2.dp)) {
                                            Text("PR", style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFFB8860B), fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                Text("${entry.totalSets} sets · ${entry.totalReps} total reps",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(WeightUnit.display(entry.maxWeight, useLbs),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (isBest) Color(0xFFFFD700) else MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.ExtraBold)
                                Text("top set", style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExerciseProgressionChart(
    history: List<ExerciseHistoryEntry>,
    useLbs: Boolean,
    lineColor: Color,
    gridColor: Color
) {
    val weights = history.map { if (useLbs) it.maxWeight * 2.20462f else it.maxWeight }
    val minW = weights.min()
    val maxW = weights.max()
    val range = (maxW - minW).coerceAtLeast(1f)

    Canvas(modifier = Modifier.fillMaxWidth().height(140.dp)) {
        val w = size.width; val h = size.height
        val padL = 8f; val padR = 8f; val padT = 8f; val padB = 8f
        val chartW = w - padL - padR; val chartH = h - padT - padB

        for (i in 0..2) {
            val y = padT + chartH * i / 2
            drawLine(gridColor, Offset(padL, y), Offset(w - padR, y), strokeWidth = 1f)
        }

        val path = Path()
        weights.forEachIndexed { idx, wt ->
            val x = padL + idx.toFloat() / (weights.size - 1) * chartW
            val y = padT + chartH * (1f - (wt - minW) / range)
            if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, lineColor, style = Stroke(width = 3f))

        // Filled area under curve
        val filledPath = Path().apply {
            addPath(path)
            lineTo(padL + chartW, padT + chartH)
            lineTo(padL, padT + chartH)
            close()
        }
        drawPath(filledPath, lineColor.copy(alpha = 0.1f))

        // Highlight best point
        val bestIdx = weights.indexOf(weights.max())
        val bx = padL + bestIdx.toFloat() / (weights.size - 1) * chartW
        val by = padT + chartH * (1f - (weights[bestIdx] - minW) / range)
        drawCircle(lineColor, radius = 6f, center = Offset(bx, by))
        drawCircle(Color.White, radius = 3f, center = Offset(bx, by))
    }

    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween) {
        Text(WeightUnit.display(if (useLbs) minW / 2.20462f else minW, useLbs),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(WeightUnit.display(if (useLbs) maxW / 2.20462f else maxW, useLbs),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold)
    }
}
