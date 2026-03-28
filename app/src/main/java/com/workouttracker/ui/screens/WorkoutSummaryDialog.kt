package com.workouttracker.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.workouttracker.data.model.ExerciseSet
import com.workouttracker.data.model.WorkoutExercise
import com.workouttracker.ui.util.WeightUnit
import kotlinx.coroutines.delay

@Composable
fun WorkoutSummaryDialog(
    exercises: List<WorkoutExercise>,
    allSets: Map<Long, List<ExerciseSet>>,
    useLbs: Boolean,
    onDismiss: () -> Unit
) {
    val totalSets     = allSets.values.sumOf { it.size }
    val totalReps     = allSets.values.sumOf { sets -> sets.sumOf { it.reps } }
    val totalVolumeKg = allSets.values.sumOf { sets ->
        sets.filter { !it.isBodyweight }.sumOf { (it.weight * it.reps).toDouble() }
    }.toFloat()

    // Only show if there's actual data
    if (exercises.isEmpty() || totalSets == 0) { onDismiss(); return }

    // Entrance animations
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(100); visible = true }

    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.8f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "dialog_scale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(300),
        label = "dialog_alpha"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f * alpha)),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth()
                    .scale(scale),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Trophy animation
                    var trophyBig by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) { delay(200); trophyBig = true }
                    val trophyScale by animateFloatAsState(
                        targetValue = if (trophyBig) 1f else 0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "trophy_scale"
                    )

                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .scale(trophyScale)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🏆", fontSize = 40.sp)
                    }

                    Text(
                        "Workout Complete!",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        "Great session — here's what you did:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    // Stats grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SummaryStatBox(
                            modifier = Modifier.weight(1f),
                            value    = exercises.size.toString(),
                            label    = "Exercises",
                            emoji    = "💪"
                        )
                        SummaryStatBox(
                            modifier = Modifier.weight(1f),
                            value    = totalSets.toString(),
                            label    = "Sets",
                            emoji    = "🔢"
                        )
                        SummaryStatBox(
                            modifier = Modifier.weight(1f),
                            value    = totalReps.toString(),
                            label    = "Reps",
                            emoji    = "⚡"
                        )
                    }

                    if (totalVolumeKg > 0) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors   = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text("🏋️", fontSize = 20.sp)
                                Spacer(Modifier.width(10.dp))
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        WeightUnit.formatVolume(totalVolumeKg, useLbs),
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        "total volume lifted",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Exercise list
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        exercises.forEach { ex ->
                            val sets = allSets[ex.id] ?: emptyList()
                            if (sets.isNotEmpty()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        ex.exerciseName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )
                                    val bestSet = sets.filter { !it.isBodyweight }
                                        .maxByOrNull { it.weight }
                                    Text(
                                        if (bestSet != null)
                                            "${sets.size} sets · top ${WeightUnit.display(bestSet.weight, useLbs)}"
                                        else
                                            "${sets.size} sets · bodyweight",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    Button(
                        onClick   = onDismiss,
                        modifier  = Modifier.fillMaxWidth().height(52.dp),
                        shape     = RoundedCornerShape(14.dp)
                    ) {
                        Text("Done 💪", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun SummaryStatBox(modifier: Modifier = Modifier, value: String, label: String, emoji: String) {
    // Count-up animation
    var displayed by remember { mutableStateOf(0) }
    val target = value.toIntOrNull() ?: 0
    LaunchedEffect(target) {
        val steps = 20
        repeat(steps) { i ->
            displayed = (target * (i + 1) / steps)
            delay(15)
        }
        displayed = target
    }

    Card(
        modifier = modifier,
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape    = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(emoji, fontSize = 18.sp)
            Text(
                displayed.toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
