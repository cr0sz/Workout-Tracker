package com.workouttracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.workouttracker.data.model.WorkoutTemplate
import com.workouttracker.ui.components.EmptyPlaceholder
import com.workouttracker.ui.viewmodel.WorkoutViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutTemplatesScreen(viewModel: WorkoutViewModel) {
    val templates by viewModel.allTemplates.collectAsState()
    val scope = rememberCoroutineScope()
    var showLoadDialog by remember { mutableStateOf<WorkoutTemplate?>(null) }
    var showPreviewDialog by remember { mutableStateOf<WorkoutTemplate?>(null) }
    val snackState = remember { SnackbarHostState() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text("Workout Templates", style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold)
                Text("Save your workouts and reuse them in one tap",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
            }

            // How to save hint
            item {
                Card(colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                    shape = RoundedCornerShape(14.dp)) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lightbulb, null,
                            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("To save a template, open any workout day and tap the bookmark icon in the top bar.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            if (templates.isEmpty()) {
                item {
                    EmptyPlaceholder(Icons.Default.BookmarkBorder,
                        "No templates yet — log a workout and save it as a template")
                }
            } else {
                items(templates, key = { it.id }) { template ->
                    val exercises by viewModel.getTemplateExercises(template.id)
                        .collectAsState(initial = emptyList())
                    val lastUsed = runCatching { LocalDate.parse(template.lastUsedDate) }.getOrNull()
                    val lastUsedStr = lastUsed?.let {
                        "${it.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())}, " +
                        "${it.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())} ${it.dayOfMonth}"
                    } ?: ""

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(template.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface)
                                    Text("${exercises.size} exercises" +
                                        if (lastUsedStr.isNotBlank()) " · Last used $lastUsedStr" else "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Row {
                                    IconButton(onClick = { showPreviewDialog = template }) {
                                        Icon(Icons.Default.Visibility, null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    IconButton(onClick = { viewModel.deleteTemplate(template) }) {
                                        Icon(Icons.Default.DeleteOutline, null,
                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
                                    }
                                }
                            }

                            // Exercise chips preview
                            if (exercises.isNotEmpty()) {
                                Spacer(Modifier.height(10.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()) {
                                    exercises.take(4).forEach { ex ->
                                        Box(modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                            .padding(horizontal = 8.dp, vertical = 4.dp)) {
                                            Text(ex.exerciseName.split(" ").first(),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    if (exercises.size > 4) {
                                        Box(modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                            .padding(horizontal = 8.dp, vertical = 4.dp)) {
                                            Text("+${exercises.size - 4}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(12.dp))
                            Button(
                                onClick = { showLoadDialog = template },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Load into Today's Workout", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }

    // ── Load confirmation dialog ───────────────────────────────────────────────
    showLoadDialog?.let { template ->
        AlertDialog(
            onDismissRequest = { showLoadDialog = null },
            icon = { Icon(Icons.Default.PlayArrow, null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Load Template", fontWeight = FontWeight.Bold) },
            text = { Text("Add all exercises from \"${template.name}\" to today's workout?") },
            confirmButton = {
                Button(onClick = {
                    viewModel.loadTemplateToWorkout(viewModel.todayDateString(), template.id)
                    showLoadDialog = null
                    scope.launch { snackState.showSnackbar("\"${template.name}\" loaded into today ✓") }
                }, shape = RoundedCornerShape(10.dp)) { Text("Load") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showLoadDialog = null },
                    shape = RoundedCornerShape(10.dp)) { Text("Cancel") }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // ── Preview dialog ────────────────────────────────────────────────────────
    showPreviewDialog?.let { template ->
        val exercises by viewModel.getTemplateExercises(template.id).collectAsState(initial = emptyList())
        Dialog(onDismissRequest = { showPreviewDialog = null }) {
            Card(shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(template.name, style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text("${exercises.size} exercises",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(16.dp))
                    exercises.forEachIndexed { i, ex ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(26.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center) {
                                Text("${i + 1}", style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(ex.exerciseName, style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface)
                                Text("${ex.defaultSets} sets · ${ex.defaultReps} reps",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        if (i < exercises.size - 1) HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    }
                    Spacer(Modifier.height(16.dp))
                    TextButton(onClick = { showPreviewDialog = null }, modifier = Modifier.align(Alignment.End)) {
                        Text("Close")
                    }
                }
            }
        }
    }
}
