package com.workouttracker.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.workouttracker.R
import com.workouttracker.data.model.ALL_PROGRAMS
import com.workouttracker.data.model.Difficulty
import com.workouttracker.data.model.ProgramCategory
import com.workouttracker.data.model.ProgramTemplate
import com.workouttracker.ui.viewmodel.WorkoutViewModel

@Composable
fun ProgramsScreen(
    viewModel: WorkoutViewModel,
    onProgramClick: (String) -> Unit
) {
    var selectedCategory by remember { mutableStateOf<ProgramCategory?>(null) }
    val allProgress by viewModel.allProgramProgress.collectAsStateWithLifecycle(initialValue = emptyList())

    val filtered = remember(selectedCategory) {
        if (selectedCategory == null) ALL_PROGRAMS
        else ALL_PROGRAMS.filter { it.category == selectedCategory }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        // Header
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
                Text(
                    stringResource(R.string.programs_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    stringResource(R.string.choose_program),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Category filter chips
        item {
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // "All" chip
                FilterChipItem(
                    label    = stringResource(R.string.overview),
                    emoji    = "🏆",
                    selected = selectedCategory == null,
                    onClick  = { selectedCategory = null }
                )
                ProgramCategory.entries.forEach { cat ->
                    FilterChipItem(
                        label    = cat.label,
                        emoji    = cat.emoji,
                        selected = selectedCategory == cat,
                        onClick  = { selectedCategory = if (selectedCategory == cat) null else cat }
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // Program cards grouped by category (when no filter selected)
        if (selectedCategory == null) {
            ProgramCategory.entries.forEach { cat ->
                val progs = ALL_PROGRAMS.filter { it.category == cat }
                item(key = "header_${cat.name}") {
                    CategoryHeader(cat)
                }
                items(progs, key = { it.id }) { program ->
                    ProgramCard(
                        program  = program,
                        progress = allProgress.find { it.programId == program.id },
                        modifier = Modifier.padding(horizontal = 16.dp),
                        onClick  = { onProgramClick(program.id) }
                    )
                    Spacer(Modifier.height(10.dp))
                }
                item { Spacer(Modifier.height(6.dp)) }
            }
        } else {
            items(filtered, key = { it.id }) { program ->
                ProgramCard(
                    program  = program,
                    progress = allProgress.find { it.programId == program.id },
                    modifier = Modifier.padding(horizontal = 16.dp),
                    onClick  = { onProgramClick(program.id) }
                )
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

// ── Category Header ───────────────────────────────────────────────────────────

@Composable
fun CategoryHeader(cat: ProgramCategory) {
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(cat.emoji, fontSize = 20.sp)
        Spacer(Modifier.width(8.dp))
        Text(
            cat.label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

// ── Filter Chip ───────────────────────────────────────────────────────────────

@Composable
fun FilterChipItem(label: String, emoji: String, selected: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surface,
        label = "chip_bg"
    )
    val textColor by animateColorAsState(
        if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
        label = "chip_text"
    )

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50.dp))
            .background(bg)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(emoji, fontSize = 14.sp)
            Text(label, color = textColor, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                style = MaterialTheme.typography.bodyMedium)
        }
    }
}

// ── Program Card ──────────────────────────────────────────────────────────────

@Composable
fun ProgramCard(
    program: ProgramTemplate,
    progress: com.workouttracker.data.model.ProgramProgress? = null,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val difficultyColor = when (program.difficulty) {
        Difficulty.BEGINNER     -> Color(0xFF4CAF50)
        Difficulty.INTERMEDIATE -> Color(0xFFFF9800)
        Difficulty.ADVANCED     -> Color(0xFFFF5252)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Top row: emoji + name + difficulty
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(program.category.emoji, fontSize = 28.sp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            program.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            program.tagline,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                // Difficulty badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(difficultyColor.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    val diffLabel = when(program.difficulty) {
                        Difficulty.BEGINNER -> stringResource(R.string.difficulty_beginner)
                        Difficulty.INTERMEDIATE -> stringResource(R.string.difficulty_intermediate)
                        Difficulty.ADVANCED -> stringResource(R.string.difficulty_advanced)
                    }
                    Text(
                        diffLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = difficultyColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MiniStat(Icons.Default.CalendarToday, "${program.durationWeeks} " + stringResource(R.string.weeks))
                MiniStat(Icons.Default.Repeat, "${program.daysPerWeek}" + stringResource(R.string.per_week))
                MiniStat(Icons.Default.FitnessCenter, program.category.label)
            }

            // Active program progress banner
            if (progress != null) {
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PlayCircle, null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.active) + " — " + stringResource(R.string.weeks) + " ${progress.currentWeek}, " + stringResource(R.string.today) + " ${progress.currentDay}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold)
                    }
                    if (progress.lastSessionDate.isNotBlank()) {
                        Text(stringResource(R.string.last_session) + ": ${progress.lastSessionDate}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                program.days.take(7).forEach { day ->
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (day.isRestDay) MaterialTheme.colorScheme.surfaceVariant
                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "D${day.dayNumber}",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
                            color = if (day.isRestDay) MaterialTheme.colorScheme.onSurfaceVariant
                                    else MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // CTA row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.choose_program),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun MiniStat(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(14.dp))
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
