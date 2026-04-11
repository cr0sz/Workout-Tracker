package com.workouttracker.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.workouttracker.R
import com.workouttracker.data.db.ExerciseCount
import com.workouttracker.data.db.VolumeEntry
import com.workouttracker.data.model.EXERCISE_CATEGORIES
import com.workouttracker.ui.viewmodel.WorkoutViewModel
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun HistoryScreen(viewModel: WorkoutViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(stringResource(R.string.history), stringResource(R.string.stats_title))

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp, 32.dp, 24.dp, 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(R.string.stats_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.ExtraBold
            )
        }

        // Tab Row
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab])
                        .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        ) {
            tabs.forEachIndexed { index, label ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            label,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        when (selectedTab) {
            0 -> WorkoutHistoryList(viewModel)
            1 -> StatsContent(viewModel)
        }
    }
}

@Composable
fun WorkoutHistoryList(viewModel: WorkoutViewModel) {
    val history by viewModel.allWorkouts.collectAsStateWithLifecycle(initialValue = emptyList())

    if (history.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.History,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    stringResource(R.string.no_workouts_yet),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(history) { workout ->
                HistoryCard(workout.date)
            }
        }
    }
}

@Composable
fun HistoryCard(date: String) {
    val localDate = remember(date) { LocalDate.parse(date) }
    val dayName = remember(localDate) { localDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault()) }
    val monthName = remember(localDate) { localDate.month.getDisplayName(TextStyle.SHORT, Locale.getDefault()) }
    val dayNum = localDate.dayOfMonth

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Date Circle
            Column(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    dayNum.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    monthName.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    dayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun StatsContent(viewModel: WorkoutViewModel) {
    val totalWorkouts by viewModel.totalWorkouts.collectAsStateWithLifecycle(initialValue = 0)
    val totalVolume by viewModel.totalVolume.collectAsStateWithLifecycle(initialValue = 0f)
    val personalRecords by viewModel.personalRecords.collectAsStateWithLifecycle(initialValue = emptyList())
    val volumeTrend by viewModel.volumeOverTime.collectAsStateWithLifecycle(initialValue = emptyList())
    val muscleDistribution by viewModel.muscleDistribution.collectAsStateWithLifecycle(initialValue = emptyList())
    val useLbs by viewModel.useLbs.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                stringResource(R.string.overview),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatSmallCard(
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.workouts),
                    value = totalWorkouts.toString(),
                    icon = Icons.Default.FitnessCenter,
                    color = Color(0xFF4CAF50)
                )
            }
        }

        item {
            val volumeValue = if (totalVolume == null) 0f else totalVolume!!
            val formattedVolume = if (useLbs) volumeValue * com.workouttracker.ui.util.WeightUnit.KG_TO_LBS else volumeValue
            val unitLabel = if (useLbs) "lbs" else "kg"
            
            StatLongCard(
                title = stringResource(R.string.total_volume_lifted),
                value = "${String.format("%,.0f", formattedVolume.toDouble())} $unitLabel",
                subtitle = "Sum of all sets across all workouts",
                icon = Icons.AutoMirrored.Filled.TrendingUp,
                color = Color(0xFFFF9800)
            )
        }

        // ── Volume Trend Graph ──
        if (volumeTrend.size >= 2) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            "Volume Trend",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "Total volume lifted per session over time",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(20.dp))
                        VolumeChart(
                            entries = volumeTrend,
                            useLbs = useLbs,
                            lineColor = Color(0xFFFF9800),
                            gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                        )
                    }
                }
            }
        }

        // ── Muscle Group Heatmap (Sets Distribution) ──
        if (muscleDistribution.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            "Muscle Distribution",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "Breakdown of total sets per muscle group",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(20.dp))
                        
                        MuscleHeatmap(
                            muscleDistribution = muscleDistribution,
                            primaryColor = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        item {
            Text(
                stringResource(R.string.personal_records),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
            )
        }
        
        if (personalRecords.isEmpty()) {
            item {
                Text(
                    stringResource(R.string.no_data_yet),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }
        } else {
            items(personalRecords) { pr ->
                val weightValue = if (useLbs) pr.maxWeight * com.workouttracker.ui.util.WeightUnit.KG_TO_LBS else pr.maxWeight
                val unitLabel = if (useLbs) "lbs" else "kg"
                val formattedWeight = if (weightValue == Math.floor(weightValue.toDouble()).toFloat()) weightValue.toInt().toString()
                                      else String.format("%.1f", weightValue)
                
                PRCard(pr.exerciseName, "$formattedWeight $unitLabel", pr.date)
            }
        }
    }
}

@Composable
fun VolumeChart(
    entries: List<VolumeEntry>,
    useLbs: Boolean,
    lineColor: Color,
    gridColor: Color
) {
    val factor = if (useLbs) com.workouttracker.ui.util.WeightUnit.KG_TO_LBS else 1f
    val data = entries.map { it.volume * factor }
    val minV = 0f
    val maxV = (data.maxOrNull() ?: 1f) * 1.1f
    val range = maxV - minV

    Canvas(modifier = Modifier.fillMaxWidth().height(180.dp)) {
        val w = size.width
        val h = size.height
        val padL = 10f; val padR = 10f; val padT = 10f; val padB = 10f
        val chartW = w - padL - padR
        val chartH = h - padT - padB

        // Horizontal grid lines
        for (i in 0..3) {
            val y = padT + chartH * i / 3
            drawLine(gridColor, Offset(padL, y), Offset(w - padR, y), strokeWidth = 1f)
        }

        // Line path
        val path = Path()
        data.forEachIndexed { idx, value ->
            val x = padL + idx.toFloat() / (data.size - 1) * chartW
            val y = padT + chartH * (1f - (value - minV) / range)
            if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, lineColor, style = Stroke(width = 4f))

        // Recent dot
        val lastX = padL + chartW
        val lastY = padT + chartH * (1f - (data.last() - minV) / range)
        drawCircle(lineColor, radius = 6f, center = Offset(lastX, lastY))
    }

    Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween) {
        val unit = if (useLbs) "lbs" else "kg"
        Text("0 $unit", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("${String.format("%,.0f", maxV.toDouble())} $unit", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun MuscleHeatmap(
    muscleDistribution: List<ExerciseCount>,
    primaryColor: Color
) {
    val categoryCounts = EXERCISE_CATEGORIES.mapValues { (_, exercises) ->
        muscleDistribution.filter { it.exerciseName in exercises }.sumOf { it.count }
    }.filterValues { it > 0 }

    if (categoryCounts.isEmpty()) return

    val maxCount = categoryCounts.values.max().toFloat()

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        categoryCounts.forEach { (category, count) ->
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(category, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    Text("$count sets", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { count / maxCount },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = primaryColor,
                    trackColor = primaryColor.copy(alpha = 0.1f)
                )
            }
        }
    }
}

@Composable
fun StatSmallCard(modifier: Modifier, title: String, value: String, icon: ImageVector, color: Color) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun StatLongCard(title: String, value: String, subtitle: String, icon: ImageVector, color: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun PRCard(exercise: String, record: String, date: String) {
    val localDate = remember(date) { runCatching { LocalDate.parse(date) }.getOrNull() }
    val displayDate = remember(localDate) {
        localDate?.let {
            "${it.dayOfMonth} ${it.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())} ${it.year}"
        } ?: date
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(exercise, fontWeight = FontWeight.Bold)
                Text(displayDate, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                record,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
