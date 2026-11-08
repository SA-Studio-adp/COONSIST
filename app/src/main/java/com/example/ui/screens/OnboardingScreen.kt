package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.ConsistCard
import com.example.ui.components.ConsistOutlinedButton
import com.example.ui.components.ConsistPrimaryButton
import com.example.ui.theme.*
import com.example.ui.viewmodel.ConsistViewModel

@Composable
fun OnboardingScreen(
    viewModel: ConsistViewModel,
    onFinished: () -> Unit
) {
    var step by remember { mutableStateOf(1) }

    // Step 1: Basics & Biometrics
    var name by remember { mutableStateOf("") }
    var ageText by remember { mutableStateOf("25") }
    var gender by remember { mutableStateOf("Male") }
    var heightText by remember { mutableStateOf("175") }
    var weightText by remember { mutableStateOf("72") }
    var targetWeightText by remember { mutableStateOf("75") }

    // Step 2: Fitness Goals & Activity
    var primaryGoal by remember { mutableStateOf("Muscle gain") }
    var activityLevel by remember { mutableStateOf("Moderately Active") }
    var exerciseFrequency by remember { mutableStateOf("3-4 days/week") }
    var fitnessExperience by remember { mutableStateOf("Intermediate") }

    // Step 3: Dietary Preferences & Cuisines
    var dietaryPreference by remember { mutableStateOf("Non-vegetarian") }
    var eatsEggs by remember { mutableStateOf(true) }
    var allergies by remember { mutableStateOf("") }
    var intolerances by remember { mutableStateOf("") }
    var foodsToAvoid by remember { mutableStateOf("") }
    var dislikedFoods by remember { mutableStateOf("") }
    var preferredCuisines by remember { mutableStateOf("Indian, Kerala, Continental") }
    var foodBudget by remember { mutableStateOf("Medium") }
    var mealsPerDay by remember { mutableIntStateOf(4) }

    val goals = listOf(
        "Muscle gain",
        "Fat loss",
        "Strength improvement",
        "Healthy weight gain",
        "Endurance",
        "Weight maintenance",
        "General fitness"
    )

    val activityLevels = listOf(
        "Sedentary" to "Desk job, little exercise",
        "Lightly Active" to "Light exercise 1-3 days/wk",
        "Moderately Active" to "Moderate training 3-5 days/wk",
        "Very Active" to "Heavy training 6-7 days/wk",
        "Extra Active" to "Physical job & hard training"
    )

    val dietaryTypes = listOf("Non-vegetarian", "Vegetarian", "Vegan")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ConsistBlack)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Top Hero Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.hero_consist_banner_1793983442186),
                contentDescription = "CONSIST Athletic Banner",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            listOf(
                                androidx.compose.ui.graphics.Color.Transparent,
                                ConsistBlack.copy(alpha = 0.85f),
                                ConsistBlack
                            )
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            ) {
                Text(
                    text = "CONSIST",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        color = ConsistCyan,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                )
                Text(
                    text = "BUILD. TRAIN. REPEAT.",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = ConsistWhite,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                )
            }
        }

        // Stepper indicator
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Step $step of 3",
                style = MaterialTheme.typography.labelLarge.copy(
                    color = ConsistCyan,
                    fontWeight = FontWeight.Bold
                )
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(3) { index ->
                    Box(
                        modifier = Modifier
                            .width(if (step == index + 1) 28.dp else 14.dp)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (step >= index + 1) ConsistCyan else ConsistBorder)
                    )
                }
            }
        }

        // Scrollable content body
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (step) {
                1 -> {
                    Text(
                        text = "Biometrics & Basics",
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = ConsistWhite,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = "Used in the scientific Mifflin-St Jeor formula to determine your exact metabolic rate.",
                        style = MaterialTheme.typography.bodySmall.copy(color = ConsistGrey)
                    )

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Your Name / Athlete Tag") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ConsistCyan,
                            unfocusedBorderColor = ConsistBorder,
                            focusedLabelColor = ConsistCyan,
                            cursorColor = ConsistCyan
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = ageText,
                            onValueChange = { ageText = it },
                            label = { Text("Age") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ConsistCyan,
                                unfocusedBorderColor = ConsistBorder,
                                focusedLabelColor = ConsistCyan
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        // Gender selection
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Gender",
                                style = MaterialTheme.typography.labelSmall.copy(color = ConsistGrey)
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf("Male", "Female").forEach { g ->
                                    val selected = gender.equals(g, ignoreCase = true)
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(52.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (selected) ConsistCyan else ConsistCharcoalCard)
                                            .border(1.dp, if (selected) ConsistCyan else ConsistBorder, RoundedCornerShape(8.dp))
                                            .clickable { gender = g },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            g,
                                            fontWeight = FontWeight.Bold,
                                            color = if (selected) ConsistBlack else ConsistWhite
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = heightText,
                            onValueChange = { heightText = it },
                            label = { Text("Height (cm)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ConsistCyan,
                                unfocusedBorderColor = ConsistBorder
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = weightText,
                            onValueChange = { weightText = it },
                            label = { Text("Weight (kg)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ConsistCyan,
                                unfocusedBorderColor = ConsistBorder
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = targetWeightText,
                        onValueChange = { targetWeightText = it },
                        label = { Text("Target Goal Weight (kg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ConsistCyan,
                            unfocusedBorderColor = ConsistBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                2 -> {
                    Text(
                        text = "Fitness Goal & Activity Level",
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = ConsistWhite,
                            fontWeight = FontWeight.Bold
                        )
                    )

                    Text(
                        "Primary Goal",
                        style = MaterialTheme.typography.titleMedium.copy(color = ConsistCyan, fontWeight = FontWeight.Bold)
                    )
                    goals.forEach { g ->
                        val selected = primaryGoal == g
                        ConsistCard(
                            backgroundColor = if (selected) ConsistCharcoalElevated else ConsistCharcoalCard,
                            borderColor = if (selected) ConsistCyan else ConsistBorder,
                            onClick = { primaryGoal = g },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    g,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        color = if (selected) ConsistCyan else ConsistWhite,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                                if (selected) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = ConsistCyan
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Activity Level",
                        style = MaterialTheme.typography.titleMedium.copy(color = ConsistCyan, fontWeight = FontWeight.Bold)
                    )
                    activityLevels.forEach { (level, desc) ->
                        val selected = activityLevel == level
                        ConsistCard(
                            backgroundColor = if (selected) ConsistCharcoalElevated else ConsistCharcoalCard,
                            borderColor = if (selected) ConsistCyan else ConsistBorder,
                            onClick = { activityLevel = level },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text(
                                    level,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = if (selected) ConsistCyan else ConsistWhite,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    desc,
                                    style = MaterialTheme.typography.bodySmall.copy(color = ConsistGrey)
                                )
                            }
                        }
                    }
                }

                3 -> {
                    Text(
                        text = "Dietary Preferences & Nutrition",
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = ConsistWhite,
                            fontWeight = FontWeight.Bold
                        )
                    )

                    Text(
                        "Dietary Preference",
                        style = MaterialTheme.typography.titleMedium.copy(color = ConsistCyan, fontWeight = FontWeight.Bold)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        dietaryTypes.forEach { type ->
                            val selected = dietaryPreference == type
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selected) ConsistCyan else ConsistCharcoalCard)
                                    .border(1.dp, if (selected) ConsistCyan else ConsistBorder, RoundedCornerShape(8.dp))
                                    .clickable { dietaryPreference = type },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    type,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (selected) ConsistBlack else ConsistWhite
                                    )
                                )
                            }
                        }
                    }

                    if (dietaryPreference == "Vegetarian") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Include Eggs (Eggetarian)",
                                style = MaterialTheme.typography.bodyMedium.copy(color = ConsistWhite)
                            )
                            Switch(
                                checked = eatsEggs,
                                onCheckedChange = { eatsEggs = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = ConsistBlack,
                                    checkedTrackColor = ConsistCyan
                                )
                            )
                        }
                    }

                    OutlinedTextField(
                        value = preferredCuisines,
                        onValueChange = { preferredCuisines = it },
                        label = { Text("Preferred Cuisines (e.g. Kerala, Indian, Continental)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ConsistCyan,
                            unfocusedBorderColor = ConsistBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = allergies,
                        onValueChange = { allergies = it },
                        label = { Text("Allergies (e.g. Peanuts, Shellfish, Gluten)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ConsistCyan,
                            unfocusedBorderColor = ConsistBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = foodsToAvoid,
                        onValueChange = { foodsToAvoid = it },
                        label = { Text("Foods to Avoid / Disliked") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ConsistCyan,
                            unfocusedBorderColor = ConsistBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Meals per day: $mealsPerDay",
                            style = MaterialTheme.typography.bodyMedium.copy(color = ConsistWhite, fontWeight = FontWeight.Bold)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(3, 4, 5).forEach { count ->
                                val selected = mealsPerDay == count
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (selected) ConsistCyan else ConsistCharcoalCard)
                                        .border(1.dp, if (selected) ConsistCyan else ConsistBorder, RoundedCornerShape(8.dp))
                                        .clickable { mealsPerDay = count },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        count.toString(),
                                        fontWeight = FontWeight.Bold,
                                        color = if (selected) ConsistBlack else ConsistWhite
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bottom Navigation Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (step > 1) {
                ConsistOutlinedButton(
                    text = "Back",
                    onClick = { step-- },
                    modifier = Modifier.weight(1f)
                )
            }

            ConsistPrimaryButton(
                text = if (step == 3) "Start CONSIST" else "Continue",
                onClick = {
                    if (step < 3) {
                        step++
                    } else {
                        val age = ageText.toIntOrNull() ?: 25
                        val height = heightText.toFloatOrNull() ?: 175f
                        val weight = weightText.toFloatOrNull() ?: 72f
                        val targetWeight = targetWeightText.toFloatOrNull() ?: weight

                        viewModel.completeOnboarding(
                            name = name,
                            age = age,
                            gender = gender,
                            heightCm = height,
                            currentWeightKg = weight,
                            targetWeightKg = targetWeight,
                            activityLevel = activityLevel,
                            exerciseFrequency = exerciseFrequency,
                            fitnessExperience = fitnessExperience,
                            primaryGoal = primaryGoal,
                            dietaryPreference = dietaryPreference,
                            eatsEggs = eatsEggs,
                            allergies = allergies,
                            foodIntolerances = intolerances,
                            foodsToAvoid = foodsToAvoid,
                            dislikedFoods = dislikedFoods,
                            preferredCuisines = preferredCuisines,
                            foodBudget = foodBudget,
                            mealsPerDay = mealsPerDay
                        )
                        onFinished()
                    }
                },
                modifier = Modifier.weight(if (step > 1) 1.5f else 1f)
            )
        }
    }
}
