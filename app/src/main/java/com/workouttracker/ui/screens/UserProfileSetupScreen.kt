package com.workouttracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.workouttracker.data.model.UserProfile
import com.workouttracker.ui.viewmodel.AiCoachViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileSetupScreen(
    viewModel: AiCoachViewModel,
    onBack: () -> Unit
) {
    val existing by viewModel.userProfile.collectAsState()

    var name              by remember(existing) { mutableStateOf(existing?.name ?: "") }
    var age               by remember(existing) { mutableStateOf(if ((existing?.age ?: 0) > 0) existing!!.age.toString() else "") }
    var experience        by remember(existing) { mutableStateOf(existing?.experience ?: "Intermediate") }
    var primaryGoal       by remember(existing) { mutableStateOf(existing?.primaryGoal ?: "") }
    var injuries          by remember(existing) { mutableStateOf(existing?.injuries ?: "") }
    var preferredSplit    by remember(existing) { mutableStateOf(existing?.preferredSplit ?: "") }
    var trainingDays      by remember(existing) { mutableStateOf(existing?.trainingDaysPerWeek?.toString() ?: "4") }
    var additionalNotes   by remember(existing) { mutableStateOf(existing?.additionalNotes ?: "") }

    val experienceLevels = listOf("Beginner", "Intermediate", "Advanced")
    val goalOptions = listOf(
        "Build muscle", "Increase strength", "Lose fat",
        "Improve athleticism", "General fitness", "Powerlifting", "Other"
    )
    val splitOptions = listOf(
        "Push / Pull / Legs", "Upper / Lower",
        "Full Body", "Body Part Split", "Custom"
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Your Profile", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                actions = {
                    TextButton(onClick = {
                        viewModel.saveProfile(UserProfile(
                            name              = name.trim(),
                            age               = age.toIntOrNull() ?: 0,
                            experience        = experience,
                            primaryGoal       = primaryGoal.trim(),
                            injuries          = injuries.trim(),
                            preferredSplit    = preferredSplit.trim(),
                            trainingDaysPerWeek = trainingDays.toIntOrNull()?.coerceIn(1, 7) ?: 4,
                            additionalNotes   = additionalNotes.trim()
                        ))
                        onBack()
                    }) {
                        Text("Save", fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text(
                    "The more you tell me, the better I can coach you.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Name + Age
            item {
                ProfileCard {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Name (optional)") },
                            modifier = Modifier.weight(2f),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = age,
                            onValueChange = { age = it.filter { c -> c.isDigit() }.take(3) },
                            label = { Text("Age") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                }
            }

            // Experience
            item {
                ProfileCard {
                    ProfileSectionLabel("Experience Level")
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        experienceLevels.forEach { level ->
                            FilterChip(
                                selected = experience == level,
                                onClick  = { experience = level },
                                label    = { Text(level) }
                            )
                        }
                    }
                }
            }

            // Primary Goal
            item {
                ProfileCard {
                    ProfileSectionLabel("Primary Goal")
                    Spacer(Modifier.height(10.dp))
                    goalOptions.chunked(3).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { goal ->
                                FilterChip(
                                    selected = primaryGoal == goal,
                                    onClick  = { primaryGoal = if (primaryGoal == goal) "" else goal },
                                    label    = { Text(goal, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                    if (primaryGoal == "Other") {
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = primaryGoal,
                            onValueChange = { primaryGoal = it },
                            label = { Text("Describe your goal") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            // Training Days
            item {
                ProfileCard {
                    ProfileSectionLabel("Training Days Per Week")
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        (2..7).forEach { d ->
                            FilterChip(
                                selected = trainingDays == d.toString(),
                                onClick  = { trainingDays = d.toString() },
                                label    = { Text("$d") }
                            )
                        }
                    }
                }
            }

            // Preferred Split
            item {
                ProfileCard {
                    ProfileSectionLabel("Preferred Training Split")
                    Spacer(Modifier.height(10.dp))
                    splitOptions.chunked(2).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { split ->
                                FilterChip(
                                    selected = preferredSplit == split,
                                    onClick  = { preferredSplit = if (preferredSplit == split) "" else split },
                                    label    = { Text(split, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                }
            }

            // Injuries
            item {
                ProfileCard {
                    ProfileSectionLabel("Injuries / Limitations")
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = injuries,
                        onValueChange = { injuries = it },
                        label = { Text("e.g. lower back issue, bad shoulder") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        minLines = 2,
                        maxLines = 3
                    )
                }
            }

            // Additional notes
            item {
                ProfileCard {
                    ProfileSectionLabel("Anything else the coach should know")
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = additionalNotes,
                        onValueChange = { additionalNotes = it },
                        label = { Text("e.g. I train in a home gym, no cables available") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        minLines = 2,
                        maxLines = 4
                    )
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun ProfileCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

@Composable
private fun ProfileSectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface
    )
}
