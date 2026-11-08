package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.calculation.NutritionCalculator
import com.example.data.model.UserProfile
import com.example.ui.components.ConsistCard
import com.example.ui.components.ConsistOutlinedButton
import com.example.ui.components.ConsistPrimaryButton
import com.example.ui.theme.*
import com.example.ui.viewmodel.ConsistViewModel

@Composable
fun ProfileScreen(
    viewModel: ConsistViewModel,
    onEditProfile: () -> Unit
) {
    val profile by viewModel.userProfile.collectAsState()
    var showMethodologyModal by remember { mutableStateOf(false) }
    var showPrivacyModal by remember { mutableStateOf(false) }

    val bmr = profile?.let {
        NutritionCalculator.calculateBMR(it.currentWeightKg, it.heightCm, it.age, it.gender)
    } ?: 1700f

    val tdee = profile?.let {
        bmr * NutritionCalculator.getActivityMultiplier(it.activityLevel)
    } ?: 2400f

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
                    text = "ATHLETE PROFILE",
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = ConsistWhite,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                )
                Text(
                    text = "CONSIST: BUILD. TRAIN. REPEAT.",
                    style = MaterialTheme.typography.labelSmall.copy(color = ConsistCyan, fontWeight = FontWeight.Bold)
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Profile Card
            item {
                ConsistCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(ConsistCyanContainer)
                                .border(2.dp, ConsistCyan, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = (profile?.name?.take(1)?.uppercase() ?: "A"),
                                color = ConsistCyan,
                                fontWeight = FontWeight.Black,
                                fontSize = 26.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = profile?.name?.ifBlank { "Athlete" } ?: "Athlete",
                                style = MaterialTheme.typography.titleLarge.copy(color = ConsistWhite, fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "${profile?.age ?: 25} yrs • ${profile?.gender ?: "Male"} • ${profile?.heightCm?.toInt() ?: 175} cm",
                                style = MaterialTheme.typography.bodySmall.copy(color = ConsistGrey)
                            )
                            Text(
                                text = "Goal: ${profile?.primaryGoal ?: "Muscle gain"}",
                                style = MaterialTheme.typography.labelSmall.copy(color = ConsistCyan, fontWeight = FontWeight.Bold)
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
                                Text("Current Weight", style = MaterialTheme.typography.labelSmall.copy(color = ConsistGrey))
                                Text("${profile?.currentWeightKg ?: 70f} kg", style = MaterialTheme.typography.bodyMedium.copy(color = ConsistWhite, fontWeight = FontWeight.Bold))
                            }
                        }
                        Surface(
                            color = ConsistCharcoalElevated,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Target Weight", style = MaterialTheme.typography.labelSmall.copy(color = ConsistGrey))
                                Text("${profile?.targetWeightKg ?: 75f} kg", style = MaterialTheme.typography.bodyMedium.copy(color = ConsistCyan, fontWeight = FontWeight.Bold))
                            }
                        }
                    }
                }
            }

            // Metabolic & Target Summary
            item {
                ConsistCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "METABOLIC ENGINE (MIFFLIN-ST JEOR)",
                        style = MaterialTheme.typography.labelMedium.copy(color = ConsistCyan, fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Basal Metabolic Rate (BMR)", style = MaterialTheme.typography.bodySmall.copy(color = ConsistGrey))
                            Text("${bmr.toInt()} kcal/day", style = MaterialTheme.typography.titleMedium.copy(color = ConsistWhite, fontWeight = FontWeight.Bold))
                        }
                        Column {
                            Text("Maintenance (TDEE)", style = MaterialTheme.typography.bodySmall.copy(color = ConsistGrey))
                            Text("${tdee.toInt()} kcal/day", style = MaterialTheme.typography.titleMedium.copy(color = ConsistWhite, fontWeight = FontWeight.Bold))
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = ConsistBorder)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Target Calories", style = MaterialTheme.typography.bodySmall.copy(color = ConsistGrey))
                            Text("${profile?.targetCalories ?: 2400} kcal", style = MaterialTheme.typography.titleLarge.copy(color = ConsistCyan, fontWeight = FontWeight.Bold))
                        }
                        Column {
                            Text("Target Protein", style = MaterialTheme.typography.bodySmall.copy(color = ConsistGrey))
                            Text("${profile?.targetProteinGrams?.toInt() ?: 140}g (2.0g/kg)", style = MaterialTheme.typography.titleMedium.copy(color = ConsistWhite, fontWeight = FontWeight.Bold))
                        }
                    }
                }
            }

            // Dietary & Lifestyle Settings
            item {
                ConsistCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "DIETARY PREFERENCES & SAFEGUARDS",
                        style = MaterialTheme.typography.labelMedium.copy(color = ConsistCyan, fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Dietary Type: ${profile?.dietaryPreference} ${if (profile?.dietaryPreference == "Vegetarian" && profile?.eatsEggs == true) "(Includes Eggs)" else ""}", color = ConsistWhite, fontSize = 13.sp)
                    Text("Activity Level: ${profile?.activityLevel}", color = ConsistWhite, fontSize = 13.sp)
                    Text("Cuisines: ${profile?.preferredCuisines}", color = ConsistWhite, fontSize = 13.sp)
                    Text("Allergies: ${profile?.allergies}", color = ConsistGrey, fontSize = 13.sp)
                    Text("Foods to Avoid: ${profile?.foodsToAvoid}", color = ConsistGrey, fontSize = 13.sp)

                    Spacer(modifier = Modifier.height(12.dp))

                    ConsistPrimaryButton(
                        text = "Edit Profile & Goals",
                        onClick = onEditProfile
                    )
                }
            }

            // Privacy & Security Controls
            item {
                ConsistCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = ConsistCyan.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Security, contentDescription = null, tint = ConsistCyan, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "PRIVACY & BACKEND SECURITY",
                                    style = MaterialTheme.typography.labelMedium.copy(color = ConsistCyan, fontWeight = FontWeight.Bold)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (profile?.localOnlyMode == true) "Mode: 100% On-Device Local Storage" else "Mode: Remote HTTPS Backend Connected",
                                style = MaterialTheme.typography.bodySmall.copy(color = ConsistWhite, fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = if (profile?.aiCloudConsentGiven == true) "AI Cloud Processing: Consented" else "AI Cloud Processing: Disabled",
                                style = MaterialTheme.typography.labelSmall.copy(color = ConsistGrey)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    ConsistOutlinedButton(
                        text = "Manage Privacy & Backend Settings",
                        onClick = { showPrivacyModal = true }
                    )
                }
            }

            // Calculation Transparency & Methodology
            item {
                ConsistCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { showMethodologyModal = true }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Nutritional Engine & Data Transparency",
                                style = MaterialTheme.typography.bodyLarge.copy(color = ConsistWhite, fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "USDA FoodData, IFCT & Mifflin-St Jeor standards",
                                style = MaterialTheme.typography.bodySmall.copy(color = ConsistCyan)
                            )
                        }
                        Icon(Icons.Default.Info, contentDescription = null, tint = ConsistCyan)
                    }
                }
            }

            // App Identity & Info
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("CONSIST v1.1.0 (Security & Privacy Edition)", color = ConsistCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text("BUILD. TRAIN. REPEAT.", color = ConsistGrey, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Text("Private On-Device Room Storage • Zero Client-Side Private Keys", color = ConsistGreyDark, fontSize = 10.sp)
                }
            }
        }
    }

    // Privacy & Security Settings Dialog
    if (showPrivacyModal) {
        var aiConsent by remember { mutableStateOf(profile?.aiCloudConsentGiven ?: false) }
        var localOnly by remember { mutableStateOf(profile?.localOnlyMode ?: true) }
        var backendUrl by remember { mutableStateOf(profile?.secureBackendUrl ?: "") }
        var backendToken by remember { mutableStateOf(profile?.secureBackendToken ?: "") }
        var validationError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showPrivacyModal = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = ConsistCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Privacy & Security Settings", color = ConsistWhite, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "CONSIST is built with privacy-by-design. Your logs, biometrics and workouts remain safely inside your device's private sandbox.",
                        color = ConsistGrey,
                        fontSize = 12.sp
                    )

                    // AI Cloud Consent Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("AI Cloud Processing Consent", color = ConsistWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Allow transmitting meal photos or queries to your secure proxy backend.", color = ConsistGrey, fontSize = 11.sp)
                        }
                        Switch(
                            checked = aiConsent,
                            onCheckedChange = {
                                aiConsent = it
                                if (it) localOnly = false
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = ConsistBlack, checkedTrackColor = ConsistCyan)
                        )
                    }

                    // Local-Only Mode Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Strict Local-Only (Offline) Mode", color = ConsistWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Completely disables all remote networking. All coaching & calculations run locally.", color = ConsistGrey, fontSize = 11.sp)
                        }
                        Switch(
                            checked = localOnly,
                            onCheckedChange = {
                                localOnly = it
                                if (it) aiConsent = false
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = ConsistBlack, checkedTrackColor = ConsistCyan)
                        )
                    }

                    // Backend URL Field
                    if (!localOnly) {
                        Text("Secure Backend Proxy (HTTPS):", color = ConsistCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = backendUrl,
                            onValueChange = {
                                backendUrl = it
                                validationError = null
                            },
                            placeholder = { Text("https://api.consist-app.com") },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ConsistCyan, unfocusedBorderColor = ConsistBorder),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text("Authorization Bearer Token (Optional):", color = ConsistGreyLight, fontSize = 12.sp)
                        OutlinedTextField(
                            value = backendToken,
                            onValueChange = { backendToken = it },
                            placeholder = { Text("Bearer token for your proxy") },
                            visualTransformation = PasswordVisualTransformation(),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ConsistCyan, unfocusedBorderColor = ConsistBorder),
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (validationError != null) {
                            Text(validationError!!, color = ConsistError, fontSize = 11.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmedUrl = backendUrl.trim()
                        if (!localOnly && trimmedUrl.isNotBlank()) {
                            if (trimmedUrl.startsWith("http://", ignoreCase = true)) {
                                validationError = "Insecure HTTP is forbidden. Please enter an HTTPS URL."
                                return@Button
                            }
                            if (!trimmedUrl.startsWith("https://", ignoreCase = true)) {
                                validationError = "URL must start with https://"
                                return@Button
                            }
                        }
                        viewModel.updatePrivacySettings(aiConsent, trimmedUrl, backendToken.trim(), localOnly)
                        showPrivacyModal = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ConsistCyan, contentColor = ConsistBlack)
                ) {
                    Text("Save Settings", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPrivacyModal = false }) {
                    Text("Cancel", color = ConsistGrey)
                }
            },
            containerColor = ConsistCharcoalDark
        )
    }

    // Methodology Modal
    if (showMethodologyModal) {
        AlertDialog(
            onDismissRequest = { showMethodologyModal = false },
            title = { Text("Nutritional Engine & Data Transparency", color = ConsistWhite, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("1. Deterministic Nutrient Scaling:", color = ConsistCyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("Nutrient amount = (Per 100g value × actual food weight in grams) / 100.0. Recalculated deterministically on every portion edit.", color = ConsistGrey, fontSize = 12.sp)

                    Text("2. Authoritative Composition Sources:", color = ConsistCyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("Nutritional values for Indian and Kerala foods are referenced from the Indian Food Composition Tables (IFCT) and international foods from USDA FoodData Central.", color = ConsistGrey, fontSize = 12.sp)

                    Text("3. Mifflin-St Jeor Equation:", color = ConsistCyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("Men: 10×W + 6.25×H - 5×A + 5\nWomen: 10×W + 6.25×H - 5×A - 161\nSafe floors enforced: Deficits never prescribed below 1200/1500 kcal, and minors <18 protected from energy deficits.", color = ConsistGrey, fontSize = 12.sp)

                    Text("4. AI Visual Portion Safeguards:", color = ConsistCyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("Photos provide visual scale estimates. Users inspect and confirm exact portion weights before saving to food diary.", color = ConsistGrey, fontSize = 12.sp)
                }
            },
            confirmButton = {
                Button(
                    onClick = { showMethodologyModal = false },
                    colors = ButtonDefaults.buttonColors(containerColor = ConsistCyan, contentColor = ConsistBlack)
                ) {
                    Text("Close", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = ConsistCharcoalDark
        )
    }
}
