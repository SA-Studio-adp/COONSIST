package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.ConsistViewModel

@Composable
fun HomeScreen(
    viewModel: ConsistViewModel,
    onNavigateToFood: () -> Unit,
    onNavigateToAiScan: () -> Unit,
    onNavigateToAiCoach: () -> Unit,
    onNavigateToWorkouts: () -> Unit,
    onNavigateToDietPlan: () -> Unit,
    onOpenVoiceLog: () -> Unit
) {
    val profile by viewModel.userProfile.collectAsState()
    val dailySummary by viewModel.dailySummary.collectAsState()
    val waterIntake by viewModel.currentDayWater.collectAsState()
    val latestWeight by viewModel.latestWeight.collectAsState()
    val workouts by viewModel.currentDayWorkouts.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()

    val targetCalories = profile?.targetCalories ?: 2400
    val targetProtein = profile?.targetProteinGrams ?: 140f
    val targetCarbs = profile?.targetCarbsGrams ?: 270f
    val targetFat = profile?.targetFatGrams ?: 65f
    val targetWater = profile?.targetWaterMl ?: 3000

    val remainingCalories = (targetCalories - dailySummary.calories).coerceAtLeast(0f)
    val totalWorkoutBurn = workouts.sumOf { it.caloriesBurned }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ConsistBlack)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 90.dp) // padding for bottom nav
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "CONSIST",
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = ConsistCyan,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp
                    )
                )
                Text(
                    text = "Hello, ${profile?.name?.ifBlank { "Athlete" } ?: "Athlete"}",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        color = ConsistWhite,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            // Streak Pill
            Surface(
                color = ConsistCharcoalCard,
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ConsistCyan.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("🔥", fontSize = 16.sp)
                    Text(
                        text = "${profile?.currentStreak ?: 1} DAY STREAK",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = ConsistCyan,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    )
                }
            }
        }

        // Hero Motivation Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .height(130.dp)
                .clip(RoundedCornerShape(16.dp))
        ) {
            Image(
                painter = painterResource(id = R.drawable.hero_consist_banner_1793983442186),
                contentDescription = "Fitness Banner",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                ConsistBlack.copy(alpha = 0.9f),
                                ConsistBlack.copy(alpha = 0.4f),
                                Color.Transparent
                            )
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(16.dp)
            ) {
                Text(
                    text = "BUILD. TRAIN. REPEAT.",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = ConsistCyan,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Personalized AI Target for ${profile?.primaryGoal ?: "Fitness"}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = ConsistWhite,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Precision Mifflin-St Jeor engine active",
                    style = MaterialTheme.typography.labelSmall.copy(color = ConsistGreyLight)
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Calorie Progress Ring & Macro Summary Card
        NeonGlowCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TODAY'S ENERGY BALANCE",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = ConsistCyan,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                )
                Text(
                    text = selectedDate,
                    style = MaterialTheme.typography.bodySmall.copy(color = ConsistGrey)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                CalorieProgressRing(
                    consumed = dailySummary.calories,
                    target = targetCalories.toFloat(),
                    remaining = remainingCalories,
                    size = 160.dp,
                    strokeWidth = 12.dp
                )

                Column(
                    modifier = Modifier.weight(1f).padding(start = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MacroProgressBar(
                        label = "Protein",
                        current = dailySummary.protein,
                        target = targetProtein,
                        accentColor = ConsistCyan
                    )
                    MacroProgressBar(
                        label = "Carbs",
                        current = dailySummary.carbs,
                        target = targetCarbs,
                        accentColor = Color(0xFF64FFDA)
                    )
                    MacroProgressBar(
                        label = "Fats",
                        current = dailySummary.fat,
                        target = targetFat,
                        accentColor = Color(0xFF80DEEA)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Calorie summary row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ConsistCharcoalElevated, RoundedCornerShape(10.dp))
                    .padding(vertical = 10.dp, horizontal = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Target", style = MaterialTheme.typography.labelSmall.copy(color = ConsistGrey))
                    Text("${targetCalories}", style = MaterialTheme.typography.bodyMedium.copy(color = ConsistWhite, fontWeight = FontWeight.Bold))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Consumed", style = MaterialTheme.typography.labelSmall.copy(color = ConsistGrey))
                    Text("${dailySummary.calories.toInt()}", style = MaterialTheme.typography.bodyMedium.copy(color = ConsistCyan, fontWeight = FontWeight.Bold))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Workout Burn", style = MaterialTheme.typography.labelSmall.copy(color = ConsistGrey))
                    Text("${totalWorkoutBurn} kcal", style = MaterialTheme.typography.bodyMedium.copy(color = ConsistWhite, fontWeight = FontWeight.Bold))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Net Left", style = MaterialTheme.typography.labelSmall.copy(color = ConsistGrey))
                    Text("${(remainingCalories + totalWorkoutBurn).toInt()}", style = MaterialTheme.typography.bodyMedium.copy(color = ConsistWhite, fontWeight = FontWeight.Bold))
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Quick Action Shortcuts Grid
        Text(
            text = "FAST ACTIONS",
            style = MaterialTheme.typography.labelMedium.copy(
                color = ConsistCyan,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            ),
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // AI Food Scanner Button
            ConsistCard(
                modifier = Modifier.weight(1f),
                borderColor = ConsistCyan.copy(alpha = 0.5f),
                onClick = onNavigateToAiScan
            ) {
                Icon(
                    Icons.Default.CameraAlt,
                    contentDescription = "AI Scanner",
                    tint = ConsistCyan,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "AI Scanner",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = ConsistWhite,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = "Photo meal recognition",
                    style = MaterialTheme.typography.labelSmall.copy(color = ConsistGrey)
                )
            }

            // Voice Food Log
            ConsistCard(
                modifier = Modifier.weight(1f),
                onClick = onOpenVoiceLog
            ) {
                Icon(
                    Icons.Default.Mic,
                    contentDescription = "Voice Log",
                    tint = ConsistCyan,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Voice Log",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = ConsistWhite,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = "Speak your meals",
                    style = MaterialTheme.typography.labelSmall.copy(color = ConsistGrey)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // AI Coach
            ConsistCard(
                modifier = Modifier.weight(1f),
                onClick = onNavigateToAiCoach
            ) {
                Icon(
                    Icons.Default.Psychology,
                    contentDescription = "AI Coach",
                    tint = ConsistCyan,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "AI Coach",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = ConsistWhite,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = "Diet & fitness guidance",
                    style = MaterialTheme.typography.labelSmall.copy(color = ConsistGrey)
                )
            }

            // AI Diet Planner
            ConsistCard(
                modifier = Modifier.weight(1f),
                onClick = onNavigateToDietPlan
            ) {
                Icon(
                    Icons.Default.MenuBook,
                    contentDescription = "Diet Plan",
                    tint = ConsistCyan,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Diet Plan",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = ConsistWhite,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = "Personalized schedule",
                    style = MaterialTheme.typography.labelSmall.copy(color = ConsistGrey)
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Water Tracker Widget
        ConsistCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.WaterDrop,
                        contentDescription = "Water",
                        tint = ConsistCyan,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "HYDRATION TRACKER",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = ConsistWhite,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Text(
                    text = "$waterIntake / $targetWater ml",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = ConsistCyan,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Water progress bar
            val waterProgress = if (targetWater > 0) (waterIntake.toFloat() / targetWater).coerceIn(0f, 1f) else 0f
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(ConsistCharcoalElevated)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(waterProgress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(5.dp))
                        .background(ConsistCyan)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Quick add buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.addWater(250) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ConsistCyan),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = Brush.horizontalGradient(listOf(ConsistCyan, ConsistCyanDim))
                    )
                ) {
                    Text("+250 ml", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = { viewModel.addWater(500) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ConsistCyan),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = Brush.horizontalGradient(listOf(ConsistCyan, ConsistCyanDim))
                    )
                ) {
                    Text("+500 ml", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Workout & Weight Snapshot Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Workout Summary
            ConsistCard(
                modifier = Modifier.weight(1f),
                onClick = onNavigateToWorkouts
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.FitnessCenter,
                        contentDescription = "Workout",
                        tint = ConsistCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "WORKOUTS",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = ConsistCyan,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (workouts.isEmpty()) "No workouts today" else "${workouts.size} session(s) logged",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = ConsistWhite,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = if (totalWorkoutBurn > 0) "$totalWorkoutBurn kcal burned" else "Tap to log training",
                    style = MaterialTheme.typography.bodySmall.copy(color = ConsistGrey)
                )
            }

            // Weight Summary
            ConsistCard(
                modifier = Modifier.weight(1f)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.MonitorWeight,
                        contentDescription = "Weight",
                        tint = ConsistCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "WEIGHT",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = ConsistCyan,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "${latestWeight?.weightKg ?: profile?.currentWeightKg ?: "--"} kg",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = ConsistWhite,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = "Target: ${profile?.targetWeightKg ?: "--"} kg",
                    style = MaterialTheme.typography.bodySmall.copy(color = ConsistGrey)
                )
            }
        }
    }
}
