package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.ConsistCard
import com.example.ui.components.NeonGlowCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.ConsistViewModel
import kotlin.math.max

@Composable
fun ProgressScreen(
    viewModel: ConsistViewModel
) {
    val profile by viewModel.userProfile.collectAsState()
    val weightEntries by viewModel.allWeightEntries.collectAsState()
    val waterIntake by viewModel.currentDayWater.collectAsState()
    val dailySummary by viewModel.dailySummary.collectAsState()
    val workouts by viewModel.allWorkouts.collectAsState()

    var showLogWeightModal by remember { mutableStateOf(false) }

    val targetCals = profile?.targetCalories ?: 2400
    val targetPro = profile?.targetProteinGrams ?: 140f
    val currentStreak = profile?.currentStreak ?: 1

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
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "PROGRESS & ANALYTICS",
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = ConsistWhite,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                )
                Text(
                    text = "BUILD. TRAIN. REPEAT.",
                    style = MaterialTheme.typography.labelSmall.copy(color = ConsistCyan, fontWeight = FontWeight.Bold)
                )
            }

            Button(
                onClick = { showLogWeightModal = true },
                colors = ButtonDefaults.buttonColors(containerColor = ConsistCyan, contentColor = ConsistBlack),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Log Weight", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Streaks & Consistency Banner
            item {
                NeonGlowCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🔥", fontSize = 24.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "$currentStreak DAY CONSISTENCY",
                                    style = MaterialTheme.typography.titleMedium.copy(color = ConsistCyan, fontWeight = FontWeight.Black)
                                )
                            }
                            Text(
                                text = "Consecutive days tracking nutrition and training.",
                                style = MaterialTheme.typography.bodySmall.copy(color = ConsistGrey)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = ConsistCharcoalElevated,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Calorie Goal", style = MaterialTheme.typography.labelSmall.copy(color = ConsistGrey))
                                Text(
                                    if (dailySummary.calories > 0) "${((dailySummary.calories / targetCals) * 100).toInt()}% Met" else "Tracking",
                                    style = MaterialTheme.typography.bodyMedium.copy(color = ConsistCyan, fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                        Surface(
                            color = ConsistCharcoalElevated,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Protein Goal", style = MaterialTheme.typography.labelSmall.copy(color = ConsistGrey))
                                Text(
                                    if (dailySummary.protein > 0) "${((dailySummary.protein / targetPro) * 100).toInt()}% Met" else "Tracking",
                                    style = MaterialTheme.typography.bodyMedium.copy(color = ConsistWhite, fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                        Surface(
                            color = ConsistCharcoalElevated,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Workouts", style = MaterialTheme.typography.labelSmall.copy(color = ConsistGrey))
                                Text(
                                    "${workouts.size} Logged",
                                    style = MaterialTheme.typography.bodyMedium.copy(color = ConsistWhite, fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                }
            }

            // Weight History & Trend Chart Card
            item {
                ConsistCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("WEIGHT TREND", style = MaterialTheme.typography.titleMedium.copy(color = ConsistWhite, fontWeight = FontWeight.Bold))
                            Text(
                                "Current: ${profile?.currentWeightKg ?: 70f} kg • Target: ${profile?.targetWeightKg ?: 75f} kg",
                                style = MaterialTheme.typography.bodySmall.copy(color = ConsistCyan)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Dynamic Trend Canvas Chart
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .background(ConsistCharcoalElevated, RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        if (weightEntries.isEmpty()) {
                            Text("Log weight check-ins to view historical trend curve", color = ConsistGrey, fontSize = 12.sp, modifier = Modifier.align(Alignment.Center))
                        } else {
                            val weights = weightEntries.take(7).reversed().map { it.weightKg }
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val minW = weights.minOrNull() ?: 60f
                                val maxW = max(minW + 5f, weights.maxOrNull() ?: 80f)
                                val range = max(1f, maxW - minW)

                                val stepX = if (weights.size > 1) size.width / (weights.size - 1) else size.width / 2

                                val points = weights.mapIndexed { idx, w ->
                                    val x = if (weights.size > 1) idx * stepX else size.width / 2
                                    val y = size.height - (((w - minW) / range) * (size.height - 20f)) - 10f
                                    Offset(x, y)
                                }

                                // Draw line connecting points
                                for (i in 0 until points.size - 1) {
                                    drawLine(
                                        color = ConsistCyan,
                                        start = points[i],
                                        end = points[i + 1],
                                        strokeWidth = 3.dp.toPx()
                                    )
                                }

                                // Draw circles on points
                                points.forEach { pt ->
                                    drawCircle(
                                        color = ConsistCyan,
                                        radius = 5.dp.toPx(),
                                        center = pt
                                    )
                                    drawCircle(
                                        color = ConsistBlack,
                                        radius = 2.dp.toPx(),
                                        center = pt
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Recent Weight Entries List
                    if (weightEntries.isNotEmpty()) {
                        weightEntries.take(4).forEach { entry ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(entry.date, color = ConsistGrey, fontSize = 12.sp)
                                Text("${entry.weightKg} kg", color = ConsistWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            // Macro Adherence Distribution Card
            item {
                ConsistCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "MACRONUTRIENT DISTRIBUTION",
                        style = MaterialTheme.typography.titleMedium.copy(color = ConsistWhite, fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    val proCals = dailySummary.protein * 4f
                    val carbCals = dailySummary.carbs * 4f
                    val fatCals = dailySummary.fat * 9f
                    val totalMacroCals = max(1f, proCals + carbCals + fatCals)

                    val proPct = ((proCals / totalMacroCals) * 100).toInt()
                    val carbPct = ((carbCals / totalMacroCals) * 100).toInt()
                    val fatPct = 100 - proPct - carbPct

                    // Segmented Macro Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(14.dp)
                            .clip(RoundedCornerShape(7.dp))
                    ) {
                        if (proPct > 0) {
                            Box(modifier = Modifier.weight(max(1f, proPct.toFloat())).fillMaxHeight().background(ConsistCyan))
                        }
                        if (carbPct > 0) {
                            Box(modifier = Modifier.weight(max(1f, carbPct.toFloat())).fillMaxHeight().background(Color(0xFF64FFDA)))
                        }
                        if (fatPct > 0) {
                            Box(modifier = Modifier.weight(max(1f, fatPct.toFloat())).fillMaxHeight().background(Color(0xFF80DEEA)))
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("● Protein: $proPct%", color = ConsistCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("● Carbs: $carbPct%", color = Color(0xFF64FFDA), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("● Fats: $fatPct%", color = Color(0xFF80DEEA), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Milestone Badges Card
            item {
                ConsistCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "ACHIEVEMENT MILESTONES",
                        style = MaterialTheme.typography.titleMedium.copy(color = ConsistCyan, fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AchievementBadgeItem("🛡️", "Day 1 Recruit", isUnlocked = true, modifier = Modifier.weight(1f))
                        AchievementBadgeItem("⚡", "Consistency", isUnlocked = currentStreak >= 3, modifier = Modifier.weight(1f))
                        AchievementBadgeItem("💧", "Hydrated", isUnlocked = waterIntake >= (profile?.targetWaterMl ?: 2500), modifier = Modifier.weight(1f))
                        AchievementBadgeItem("🏋️", "Lifter", isUnlocked = workouts.isNotEmpty(), modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }

    // Log Weight Dialog
    if (showLogWeightModal) {
        var inputWeight by remember { mutableStateOf(profile?.currentWeightKg?.toString() ?: "72") }
        var notesText by remember { mutableStateOf("Morning empty stomach") }

        AlertDialog(
            onDismissRequest = { showLogWeightModal = false },
            title = { Text("Log Weight Check-in", color = ConsistWhite, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = inputWeight,
                        onValueChange = { inputWeight = it },
                        label = { Text("Weight (kg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ConsistCyan, unfocusedBorderColor = ConsistBorder),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = notesText,
                        onValueChange = { notesText = it },
                        label = { Text("Notes (e.g. Morning, post-workout)") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ConsistCyan, unfocusedBorderColor = ConsistBorder),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val w = inputWeight.toFloatOrNull()
                        if (w != null && w > 0f) {
                            viewModel.logWeight(w, notesText)
                        }
                        showLogWeightModal = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ConsistCyan, contentColor = ConsistBlack)
                ) {
                    Text("Save Check-in", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogWeightModal = false }) { Text("Cancel", color = ConsistGrey) }
            },
            containerColor = ConsistCharcoalDark
        )
    }
}

@Composable
fun AchievementBadgeItem(
    icon: String,
    title: String,
    isUnlocked: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (isUnlocked) ConsistCharcoalElevated else ConsistCharcoalCard.copy(alpha = 0.5f),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isUnlocked) ConsistCyan.copy(alpha = 0.4f) else ConsistBorder),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(icon, fontSize = 20.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = if (isUnlocked) ConsistWhite else ConsistGrey,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )
            )
        }
    }
}
