package com.workouttracker.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.workouttracker.R
import com.workouttracker.data.model.BodyweightEntry
import com.workouttracker.ui.components.EmptyPlaceholder
import com.workouttracker.ui.viewmodel.WorkoutViewModel
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BodyweightScreen(viewModel: WorkoutViewModel) {
    val entries by viewModel.allBodyweightEntries.collectAsStateWithLifecycle(initialValue = emptyList())
    var showAddDialog by remember { mutableStateOf(false) }

    val latest  = entries.firstOrNull()
    val change  = if (entries.size >= 2) latest!!.weightKg - entries.last().weightKg else null

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick        = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor   = Color.White,
                shape          = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.log_weight), fontWeight = FontWeight.SemiBold)
            }
        }
    ) { padding ->
        LazyColumn(
            modifier        = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            contentPadding  = PaddingValues(top = 16.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(stringResource(R.string.bodyweight_title), style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold)
                Text(stringResource(R.string.track_weight),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // Summary cards
            if (latest != null) {
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        BodyweightStatCard(modifier = Modifier.weight(1f),
                            icon = Icons.Default.MonitorWeight, title = stringResource(R.string.current),
                            value = "${latest.weightKg} kg")
                        if (change != null) {
                            val isGain = change > 0
                            Card(modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(18.dp)) {
                                Column(modifier = Modifier.padding(18.dp)) {
                                    Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center) {
                                        Icon(if (isGain) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                            null, tint = if (isGain) Color(0xFFFF5252) else Color(0xFF4CAF50),
                                            modifier = Modifier.size(22.dp))
                                    }
                                    Spacer(Modifier.height(12.dp))
                                    Text(
                                        "${if (isGain) "+" else ""}${String.format("%.1f", change)} kg",
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = if (isGain) Color(0xFFFF5252) else Color(0xFF4CAF50),
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Text(stringResource(R.string.total_change), style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        } else {
                            BodyweightStatCard(modifier = Modifier.weight(1f),
                                icon = Icons.Default.CalendarToday, title = stringResource(R.string.active),
                                value = "${entries.size} days")
                        }
                    }
                }

                // Chart
                if (entries.size >= 2) {
                    item {
                        Card(modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(18.dp)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(stringResource(R.string.progress), style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface)
                                Spacer(Modifier.height(12.dp))
                                BodyweightChart(
                                    entries = entries.reversed(),
                                    lineColor = MaterialTheme.colorScheme.primary,
                                    gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                                )
                            }
                        }
                    }
                }
            }

            // History list
            item {
                Text(stringResource(R.string.history), style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            }

            if (entries.isEmpty()) {
                item {
                    EmptyPlaceholder(Icons.Default.MonitorWeight, stringResource(R.string.track_weight))
                }
            } else {
                items(entries, key = { it.date }) { entry ->
                    val prev = entries.getOrNull(entries.indexOf(entry) + 1)
                    val diff = if (prev != null) entry.weightKg - prev.weightKg else null
                    val localDate = runCatching { LocalDate.parse(entry.date) }.getOrNull()
                    val displayDate = localDate?.let {
                        "${it.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())}, " +
                        "${it.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())} ${it.dayOfMonth}, ${it.year}"
                    } ?: entry.date

                    Card(modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(14.dp)) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(displayDate, style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                                if (diff != null) {
                                    Text(
                                        "${if (diff > 0) "▲ +" else "▼ "}${String.format("%.1f", diff)} kg from last",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (diff > 0) Color(0xFFFF5252) else Color(0xFF4CAF50)
                                    )
                                }
                            }
                            Text("${entry.weightKg} kg",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.ExtraBold)
                            Spacer(Modifier.width(8.dp))
                            IconButton(onClick = { viewModel.deleteBodyweight(entry) },
                                modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Close, null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddBodyweightDialog(
            defaultDate = viewModel.todayDateString(),
            onDismiss   = { showAddDialog = false },
            onSave      = { date, weight ->
                viewModel.upsertBodyweight(date, weight)
                showAddDialog = false
            }
        )
    }
}

// ── Line chart ────────────────────────────────────────────────────────────────

@Composable
fun BodyweightChart(
    entries: List<BodyweightEntry>,
    lineColor: Color,
    gridColor: Color
) {
    if (entries.size < 2) return
    val weights = entries.map { it.weightKg }
    val minW = weights.min()
    val maxW = weights.max()
    val range = (maxW - minW).coerceAtLeast(1f)

    Canvas(modifier = Modifier.fillMaxWidth().height(160.dp)) {
        val w = size.width
        val h = size.height
        val padL = 8f; val padR = 8f; val padT = 8f; val padB = 8f
        val chartW = w - padL - padR
        val chartH = h - padT - padB

        // Horizontal grid lines (3)
        for (i in 0..2) {
            val y = padT + chartH * i / 2
            drawLine(gridColor, Offset(padL, y), Offset(w - padR, y), strokeWidth = 1f)
        }

        // Line path
        val path = Path()
        entries.forEachIndexed { idx, entry ->
            val x = padL + idx.toFloat() / (entries.size - 1) * chartW
            val y = padT + chartH * (1f - (entry.weightKg - minW) / range)
            if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, lineColor, style = Stroke(width = 3f))

        // Dots on recent 3 points
        entries.takeLast(3).forEachIndexed { i, entry ->
            val idx = entries.size - 3 + i
            val x = padL + idx.toFloat() / (entries.size - 1) * chartW
            val y = padT + chartH * (1f - (entry.weightKg - minW) / range)
            drawCircle(lineColor, radius = 5f, center = Offset(x, y))
        }
    }

    // Min/max labels
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween) {
        Text("${String.format("%.1f", minW)} kg", style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("${String.format("%.1f", maxW)} kg", style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun BodyweightStatCard(modifier: Modifier = Modifier, icon: ImageVector, title: String, value: String) {
    Card(modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp)) {
        Column(modifier = Modifier.padding(18.dp)) {
            Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text(value, style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.ExtraBold)
            Text(title, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBodyweightDialog(defaultDate: String, onDismiss: () -> Unit, onSave: (String, Float) -> Unit) {
    var weightText by remember { mutableStateOf("") }
    var dateText by remember { mutableStateOf(defaultDate) }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.log_weight), style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(20.dp))

                OutlinedTextField(
                    value = weightText,
                    onValueChange = { weightText = it },
                    label = { Text(stringResource(R.string.weight_kg)) },
                    placeholder = { Text("00.0") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = dateText,
                    onValueChange = { dateText = it },
                    label = { Text(stringResource(R.string.today)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = {
                        val w = weightText.toFloatOrNull() ?: 0f
                        if (w > 0) onSave(dateText, w)
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.save), fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = onDismiss, modifier = Modifier.padding(top = 4.dp)) {
                    Text(stringResource(R.string.cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
