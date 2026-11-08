package com.example.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.calculation.NutritionCalculator
import com.example.data.model.Exercise
import com.example.data.model.WorkoutSession
import com.example.data.model.WorkoutSet
import com.example.ui.components.ConsistCard
import com.example.ui.components.ConsistPrimaryButton
import com.example.ui.theme.*
import com.example.ui.viewmodel.ConsistViewModel

@Composable
fun WorkoutScreen(
    viewModel: ConsistViewModel,
    onBack: () -> Unit
) {
    val workouts by viewModel.allWorkouts.collectAsState()
    val exercises by viewModel.allExercises.collectAsState()
    val profile by viewModel.userProfile.collectAsState()

    var showLogWorkoutModal by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ConsistBlack)
            .statusBarsPadding()
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = ConsistCyan)
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column {
                    Text(
                        text = "WORKOUT TRACKER",
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = ConsistWhite,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = "BUILD. TRAIN. REPEAT.",
                        style = MaterialTheme.typography.labelSmall.copy(color = ConsistCyan, fontWeight = FontWeight.Bold)
                    )
                }
            }

            Button(
                onClick = { showLogWorkoutModal = true },
                colors = ButtonDefaults.buttonColors(containerColor = ConsistCyan, contentColor = ConsistBlack),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Log Workout", fontWeight = FontWeight.Bold)
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Exercise Catalog Preview Card
            item {
                ConsistCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "EXERCISE CATALOG (${exercises.size})",
                        style = MaterialTheme.typography.labelMedium.copy(color = ConsistCyan, fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Science-backed compound movements with MET energy expenditure metrics.",
                        style = MaterialTheme.typography.bodySmall.copy(color = ConsistGrey)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Chest", "Legs", "Back", "Core", "Cardio").forEach { cat ->
                            Surface(
                                color = ConsistCharcoalElevated,
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.padding(vertical = 2.dp)
                            ) {
                                Text(
                                    cat,
                                    color = ConsistWhite,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Workout History
            item {
                Text(
                    text = "TRAINING LOG HISTORY",
                    style = MaterialTheme.typography.labelMedium.copy(color = ConsistCyan, fontWeight = FontWeight.Bold)
                )
            }

            if (workouts.isEmpty()) {
                item {
                    ConsistCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = ConsistGrey, modifier = Modifier.size(40.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No workouts logged yet", color = ConsistWhite, fontWeight = FontWeight.Bold)
                            Text("Tap '+ Log Workout' above to record today's session.", color = ConsistGrey, fontSize = 12.sp)
                        }
                    }
                }
            } else {
                items(workouts) { session ->
                    ConsistCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = session.title,
                                    style = MaterialTheme.typography.titleMedium.copy(color = ConsistWhite, fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "${session.date} • ${session.durationMinutes} min",
                                    style = MaterialTheme.typography.bodySmall.copy(color = ConsistGrey)
                                )
                            }
                            Surface(
                                color = ConsistCyanContainer,
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, ConsistCyan.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = "-${session.caloriesBurned} kcal",
                                    style = MaterialTheme.typography.labelMedium.copy(color = ConsistCyan, fontWeight = FontWeight.Bold),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                        if (session.notes.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Notes: ${session.notes}",
                                style = MaterialTheme.typography.bodySmall.copy(color = ConsistGreyLight)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showLogWorkoutModal) {
        LogWorkoutDialog(
            exercises = exercises,
            weightKg = profile?.currentWeightKg ?: 70f,
            onDismiss = { showLogWorkoutModal = false },
            onSave = { title, duration, exercise, notes ->
                viewModel.logWorkout(title, duration, exercise, emptyList(), notes)
                showLogWorkoutModal = false
            }
        )
    }
}

@Composable
fun LogWorkoutDialog(
    exercises: List<Exercise>,
    weightKg: Float,
    onDismiss: () -> Unit,
    onSave: (String, Int, Exercise?, String) -> Unit
) {
    var title by remember { mutableStateOf("Hypertrophy Session") }
    var durationText by remember { mutableStateOf("45") }
    var selectedExercise by remember { mutableStateOf<Exercise?>(exercises.firstOrNull()) }
    var notes by remember { mutableStateOf("") }

    val duration = durationText.toIntOrNull() ?: 45
    val estimatedBurn = NutritionCalculator.calculateExerciseCalories(
        selectedExercise?.metValue ?: 5.0f,
        weightKg,
        duration
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Workout Session", color = ConsistWhite, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Session Title") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ConsistCyan, unfocusedBorderColor = ConsistBorder),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = durationText,
                    onValueChange = { durationText = it },
                    label = { Text("Duration (minutes)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ConsistCyan, unfocusedBorderColor = ConsistBorder),
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Primary Movement / Exercise:", color = ConsistCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)

                // Select from exercises
                LazyColumn(
                    modifier = Modifier.height(140.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(exercises) { ex ->
                        val isSelected = selectedExercise?.id == ex.id
                        Surface(
                            color = if (isSelected) ConsistCyanContainer else ConsistCharcoalCard,
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) ConsistCyan else ConsistBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedExercise = ex }
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(ex.name, color = ConsistWhite, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Text("${ex.targetMuscle} • MET ${ex.metValue}", color = ConsistCyan, fontSize = 11.sp)
                            }
                        }
                    }
                }

                // Calculated Burn Preview
                Surface(
                    color = ConsistCharcoalElevated,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Deterministic Energy Burn:", color = ConsistGrey, fontSize = 12.sp)
                        Text("$estimatedBurn kcal", color = ConsistCyan, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes / Sets / RPE") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ConsistCyan, unfocusedBorderColor = ConsistBorder),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(title, duration, selectedExercise, notes)
                },
                colors = ButtonDefaults.buttonColors(containerColor = ConsistCyan, contentColor = ConsistBlack)
            ) {
                Text("Save Workout", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = ConsistGrey) }
        },
        containerColor = ConsistCharcoalDark
    )
}
