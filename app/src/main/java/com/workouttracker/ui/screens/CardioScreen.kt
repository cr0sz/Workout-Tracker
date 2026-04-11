package com.workouttracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.workouttracker.R
import com.workouttracker.data.model.CARDIO_TYPES
import com.workouttracker.data.model.CardioSession
import com.workouttracker.ui.components.EmptyPlaceholder
import com.workouttracker.ui.viewmodel.WorkoutViewModel
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardioScreen(viewModel: WorkoutViewModel) {
    val sessions by viewModel.allCardioSessions.collectAsStateWithLifecycle(initialValue = emptyList())
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.log_cardio), fontWeight = FontWeight.SemiBold)
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                    stringResource(R.string.cardio_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.track_cardio),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
            }

            if (sessions.isEmpty()) {
                item {
                    EmptyPlaceholder(
                        icon = Icons.Default.DirectionsRun,
                        text = stringResource(R.string.no_cardio_yet)
                    )
                }
            } else {
                // Group by date header
                val grouped = sessions.groupBy { it.date }
                grouped.forEach { (date, daySessions) ->
                    item(key = "header_$date") {
                        val localDate = runCatching { LocalDate.parse(date) }.getOrNull()
                        val displayDate = localDate?.let {
                            val isToday = it == LocalDate.now()
                            val isYesterday = it == LocalDate.now().minusDays(1)
                            when {
                                isToday -> stringResource(R.string.today)
                                isYesterday -> stringResource(R.string.yesterday)
                                else -> "${it.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())}, " +
                                        "${it.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())} ${it.dayOfMonth}"
                            }
                        } ?: date

                        Text(
                            displayDate,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                    }

                    items(daySessions, key = { it.id }) { session ->
                        CardioSessionCard(
                            session = session,
                            onDelete = { viewModel.deleteCardioSession(session) }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddCardioDialog(
            defaultDate = viewModel.todayDateString(),
            onDismiss = { showAddDialog = false },
            onSave = { session ->
                viewModel.addCardioSession(session)
                showAddDialog = false
            }
        )
    }
}

// ── Cardio Session Card ───────────────────────────────────────────────────────

@Composable
fun CardioSessionCard(session: CardioSession, onDelete: () -> Unit) {
    val icon = when (session.type.lowercase()) {
        "walk"         -> Icons.Default.DirectionsWalk
        "run"          -> Icons.Default.DirectionsRun
        "cycle"        -> Icons.Default.DirectionsBike
        "swim"         -> Icons.Default.Pool
        "rowing"       -> Icons.Default.Rowing
        else           -> Icons.Default.FitnessCenter
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp))
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    session.type,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Stat chips row
                val pace = if (session.distanceKm != null && session.durationMinutes != null &&
                               session.distanceKm > 0f) {
                    val minPerKm = session.durationMinutes / session.distanceKm
                    val mins = minPerKm.toInt()
                    val secs = ((minPerKm - mins) * 60).toInt()
                    "%d:%02d /km".format(mins, secs)
                } else null

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    session.distanceKm?.let { StatChip("${it} km") }
                    session.durationMinutes?.let { StatChip("${it} min") }
                    pace?.let { StatChip(it) }
                    session.weightKg?.let { StatChip("${it} kg") }
                    session.calories?.let { StatChip("${it} kcal") }
                }

                if (session.notes.isNotBlank()) {
                    Text(
                        session.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                        maxLines = 2
                    )
                }
            }

            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Default.DeleteOutline,
                    contentDescription = stringResource(R.string.delete),
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun StatChip(label: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp
        )
    }
}

// ── Add Cardio Dialog ─────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCardioDialog(
    defaultDate: String,
    onDismiss: () -> Unit,
    onSave: (CardioSession) -> Unit
) {
    var selectedType by remember { mutableStateOf("Walk") }
    var distance by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var calories by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var typeDropdownExpanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 680.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            LazyColumn(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        stringResource(R.string.log_cardio),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(4.dp))
                }

                // Type picker
                item {
                    ExposedDropdownMenuBox(
                        expanded = typeDropdownExpanded,
                        onExpandedChange = { typeDropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedType,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.type)) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            shape = RoundedCornerShape(12.dp),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(typeDropdownExpanded) }
                        )
                        ExposedDropdownMenu(
                            expanded = typeDropdownExpanded,
                            onDismissRequest = { typeDropdownExpanded = false }
                        ) {
                            CARDIO_TYPES.forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type) },
                                    onClick = {
                                        selectedType = type
                                        typeDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Distance
                item {
                    OutlinedTextField(
                        value = distance,
                        onValueChange = { distance = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text(stringResource(R.string.distance_km)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Duration
                item {
                    OutlinedTextField(
                        value = duration,
                        onValueChange = { duration = it.filter(Char::isDigit) },
                        label = { Text(stringResource(R.string.duration_min)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Weight carried
                item {
                    OutlinedTextField(
                        value = weight,
                        onValueChange = { weight = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text(stringResource(R.string.weight_carried)) },
                        placeholder = { Text("e.g. 5 for a 5 kg weighted walk") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Calories
                item {
                    OutlinedTextField(
                        value = calories,
                        onValueChange = { calories = it.filter(Char::isDigit) },
                        label = { Text(stringResource(R.string.calories_burned)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Live pace preview
                val livePace = run {
                    val d = distance.toFloatOrNull()
                    val t = duration.toIntOrNull()
                    if (d != null && t != null && d > 0f) {
                        val minPerKm = t / d
                        val mins = minPerKm.toInt()
                        val secs = ((minPerKm - mins) * 60).toInt()
                        "%d:%02d min/km".format(mins, secs)
                    } else null
                }
                if (livePace != null) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.Speed,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                "Pace: $livePace",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Notes
                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text(stringResource(R.string.notes) + " — " + stringResource(R.string.optional)) },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp),
                        maxLines = 3,
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Buttons
                item {
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) { Text(stringResource(R.string.cancel)) }

                        Button(
                            onClick = {
                                onSave(
                                    CardioSession(
                                        date            = defaultDate,
                                        type            = selectedType,
                                        distanceKm      = distance.toFloatOrNull(),
                                        durationMinutes = duration.toIntOrNull(),
                                        weightKg        = weight.toFloatOrNull(),
                                        calories        = calories.toIntOrNull(),
                                        notes           = notes.trim()
                                    )
                                )
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) { Text(stringResource(R.string.save), fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }
    }
}
