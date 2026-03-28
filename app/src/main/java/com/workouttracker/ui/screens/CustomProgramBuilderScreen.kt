package com.workouttracker.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.workouttracker.data.model.CustomProgram
import com.workouttracker.data.model.CustomProgramDay
import com.workouttracker.data.model.PRESET_EXERCISES
import com.workouttracker.ui.components.EmptyPlaceholder
import com.workouttracker.ui.viewmodel.WorkoutViewModel
import kotlinx.coroutines.launch

// ── Browse custom programs ────────────────────────────────────────────────────

@Composable
fun CustomProgramsScreen(
    viewModel: WorkoutViewModel,
    onCreateNew: () -> Unit,
    onOpenProgram: (Long) -> Unit
) {
    val programs by viewModel.allCustomPrograms.collectAsState()
    val progress by viewModel.allProgramProgress.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("My Programs", style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold)
                    Text("Custom training programs you've built",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                FloatingActionButton(
                    onClick = onCreateNew,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    modifier = Modifier.size(48.dp)
                ) { Icon(Icons.Default.Add, null) }
            }
            Spacer(Modifier.height(4.dp))
        }

        if (programs.isEmpty()) {
            item {
                EmptyPlaceholder(Icons.Default.Build, "Tap + to build your first custom program")
            }
        } else {
            items(programs, key = { it.id }) { program ->
                val prog = progress.find { it.programId == "custom_${program.id}" }
                CustomProgramCard(
                    program  = program,
                    isActive = prog != null,
                    onOpen   = { onOpenProgram(program.id) },
                    onDelete = { viewModel.deleteCustomProgram(program) }
                )
            }
        }
    }
}

@Composable
fun CustomProgramCard(
    program: CustomProgram,
    isActive: Boolean,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth().clickable { onOpen() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Text("🏗️", fontSize = 28.sp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(program.name, style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
                        if (program.tagline.isNotBlank())
                            Text(program.tagline, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.DeleteOutline, null,
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BuilderMiniStat(Icons.Default.Repeat, "${program.daysPerWeek}x/week")
                BuilderMiniStat(Icons.Default.CalendarToday, "${program.durationWeeks} weeks")
                BuilderMiniStat(Icons.Default.Speed, program.difficulty)
            }
            if (isActive) {
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PlayCircle, null,
                        tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Active", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ── New Custom Program ────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewCustomProgramScreen(
    viewModel: WorkoutViewModel,
    onBack: () -> Unit,
    onCreated: (Long) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var tagline by remember { mutableStateOf("") }
    var daysPerWeek by remember { mutableStateOf("3") }
    var durationWeeks by remember { mutableStateOf("12") }
    var difficulty by remember { mutableStateOf("Intermediate") }
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("New Custom Program", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Program Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = tagline,
                onValueChange = { tagline = it },
                label = { Text("Tagline (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = daysPerWeek,
                    onValueChange = { daysPerWeek = it },
                    label = { Text("Days / Week") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = durationWeeks,
                    onValueChange = { durationWeeks = it },
                    label = { Text("Weeks") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            Text("Difficulty", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            val options = listOf("Beginner", "Intermediate", "Advanced")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEach { opt ->
                    FilterChip(
                        selected = difficulty == opt,
                        onClick = { difficulty = opt },
                        label = { Text(opt) }
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        scope.launch {
                            val id = viewModel.createCustomProgram(
                                name = name,
                                tagline = tagline,
                                daysPerWeek = daysPerWeek.toIntOrNull() ?: 3,
                                durationWeeks = durationWeeks.toIntOrNull() ?: 12,
                                difficulty = difficulty
                            )
                            if (id != null) onCreated(id)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                enabled = name.isNotBlank()
            ) {
                Text("Create Program", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ── Program Detail / Editor ───────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomProgramDetailScreen(
    programId: Long,
    viewModel: WorkoutViewModel,
    onBack: () -> Unit,
    onLoadedToWorkout: (String) -> Unit
) {
    val programs by viewModel.allCustomPrograms.collectAsState()
    val program = programs.find { it.id == programId }
    val days by viewModel.getCustomDays(programId).collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var showAddDayDialog by remember { mutableStateOf(false) }
    var showLoadDialog by remember { mutableStateOf(false) }
    val snackState = remember { SnackbarHostState() }

    if (program == null) { onBack(); return }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackState) },
        topBar = {
            TopAppBar(
                title = { Text(program.name, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                }},
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SmallFloatingActionButton(
                    onClick = { showAddDayDialog = true },
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ) { Icon(Icons.Default.Add, null) }
                ExtendedFloatingActionButton(
                    onClick = { showLoadDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Load Day", fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    BuilderMiniStat(Icons.Default.Repeat, "${program.daysPerWeek}x/week")
                    BuilderMiniStat(Icons.Default.CalendarToday, "${program.durationWeeks} weeks")
                    BuilderMiniStat(Icons.Default.Speed, program.difficulty)
                }
            }

            if (days.isEmpty()) {
                item {
                    EmptyPlaceholder(Icons.Default.CalendarToday,
                        "Tap + to add your first training day")
                }
            } else {
                items(days, key = { it.id }) { day ->
                    CustomDayCard(day = day, viewModel = viewModel,
                        onDelete = { viewModel.deleteCustomDay(day) })
                }
            }
        }
    }

    if (showAddDayDialog) {
        AddCustomDayDialog(
            nextDayNumber = (days.maxOfOrNull { it.dayNumber } ?: 0) + 1,
            onDismiss     = { showAddDayDialog = false },
            onSave        = { name, focus, isRest ->
                val nextNum = (days.maxOfOrNull { it.dayNumber } ?: 0) + 1
                scope.launch {
                    viewModel.addCustomDay(programId, nextNum, name, focus, isRest)
                    showAddDayDialog = false
                }
            }
        )
    }

    if (showLoadDialog && days.isNotEmpty()) {
        LoadCustomDayDialog(
            days      = days,
            onDismiss = { showLoadDialog = false },
            onLoad    = { day ->
                if (!day.isRestDay) {
                    val date = viewModel.todayDateString()
                    viewModel.loadCustomDayToWorkout(date, day.id, programId)
                    showLoadDialog = false
                    scope.launch { snackState.showSnackbar("Loaded \"${day.name}\" into today ✓") }
                    onLoadedToWorkout(date)
                }
            }
        )
    }
}

// ── Custom Day Card ───────────────────────────────────────────────────────────

@Composable
fun CustomDayCard(day: CustomProgramDay, viewModel: WorkoutViewModel, onDelete: () -> Unit) {
    val exercises by viewModel.getCustomExercises(day.id).collectAsState(initial = emptyList())
    var expanded by remember { mutableStateOf(false) }
    var showAddExercise by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Card(colors = CardDefaults.cardColors(
        containerColor = if (day.isRestDay) MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                         else MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth().clickable { if (!day.isRestDay) expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(38.dp).clip(CircleShape)
                        .background(if (day.isRestDay) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                                    else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center) {
                        Text(day.dayNumber.toString(), fontWeight = FontWeight.Bold,
                            color = if (day.isRestDay) MaterialTheme.colorScheme.onSurfaceVariant
                                    else MaterialTheme.colorScheme.primary)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(day.name, style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        if (day.isRestDay)
                            Text("Rest day", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        else if (day.focus.isNotBlank())
                            Text(day.focus, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.DeleteOutline, null,
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.4f),
                            modifier = Modifier.size(20.dp))
                    }
                    if (!day.isRestDay) {
                        Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            AnimatedVisibility(visible = expanded && !day.isRestDay) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    exercises.forEach { ex ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.FitnessCenter, null,
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(ex.exerciseName, modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface)
                            Text("${ex.sets}×${ex.reps}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                            IconButton(onClick = { scope.launch { viewModel.deleteCustomExercise(ex) } },
                                modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Close, null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = { showAddExercise = true },
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Add Exercise")
                    }
                }
            }
        }
    }

    if (showAddExercise) {
        AddExerciseToDayDialog(
            onDismiss = { showAddExercise = false },
            onAdd     = { name, sets, reps, notes ->
                scope.launch {
                    viewModel.addCustomExercise(day.id, name, sets, reps, notes, exercises.size)
                    showAddExercise = false
                }
            }
        )
    }
}

// ── Dialogs ───────────────────────────────────────────────────────────────────

@Composable
fun AddCustomDayDialog(nextDayNumber: Int, onDismiss: () -> Unit,
    onSave: (name: String, focus: String, isRest: Boolean) -> Unit) {
    var name by remember { mutableStateOf("Day $nextDayNumber") }
    var focus by remember { mutableStateOf("") }
    var isRest by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Training Day") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it },
                    label = { Text("Day Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = focus, onValueChange = { focus = it },
                    label = { Text("Focus (e.g. Chest/Triceps)") }, modifier = Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isRest, onCheckedChange = { isRest = it })
                    Text("This is a rest day")
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(name, focus, isRest) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AddExerciseToDayDialog(onDismiss: () -> Unit,
    onAdd: (name: String, sets: Int, reps: String, notes: String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var sets by remember { mutableStateOf("3") }
    var reps by remember { mutableStateOf("10") }
    var notes by remember { mutableStateOf("") }
    var showPresetList by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Add Exercise", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

                Box {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it; showPresetList = it.isNotBlank() },
                        label = { Text("Exercise Name") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            if (name.isNotBlank()) {
                                IconButton(onClick = { name = ""; showPresetList = false }) {
                                    Icon(Icons.Default.Clear, null)
                                }
                            }
                        }
                    )

                    if (showPresetList) {
                        val filtered = PRESET_EXERCISES.filter { it.contains(name, ignoreCase = true) }.take(5)
                        if (filtered.isNotEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(top = 64.dp),
                                elevation = CardDefaults.cardElevation(8.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column {
                                    filtered.forEach { preset ->
                                        Text(preset, modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { name = preset; showPresetList = false }
                                            .padding(16.dp))
                                        if (preset != filtered.last()) HorizontalDivider()
                                    }
                                }
                            }
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = sets, onValueChange = { sets = it },
                        label = { Text("Sets") }, modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    OutlinedTextField(value = reps, onValueChange = { reps = it },
                        label = { Text("Reps") }, modifier = Modifier.weight(1f))
                }

                OutlinedTextField(value = notes, onValueChange = { notes = it },
                    label = { Text("Notes (optional)") }, modifier = Modifier.fillMaxWidth())

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = {
                        val s = sets.toIntOrNull() ?: 0
                        if (name.isNotBlank()) onAdd(name, s, reps, notes)
                    }) { Text("Add") }
                }
            }
        }
    }
}

@Composable
fun LoadCustomDayDialog(days: List<CustomProgramDay>, onDismiss: () -> Unit, onLoad: (CustomProgramDay) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Load Day into Today") },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                items(days.filter { !it.isRestDay }) { day ->
                    ListItem(
                        headlineContent = { Text(day.name, fontWeight = FontWeight.Bold) },
                        supportingContent = { if (day.focus.isNotBlank()) Text(day.focus) },
                        leadingContent = {
                            Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center) {
                                Text(day.dayNumber.toString(), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                            }
                        },
                        modifier = Modifier.clickable { onLoad(day) }
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
fun BuilderMiniStat(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .padding(horizontal = 8.dp, vertical = 4.dp)) {
        Icon(icon, null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(4.dp))
        Text(text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
