package com.workouttracker.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.activity.compose.BackHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.workouttracker.R
import com.workouttracker.data.model.ExerciseSet
import com.workouttracker.data.model.PRESET_EXERCISES
import com.workouttracker.data.model.EXERCISE_CATEGORIES
import com.workouttracker.data.model.WorkoutExercise
import com.workouttracker.ui.screens.calculate1RM
import com.workouttracker.ui.util.WeightUnit
import com.workouttracker.ui.viewmodel.WorkoutViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutDetailScreen(
    viewModel: WorkoutViewModel,
    date: String,
    onBack: () -> Unit
) {
    val exercises by viewModel.getExercisesForDate(date).collectAsStateWithLifecycle(initialValue = emptyList())
    val notesFlow by viewModel.getWorkoutNotesFlow(date).collectAsStateWithLifecycle(initialValue = "")
    val activeWorkoutStartTime by viewModel.activeWorkoutStartTime.collectAsStateWithLifecycle()
    val workoutDuration by viewModel.workoutDurationFormatted.collectAsStateWithLifecycle()

    var showExercisePicker by remember { mutableStateOf(false) }
    var showNotesDialog by remember { mutableStateOf(false) }
    var showSaveTemplateDialog by remember { mutableStateOf(false) }
    var templateSaved by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val snackState = remember { SnackbarHostState() }

    val savedAsTemplateMsg = stringResource(R.string.saved_as_template)
    val isToday = remember(date) { date == LocalDate.now().toString() }

    // Intercept back — just go back, no summary dialog
    BackHandler {
        if (activeWorkoutStartTime != null) {
            viewModel.endWorkout()
        }
        onBack()
    }

    LaunchedEffect(templateSaved) {
        if (templateSaved) { snackState.showSnackbar(savedAsTemplateMsg); templateSaved = false }
    }

    val parsedDate  = remember(date) { runCatching { LocalDate.parse(date) }.getOrNull() }
    val displayDate = remember(date) {
        parsedDate?.let {
            "${it.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())}, " +
            "${it.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${it.dayOfMonth}, ${it.year}"
        } ?: date
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(if (activeWorkoutStartTime != null) stringResource(R.string.active_workout) else displayDate, 
                            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        if (activeWorkoutStartTime != null) {
                            Text(stringResource(R.string.duration) + ": $workoutDuration", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        } else if (notesFlow.isNotBlank()) {
                            Text(notesFlow, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                        }
                    }
                },
                navigationIcon = { IconButton(onClick = {
                    if (activeWorkoutStartTime != null) viewModel.endWorkout()
                    onBack()
                }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cancel)) } },
                actions = {
                    if (activeWorkoutStartTime != null) {
                        TextButton(onClick = { 
                            viewModel.endWorkout()
                            onBack()
                        }) {
                            Text(stringResource(R.string.finish), fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                        }
                    } else {
                        IconButton(onClick = { showSaveTemplateDialog = true }) {
                            Icon(Icons.Default.BookmarkBorder, contentDescription = stringResource(R.string.save_as_template),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { showNotesDialog = true }) {
                            Icon(Icons.Default.NoteAlt, contentDescription = stringResource(R.string.notes),
                                tint = if (notesFlow.isNotBlank()) MaterialTheme.colorScheme.primary
                                       else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showExercisePicker = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.add_exercise), fontWeight = FontWeight.SemiBold)
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
        ) {
            if (isToday && activeWorkoutStartTime == null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.ready_to_train), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(stringResource(R.string.start_live_session_desc), style = MaterialTheme.typography.bodySmall)
                            }
                            Button(onClick = { viewModel.startWorkout() }, shape = RoundedCornerShape(12.dp)) {
                                Text(stringResource(R.string.start_workout))
                            }
                        }
                    }
                }
            }

            if (exercises.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.FitnessCenter,
                            contentDescription = null,
                            modifier = Modifier.size(72.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            stringResource(R.string.no_exercises_yet),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            stringResource(R.string.tap_add_exercise),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            items(exercises, key = { it.id }) { exercise ->
                ExerciseCard(
                    exercise = exercise,
                    viewModel = viewModel,
                    onDelete = { viewModel.deleteExercise(exercise) }
                )
            }
        }
    }

    if (showExercisePicker) {
        ExercisePickerDialog(
            onDismiss = { showExercisePicker = false },
            onSelected = { name ->
                viewModel.addExerciseToWorkout(date, name)
                showExercisePicker = false
            }
        )
    }

    if (showNotesDialog) {
        NotesDialog(initialNotes = notesFlow, onDismiss = { showNotesDialog = false },
            onSave = { notes -> viewModel.updateWorkoutNotes(date, notes); showNotesDialog = false })
    }

    if (showSaveTemplateDialog) {
        SaveTemplateDialog(
            onDismiss = { showSaveTemplateDialog = false },
            onSave = { templateName ->
                scope.launch {
                    viewModel.saveWorkoutAsTemplate(date, templateName)
                    showSaveTemplateDialog = false
                    templateSaved = true
                }
            }
        )
    }
}

// ── Exercise Card ─────────────────────────────────────────────────────────────

@Composable
fun ExerciseCard(
    exercise: WorkoutExercise,
    viewModel: WorkoutViewModel,
    onDelete: () -> Unit
) {
    val sets by viewModel.getSetsForExercise(exercise.id).collectAsStateWithLifecycle(initialValue = emptyList())
    var showAddSet by remember { mutableStateOf(false) }
    var expanded   by rememberSaveable { mutableStateOf(true) }
    var lastSet    by remember { mutableStateOf<com.workouttracker.data.model.LastSetInfo?>(null) }
    var suggestion by remember { mutableStateOf<com.workouttracker.ui.util.WeightSuggestion?>(null) }
    val useLbs     by viewModel.useLbs.collectAsStateWithLifecycle()

    LaunchedEffect(exercise.exerciseName) {
        lastSet    = viewModel.getBestLastSet(exercise.exerciseName, exercise.workoutDate)
        val history = viewModel.getExerciseHistorySync(exercise.exerciseName)
        suggestion = com.workouttracker.ui.util.buildWeightSuggestion(
            lastSet      = lastSet,
            history      = history,
            exerciseName = exercise.exerciseName,
            useLbs       = useLbs
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.FitnessCenter,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            exercise.exerciseName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                "${sets.size} " + stringResource(R.string.sets).lowercase(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            lastSet?.let { ls ->
                                Text("•", style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    (stringResource(R.string.last_session) + ": ") +
                                    if (ls.isBodyweight) stringResource(R.string.bodyweight) + "×${ls.reps}" else "${ls.weight}kg×${ls.reps}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
                Row {
                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(
                            if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Toggle",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = stringResource(R.string.delete),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // Sets section
            AnimatedVisibility(visible = expanded) {
                Column {
                    if (sets.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))

                        // Column labels
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(stringResource(R.string.sets).uppercase(), modifier = Modifier.width(36.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(stringResource(R.string.reps).uppercase(), modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(stringResource(if (useLbs) R.string.weight_lbs else R.string.weight_kg).uppercase(), modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.width(32.dp))
                        }

                        sets.forEach { set ->
                            SetRow(
                                set = set,
                                useLbs = useLbs,
                                onDelete = { viewModel.deleteSet(set) }
                            )
                            if (set != sets.last()) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = { showAddSet = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = androidx.compose.ui.graphics.SolidColor(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            )
                        )
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.log_set), fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }

    if (showAddSet) {
        AddSetDialog(
            exerciseName = exercise.exerciseName,
            lastSet      = lastSet,
            suggestion   = suggestion,
            useLbs       = useLbs,
            onDismiss    = { showAddSet = false },
            onAdd = { reps, weight, isBodyweight ->
                viewModel.addSet(exercise.id, reps, weight, isBodyweight)
                showAddSet = false
            }
        )
    }
}

// ── Set Row ───────────────────────────────────────────────────────────────────

@Composable
fun SetRow(set: ExerciseSet, useLbs: Boolean, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(36.dp)
                .height(24.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                set.setNumber.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }

        val repsText = stringResource(R.string.reps).lowercase()
        Text(
            "${set.reps} $repsText",
            modifier = Modifier.weight(1f).padding(start = 12.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        val unit = if (useLbs) "lbs" else "kg"
        Text(
            if (set.isBodyweight) stringResource(R.string.bodyweight) else "${set.weight} $unit",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = if (set.isBodyweight) MaterialTheme.colorScheme.secondary
                    else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (set.isBodyweight) FontWeight.SemiBold else FontWeight.Normal
        )

        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = stringResource(R.string.delete),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// ── Add Set Dialog ────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSetDialog(
    exerciseName: String,
    lastSet: com.workouttracker.data.model.LastSetInfo? = null,
    suggestion: com.workouttracker.ui.util.WeightSuggestion? = null,
    useLbs: Boolean = false,
    onDismiss: () -> Unit,
    onAdd: (reps: Int, weight: Float, isBodyweight: Boolean) -> Unit
) {
    // Auto-fill suggested weight when dialog opens
    var reps by remember {
        mutableStateOf(suggestion?.suggestedReps?.toString() ?: "")
    }
    var weight by remember {
        mutableStateOf(
            suggestion?.suggestedWeight?.let {
                val v = if (useLbs) it * com.workouttracker.ui.util.WeightUnit.KG_TO_LBS else it
                if (v == kotlin.math.floor(v.toDouble()).toFloat()) v.toInt().toString()
                else String.format("%.2f", v)
            } ?: ""
        )
    }
    var isBodyweight by remember { mutableStateOf(false) }
    val repsError   = reps.isNotEmpty() && reps.toIntOrNull() == null
    val weightError = !isBodyweight && weight.isNotEmpty() && weight.toFloatOrNull() == null

    // 1RM calculation
    val estimated1RM = remember(weight, reps) {
        val w = weight.toFloatOrNull()
        val r = reps.toIntOrNull()
        if (w != null && r != null && w > 0 && r > 0 && r <= 30) calculate1RM(
            if (useLbs) w * com.workouttracker.ui.util.WeightUnit.LBS_TO_KG else w, r
        ) else null
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape  = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(stringResource(R.string.log_set),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface)
                Text(exerciseName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold)

                // ── Smart suggestion banner ───────────────────────────────────
                if (suggestion != null && suggestion.type != com.workouttracker.ui.util.SuggestionType.FIRST_TIME) {
                    Spacer(Modifier.height(10.dp))
                    val bannerColor = when (suggestion.type) {
                        com.workouttracker.ui.util.SuggestionType.INCREASE -> androidx.compose.ui.graphics.Color(0xFF4CAF50)
                        com.workouttracker.ui.util.SuggestionType.DELOAD   -> androidx.compose.ui.graphics.Color(0xFFFF9800)
                        else                                                 -> MaterialTheme.colorScheme.primary
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(bannerColor.copy(alpha = 0.1f))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(suggestion.emoji, style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.smart_suggestion),
                                style = MaterialTheme.typography.labelSmall,
                                color = bannerColor)
                            Text(suggestion.reason,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium)
                        }
                    }
                } else if (suggestion?.type == com.workouttracker.ui.util.SuggestionType.FIRST_TIME) {
                    // First time tip
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("💡", style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.width(10.dp))
                        Text(suggestion.reason,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else if (lastSet != null) {
                    // Fallback: show last session if no suggestion
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.TrendingUp, null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(stringResource(R.string.last_session),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary)
                            Text(
                                if (lastSet.isBodyweight) stringResource(R.string.bodyweight) + " × ${lastSet.reps} " + stringResource(R.string.reps).lowercase()
                                else "${lastSet.weight} kg × ${lastSet.reps} " + stringResource(R.string.reps).lowercase(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = reps,
                    onValueChange = { if (it.length <= 4) reps = it.filter(Char::isDigit) },
                    label = { Text(stringResource(R.string.reps)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = repsError,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { isBodyweight = !isBodyweight }
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stringResource(R.string.bodyweight), color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = isBodyweight,
                        onCheckedChange = { isBodyweight = it },
                        colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary)
                    )
                }

                if (!isBodyweight) {
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = weight,
                        onValueChange = { if (it.length <= 7) weight = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text(stringResource(if (useLbs) R.string.weight_lbs else R.string.weight_kg)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = weightError,
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // 1RM estimate
                if (estimated1RM != null) {
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.est_1rm),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${String.format("%.1f", estimated1RM)} kg",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.ExtraBold)
                    }
                }

                Spacer(Modifier.height(16.dp))

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
                            val r = reps.toIntOrNull() ?: return@Button
                            val w = if (isBodyweight) 0f else (weight.toFloatOrNull() ?: return@Button)
                            onAdd(r, w, isBodyweight)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        enabled = reps.isNotBlank() && (isBodyweight || weight.isNotBlank())
                    ) { Text(stringResource(R.string.save), fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

// ── Exercise Picker Dialog ────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExercisePickerDialog(
    onDismiss: () -> Unit,
    onSelected: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var customText by remember { mutableStateOf("") }

    val filteredCategories = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            EXERCISE_CATEGORIES
        } else {
            EXERCISE_CATEGORIES.mapValues { (_, exercises) ->
                exercises.filter { it.contains(searchQuery, ignoreCase = true) }
            }.filterValues { it.isNotEmpty() }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 640.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    stringResource(R.string.add_exercise),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(Modifier.height(16.dp))

                // Custom name
                OutlinedTextField(
                    value = customText,
                    onValueChange = { customText = it },
                    label = { Text("Custom exercise name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        if (customText.isNotBlank()) {
                            IconButton(onClick = { onSelected(customText.trim()) }) {
                                Icon(Icons.Default.Check, contentDescription = "Use custom",
                                    tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                )

                Spacer(Modifier.height(10.dp))

                // Search
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(stringResource(R.string.search)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                )

                Spacer(Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (filteredCategories.isEmpty()) {
                        item {
                            Text(
                                "No matches — use custom name above",
                                modifier = Modifier.padding(16.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    
                    filteredCategories.forEach { (category, exercises) ->
                        item {
                            Text(
                                category,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp, start = 4.dp)
                            )
                        }
                        
                        items(exercises) { exercise ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onSelected(exercise) }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.FitnessCenter,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    exercise,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) { Text(stringResource(R.string.cancel)) }
            }
        }
    }
}

// ── Save Template Dialog ──────────────────────────────────────────────────────

@Composable
fun SaveTemplateDialog(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(stringResource(R.string.save_as_template), style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text("Give this workout a name to reuse it later",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(value = name, onValueChange = { name = it },
                    label = { Text(stringResource(R.string.template_name)) }, modifier = Modifier.fillMaxWidth(),
                    singleLine = true, shape = RoundedCornerShape(12.dp),
                    placeholder = { Text("e.g. My Push Day") })
                Spacer(Modifier.height(20.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)) { Text(stringResource(R.string.cancel)) }
                    Button(onClick = { onSave(name.trim()) }, modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp), enabled = name.isNotBlank()) {
                        Text(stringResource(R.string.save), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ── Notes Dialog ──────────────────────────────────────────────────────────────

@Composable
fun NotesDialog(
    initialNotes: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var notes by remember(initialNotes) { mutableStateOf(initialNotes) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    stringResource(R.string.workout_notes),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    placeholder = { Text("How did today's session go?") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp),
                    maxLines = 6,
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(Modifier.height(20.dp))
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
                        onClick = { onSave(notes) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text(stringResource(R.string.save), fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}
