package com.workouttracker.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.workouttracker.R
import com.workouttracker.ui.viewmodel.WorkoutViewModel
import kotlinx.coroutines.launch

@Composable
fun ToolsScreen(
    viewModel: WorkoutViewModel,
    onNavigatePlateCalc: () -> Unit,
    onNavigateBodyweight: () -> Unit,
    onNavigateTemplates: () -> Unit,
    onNavigateExerciseHistory: () -> Unit,
    onNavigateAccount: () -> Unit,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    useLbs: Boolean,
    onToggleUnit: () -> Unit
) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()
    var exporting by remember { mutableStateOf(false) }
    var exportDone by remember { mutableStateOf(false) }

    val currentLanguage by viewModel.language.collectAsStateWithLifecycle()
    val timerEnabled by viewModel.timerEnabled.collectAsStateWithLifecycle()
    val restSeconds by viewModel.defaultRestSeconds.collectAsStateWithLifecycle()
    
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showTimerDialog by remember { mutableStateOf(false) }

    val languages = listOf(
        "en" to "English",
        "tr" to "Türkçe",
        "de" to "Deutsch",
        "es" to "Español",
        "fr" to "Français"
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(stringResource(R.string.tools_title), style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold)
            Text(stringResource(R.string.utilities_settings),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
        }

        // ── Workouts ──────────────────────────────────────────────────────────
        item { SectionLabel("Workouts") }

        item {
            ToolCard(icon = Icons.Default.BookmarkBorder, iconColor = MaterialTheme.colorScheme.primary,
                title = stringResource(R.string.workout_templates),
                subtitle = "Save and reuse your favourite workouts in one tap",
                onClick = onNavigateTemplates)
        }

        item {
            ToolCard(icon = Icons.Default.TrendingUp, iconColor = Color(0xFF4CAF50),
                title = stringResource(R.string.exercise_history),
                subtitle = "See progression charts for any exercise you've logged",
                onClick = onNavigateExerciseHistory)
        }

        // ── Tracking ──────────────────────────────────────────────────────────
        item { SectionLabel("Tracking") }

        item {
            ToolCard(icon = Icons.Default.MonitorWeight, iconColor = Color(0xFF2196F3),
                title = stringResource(R.string.body_weight_log),
                subtitle = "Track your weight over time with a graph",
                onClick = onNavigateBodyweight)
        }

        item {
            ToolCard(icon = Icons.Default.FitnessCenter, iconColor = MaterialTheme.colorScheme.primary,
                title = stringResource(R.string.plate_calculator),
                subtitle = "Find exact plate setup + estimated 1RM table",
                onClick = onNavigatePlateCalc)
        }

        // ── Data ──────────────────────────────────────────────────────────────
        item { SectionLabel("Data") }

        item {
            ToolCard(
                icon = Icons.Default.CloudQueue, iconColor = Color(0xFF2196F3),
                title = stringResource(R.string.cloud_backup),
                subtitle = "Optional — back up your data to the cloud via Google Sign-In",
                onClick = onNavigateAccount
            )
        }

        item {
            ToolCard(
                icon = Icons.Default.Download, iconColor = Color(0xFF4CAF50),
                title = stringResource(R.string.export_csv),
                subtitle = if (exporting) "Generating…"
                            else if (exportDone) "✓ Check your share sheet"
                            else "Download all workouts, cardio & bodyweight data",
                onClick = {
                    if (!exporting) {
                        exporting = true; exportDone = false
                        scope.launch {
                            val csv = viewModel.generateCsv()
                            if (csv == null) {
                                exporting = false
                                return@launch
                            }
                            val file = java.io.File(context.cacheDir, "workout_export.csv")
                            file.writeText(csv)
                            val uri  = androidx.core.content.FileProvider.getUriForFile(
                                context, "${context.packageName}.fileprovider", file)
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/csv"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(intent, "Export workout data"))
                            exporting = false; exportDone = true
                        }
                    }
                },
                trailing = {
                    if (exporting) CircularProgressIndicator(modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp, color = Color(0xFF4CAF50))
                }
            )
        }

        // ── Appearance & Settings ─────────────────────────────────────────────
        item { SectionLabel("Settings") }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth().clickable { onToggleTheme() }.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF9C27B0).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center) {
                        Icon(if (isDarkTheme) Icons.Default.DarkMode else Icons.Default.LightMode,
                            null, tint = Color(0xFF9C27B0), modifier = Modifier.size(24.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.theme), style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                        Text(if (isDarkTheme) stringResource(R.string.dark_mode) else stringResource(R.string.light_mode),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = isDarkTheme, onCheckedChange = { onToggleTheme() },
                        thumbContent = {
                            Icon(if (isDarkTheme) Icons.Default.DarkMode else Icons.Default.LightMode,
                                null, modifier = Modifier.size(14.dp))
                        },
                        colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary))
                }
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth().clickable { onToggleUnit() }.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFFF9800).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Scale, null, tint = Color(0xFFFF9800), modifier = Modifier.size(24.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.weight_unit), style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                        Text(if (useLbs) stringResource(R.string.pounds) else stringResource(R.string.kilograms),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    // kg / lbs pill toggle
                    Row(modifier = Modifier.clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)) {
                        listOf(false to "kg", true to "lbs").forEach { (isLbs, label) ->
                            Box(modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (useLbs == isLbs) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable { if (useLbs != isLbs) onToggleUnit() }
                                .padding(horizontal = 14.dp, vertical = 6.dp)) {
                                Text(label, color = if (useLbs == isLbs) Color.White
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth().clickable { showTimerDialog = true }.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF44336).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Timer, null, tint = Color(0xFFF44336), modifier = Modifier.size(24.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Rest Timer", style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                        Text(if (timerEnabled) "Auto-start after set: ${restSeconds}s" else "Disabled",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = timerEnabled, onCheckedChange = { viewModel.setTimerEnabled(it) },
                        colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary))
                }
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth().clickable { showLanguageDialog = true }.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF009688).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Language, null, tint = Color(0xFF009688), modifier = Modifier.size(24.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.language), style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                        Text(languages.find { it.first == currentLanguage }?.second ?: "English",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // ── Feedback (at the very bottom, small & quiet) ───────────────────
        item { Spacer(Modifier.height(24.dp)) }

        item {
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth().clickable {
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:")
                    putExtra(Intent.EXTRA_EMAIL, arrayOf("workouttrackersupport@gmail.com"))
                    putExtra(Intent.EXTRA_SUBJECT, "Workout Tracker Feedback")
                }
                context.startActivity(intent)
            }.padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.MailOutline, null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.send_feedback),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            }
        }
    }

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(stringResource(R.string.language)) },
            text = {
                Column {
                    languages.forEach { (code, name) ->
                        Row(modifier = Modifier.fillMaxWidth().clickable {
                            viewModel.setLanguage(code)
                            showLanguageDialog = false
                        }.padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = currentLanguage == code, onClick = null)
                            Spacer(Modifier.width(12.dp))
                            Text(name, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }

    if (showTimerDialog) {
        AlertDialog(
            onDismissRequest = { showTimerDialog = false },
            title = { Text("Rest Timer Settings") },
            text = {
                Column {
                    Text("Auto-start rest timer after each set you log.", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(16.dp))
                    listOf(30, 60, 90, 120, 180).forEach { seconds ->
                        Row(modifier = Modifier.fillMaxWidth().clickable {
                            viewModel.setDefaultRestSeconds(seconds)
                        }.padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = restSeconds == seconds, onClick = null)
                            Spacer(Modifier.width(12.dp))
                            Text("${seconds} seconds", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTimerDialog = false }) {
                    Text(stringResource(R.string.done))
                }
            }
        )
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp))
}

@Composable
fun ToolCard(icon: ImageVector, iconColor: Color, title: String, subtitle: String,
    onClick: () -> Unit, trailing: @Composable (() -> Unit)? = null) {
    Card(modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp))
                .background(iconColor.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = iconColor, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                Text(subtitle, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (trailing != null) trailing()
            else Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
