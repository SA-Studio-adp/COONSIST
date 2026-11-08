package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ai.AiScannedFoodResult
import com.example.ui.components.ConsistCard
import com.example.ui.components.ConsistPrimaryButton
import com.example.ui.theme.*
import com.example.ui.viewmodel.AiScanUiState
import com.example.ui.viewmodel.ConsistViewModel

@Composable
fun AiScannerScreen(
    viewModel: ConsistViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scanState by viewModel.scanState.collectAsState()

    var selectedMealType by remember { mutableStateOf("Lunch") }
    var editableItems by remember { mutableStateOf<List<AiScannedFoodResult>>(emptyList()) }

    // Photo Picker (Android standard zero-permission)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, it)) { decoder, _, _ ->
                        decoder.isMutableRequired = true
                    }
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, it)
                }
                viewModel.scanImage(bitmap)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Camera Launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        bitmap?.let {
            viewModel.scanImage(it)
        }
    }

    // Sync editable items when scan succeeds
    LaunchedEffect(scanState) {
        if (scanState is AiScanUiState.Success) {
            editableItems = (scanState as AiScanUiState.Success).response.detectedItems
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ConsistBlack)
            .statusBarsPadding()
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                viewModel.resetScanState()
                onBack()
            }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = ConsistCyan)
            }
            Text(
                text = "AI MEAL SCANNER",
                style = MaterialTheme.typography.titleLarge.copy(
                    color = ConsistWhite,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            )
        }

        when (val state = scanState) {
            is AiScanUiState.Idle -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(ConsistCharcoalCard)
                            .border(2.dp, ConsistCyan, RoundedCornerShape(24.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = "Camera",
                            tint = ConsistCyan,
                            modifier = Modifier.size(56.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Snap or Upload Your Meal",
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = ConsistWhite,
                            fontWeight = FontWeight.Bold
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Our multimodal AI analyzes your plate, recognizes multiple food items, estimates portion sizes, and computes exact nutrition deterministically.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = ConsistGrey,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    ConsistPrimaryButton(
                        text = "Take Food Photo",
                        onClick = { cameraLauncher.launch() }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                androidx.activity.result.PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ConsistCyan),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(ConsistCyan, ConsistCyanDim))
                        )
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pick From Gallery", fontWeight = FontWeight.Bold)
                    }
                }
            }

            is AiScanUiState.Loading -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(color = ConsistCyan, strokeWidth = 4.dp, modifier = Modifier.size(56.dp))
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        "Analyzing Plate with AI...",
                        style = MaterialTheme.typography.titleMedium.copy(color = ConsistWhite, fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Detecting individual items & estimating portion grams...",
                        style = MaterialTheme.typography.bodySmall.copy(color = ConsistGrey)
                    )
                }
            }

            is AiScanUiState.Success -> {
                val totalCal = editableItems.sumOf { it.calories.toDouble() }.toFloat()
                val totalPro = editableItems.sumOf { it.protein.toDouble() }.toFloat()
                val totalCarb = editableItems.sumOf { it.carbs.toDouble() }.toFloat()
                val totalFat = editableItems.sumOf { it.fat.toDouble() }.toFloat()

                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(bottom = 30.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Image Preview & Overview
                    item {
                        ConsistCard(modifier = Modifier.fillMaxWidth()) {
                            Image(
                                bitmap = state.imageBitmap.asImageBitmap(),
                                contentDescription = "Scanned Meal",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = state.response.mealOverview,
                                style = MaterialTheme.typography.titleMedium.copy(color = ConsistCyan, fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "⚠️ Visual portions are estimates. Adjust weights in grams below before confirming.",
                                style = MaterialTheme.typography.bodySmall.copy(color = ConsistGreyLight)
                            )
                        }
                    }

                    // Target Meal Selector
                    item {
                        Text(
                            text = "Log to Meal:",
                            style = MaterialTheme.typography.labelMedium.copy(color = ConsistGrey, fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("Breakfast", "Lunch", "Dinner", "Snack").forEach { meal ->
                                val selected = selectedMealType.contains(meal, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (selected) ConsistCyan else ConsistCharcoalCard)
                                        .border(1.dp, if (selected) ConsistCyan else ConsistBorder, RoundedCornerShape(8.dp))
                                        .clickable { selectedMealType = meal },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        meal,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = if (selected) ConsistBlack else ConsistWhite,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // Total Computed Nutrition Card
                    item {
                        Surface(
                            color = ConsistCharcoalElevated,
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ConsistCyan.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Total Calories", style = MaterialTheme.typography.labelSmall.copy(color = ConsistGrey))
                                    Text("${totalCal.toInt()} kcal", style = MaterialTheme.typography.titleLarge.copy(color = ConsistCyan, fontWeight = FontWeight.Bold))
                                }
                                Column {
                                    Text("Protein", style = MaterialTheme.typography.labelSmall.copy(color = ConsistGrey))
                                    Text("${totalPro.toInt()}g", style = MaterialTheme.typography.titleMedium.copy(color = ConsistWhite, fontWeight = FontWeight.Bold))
                                }
                                Column {
                                    Text("Carbs", style = MaterialTheme.typography.labelSmall.copy(color = ConsistGrey))
                                    Text("${totalCarb.toInt()}g", style = MaterialTheme.typography.titleMedium.copy(color = ConsistWhite, fontWeight = FontWeight.Bold))
                                }
                                Column {
                                    Text("Fat", style = MaterialTheme.typography.labelSmall.copy(color = ConsistGrey))
                                    Text("${totalFat.toInt()}g", style = MaterialTheme.typography.titleMedium.copy(color = ConsistWhite, fontWeight = FontWeight.Bold))
                                }
                            }
                        }
                    }

                    // Detected Items with Portion Adjusters
                    item {
                        Text(
                            text = "DETECTED FOOD ITEMS (${editableItems.size})",
                            style = MaterialTheme.typography.labelMedium.copy(color = ConsistCyan, fontWeight = FontWeight.Bold)
                        )
                    }

                    itemsIndexed(editableItems) { index, item ->
                        ConsistCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.foodName,
                                        style = MaterialTheme.typography.bodyLarge.copy(color = ConsistWhite, fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "${item.calories.toInt()} kcal • P: ${item.protein.toInt()}g • C: ${item.carbs.toInt()}g • F: ${item.fat.toInt()}g",
                                        style = MaterialTheme.typography.bodySmall.copy(color = ConsistCyan)
                                    )
                                    Text(
                                        text = item.uncertaintyNote,
                                        style = MaterialTheme.typography.labelSmall.copy(color = ConsistGrey)
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        editableItems = editableItems.filterIndexed { i, _ -> i != index }
                                    }
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove", tint = ConsistError)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Portion Adjuster Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Weight (g):", style = MaterialTheme.typography.bodySmall.copy(color = ConsistWhite))
                                OutlinedTextField(
                                    value = item.estimatedGrams.toInt().toString(),
                                    onValueChange = { newText ->
                                        val newGrams = newText.toFloatOrNull() ?: item.estimatedGrams
                                        val ratio = if (item.estimatedGrams > 0) newGrams / item.estimatedGrams else 1f
                                        val updated = item.copy(
                                            estimatedGrams = newGrams,
                                            calories = item.calories * ratio,
                                            protein = item.protein * ratio,
                                            carbs = item.carbs * ratio,
                                            fat = item.fat * ratio,
                                            servingDescription = "${newGrams.toInt()}g"
                                        )
                                        editableItems = editableItems.mapIndexed { i, existing -> if (i == index) updated else existing }
                                    },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = ConsistCyan,
                                        unfocusedBorderColor = ConsistBorder
                                    ),
                                    modifier = Modifier.width(100.dp).height(50.dp)
                                )
                            }
                        }
                    }

                    // Save / Rescan Buttons
                    item {
                        ConsistPrimaryButton(
                            text = "Confirm & Save Meal to Diary",
                            onClick = {
                                viewModel.confirmSaveScannedMeal(selectedMealType, editableItems)
                                onBack()
                            }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        TextButton(
                            onClick = { viewModel.resetScanState() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Retake Photo / Scan Another", color = ConsistGrey)
                        }
                    }
                }
            }

            is AiScanUiState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        if (state.isConsentRequired) Icons.Default.Security else Icons.Default.Info,
                        contentDescription = null,
                        tint = if (state.isConsentRequired) ConsistCyan else ConsistWarning,
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (state.isConsentRequired) "AI Cloud Consent Required" else if (state.isBackendUnconfigured) "AI Backend Not Configured" else "Photo Recognition Unavailable",
                        color = ConsistWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = state.message,
                        color = ConsistGrey,
                        fontSize = 14.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    if (state.isConsentRequired) {
                        ConsistPrimaryButton(
                            text = "Grant AI Cloud Processing Consent",
                            onClick = {
                                viewModel.grantAiConsent()
                                viewModel.resetScanState()
                            }
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    ConsistPrimaryButton(
                        text = "+ Enter Food Manually (Verified Database)",
                        onClick = {
                            viewModel.resetScanState()
                            onBack()
                        }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = { viewModel.resetScanState() },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ConsistCyan),
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) {
                        Text("Retake Photo", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
