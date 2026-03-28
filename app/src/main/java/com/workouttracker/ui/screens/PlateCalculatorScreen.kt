package com.workouttracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.workouttracker.R
import com.workouttracker.ui.util.WeightUnit
import com.workouttracker.ui.viewmodel.WorkoutViewModel
import java.util.Locale
import kotlin.math.floor

// Standard plate weights in kg
val PLATE_SIZES = listOf(25f, 20f, 15f, 10f, 5f, 2.5f, 1.25f)
val PLATE_COLORS = listOf(
    Color(0xFFFF5252), // 25 - red
    Color(0xFF2196F3), // 20 - blue
    Color(0xFFFFEB3B), // 15 - yellow
    Color(0xFF4CAF50), // 10 - green
    Color(0xFF9E9E9E), // 5  - white/grey
    Color(0xFFFF9800), // 2.5 - orange
    Color(0xFF9C27B0)  // 1.25 - purple
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlateCalculatorScreen(viewModel: WorkoutViewModel) {
    var targetWeightInput by remember { mutableStateOf("") }
    var barWeightInput by remember { mutableStateOf("20") }
    val useLbs by viewModel.useLbs.collectAsStateWithLifecycle()

    val result = remember(targetWeightInput, barWeightInput, useLbs) {
        val total = targetWeightInput.toFloatOrNull() ?: 0f
        val bar   = barWeightInput.toFloatOrNull() ?: 20f
        
        // Always calculate in KG for the plate logic, convert input if it's in LBS
        val totalKg = if (useLbs) total * WeightUnit.LBS_TO_KG else total
        val barKg   = if (useLbs) bar * WeightUnit.LBS_TO_KG else bar
        
        calculatePlates(totalKg, barKg)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(stringResource(R.string.plate_calc_title), style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold)
            Text(stringResource(R.string.target_weight) + " (${if (useLbs) "lbs" else "kg"})",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        // Input card
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(18.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = targetWeightInput,
                        onValueChange = { targetWeightInput = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text(stringResource(R.string.target_weight)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = { Icon(Icons.Default.FitnessCenter, null, tint = MaterialTheme.colorScheme.primary) }
                    )

                    // Bar type selector
                    Text(stringResource(R.string.bar_weight), style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val bars = if (useLbs) listOf("35" to "Training", "45" to "Standard", "55" to "Heavy")
                                   else listOf("15" to "Women's", "20" to "Olympic", "25" to "Heavy")
                        
                        bars.forEach { (valStr, label) ->
                            val selected = barWeightInput == valStr
                            FilterChip(
                                selected = selected,
                                onClick  = { barWeightInput = valStr },
                                label    = { Text("$valStr ${if (useLbs) "lb" else "kg"}\n$label", textAlign = TextAlign.Center, fontSize = 11.sp) },
                                colors   = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor     = Color.White
                                )
                            )
                        }
                    }
                }
            }
        }

        // Result card
        if (targetWeightInput.toFloatOrNull() != null && targetWeightInput.toFloat() > 0f) {
            item {
                val total = targetWeightInput.toFloat()
                val bar   = barWeightInput.toFloatOrNull() ?: 20f
                val valid = total >= bar

                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(18.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        if (!valid) {
                            Text(stringResource(R.string.error_try_again),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium)
                        } else {
                            Text(stringResource(R.string.per_side), style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            
                            val unit = if (useLbs) "lbs" else "kg"
                            Text("${stringResource(R.string.bar_weight)}: ${bar}$unit  •  Total: ${total}$unit",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(16.dp))

                            if (result.isEmpty()) {
                                Text(stringResource(R.string.no_plates),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else {
                                // Visual bar diagram
                                BarbellDiagram(result)
                                Spacer(Modifier.height(16.dp))

                                // Plate list
                                result.forEach { (plateKg, count) ->
                                    val plateIdx = PLATE_SIZES.indexOf(plateKg)
                                    val color = PLATE_COLORS.getOrElse(plateIdx) { MaterialTheme.colorScheme.primary }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(modifier = Modifier.size(28.dp).clip(CircleShape).background(color),
                                            contentAlignment = Alignment.Center) {
                                            Text("${plateKg.toInt().let { if (plateKg == it.toFloat()) "$it" else "$plateKg" }}",
                                                color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Spacer(Modifier.width(12.dp))
                                        
                                        // Conversion for display if in LBS mode
                                        val displayWeight = if (useLbs) String.format(Locale.US, "%.1f", plateKg * WeightUnit.KG_TO_LBS) else plateKg.toString()
                                        
                                        Text("$displayWeight $unit plates",
                                            modifier = Modifier.weight(1f),
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurface)
                                        Text("× $count",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = color, fontWeight = FontWeight.ExtraBold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 1RM reference table
            item {
                val target = targetWeightInput.toFloatOrNull() ?: return@item
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(18.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(stringResource(R.string.est_1rm),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.height(12.dp))

                        // Column headers
                        Row(modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 12.dp, vertical = 6.dp)) {
                            Text(stringResource(R.string.reps), modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(stringResource(R.string.est_1rm), modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.End)
                        }
                        listOf(1,2,3,4,5,6,8,10,12).forEach { reps ->
                            val orm = calculate1RM(target, reps)
                            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
                                Text("$reps ${stringResource(R.string.reps).lowercase()}", modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface)
                                Text("${String.format(Locale.US, "%.1f", orm)} ${if (useLbs) "lbs" else "kg"}", modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End)
                            }
                            if (reps != 12) HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.padding(horizontal = 12.dp))
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
fun BarbellDiagram(plates: List<Pair<Float, Int>>) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        // Left collar
        Box(modifier = Modifier.width(8.dp).height(20.dp).clip(RoundedCornerShape(2.dp))
            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)))
        // Left plates (reversed)
        plates.reversed().forEach { (kg, count) ->
            val idx = PLATE_SIZES.indexOf(kg)
            val color = PLATE_COLORS.getOrElse(idx) { MaterialTheme.colorScheme.primary }
            val plateH = (20 + PLATE_SIZES.indexOf(kg).coerceAtMost(5) * 6).dp
            repeat(count) {
                Spacer(Modifier.width(2.dp))
                Box(modifier = Modifier.width(12.dp).height(plateH).clip(RoundedCornerShape(3.dp)).background(color))
            }
        }
        // Bar
        Box(modifier = Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)))
        // Right plates
        plates.forEach { (kg, count) ->
            val idx = PLATE_SIZES.indexOf(kg)
            val color = PLATE_COLORS.getOrElse(idx) { MaterialTheme.colorScheme.primary }
            val plateH = (20 + PLATE_SIZES.indexOf(kg).coerceAtMost(5) * 6).dp
            repeat(count) {
                Spacer(Modifier.width(2.dp))
                Box(modifier = Modifier.width(12.dp).height(plateH).clip(RoundedCornerShape(3.dp)).background(color))
            }
        }
        // Right collar
        Spacer(Modifier.width(2.dp))
        Box(modifier = Modifier.width(8.dp).height(20.dp).clip(RoundedCornerShape(2.dp))
            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)))
    }
}

fun calculatePlates(totalKg: Float, barKg: Float): List<Pair<Float, Int>> {
    if (totalKg <= barKg) return emptyList()
    var remaining = (totalKg - barKg) / 2f
    val result = mutableListOf<Pair<Float, Int>>()
    for (plate in PLATE_SIZES) {
        if (remaining >= plate) {
            val count = floor(remaining / plate).toInt()
            result.add(plate to count)
            remaining -= count * plate
        }
        if (remaining < 0.01f) break
    }
    return result
}

fun calculate1RM(weight: Float, reps: Int): Float {
    if (reps == 1) return weight
    return weight * (1f + reps / 30f)  // Epley formula
}
