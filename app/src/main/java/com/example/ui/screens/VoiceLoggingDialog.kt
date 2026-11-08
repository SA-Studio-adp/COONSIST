package com.example.ui.screens

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ai.AiScannedFoodResult
import com.example.ui.components.ConsistCard
import com.example.ui.components.ConsistPrimaryButton
import com.example.ui.theme.*
import com.example.ui.viewmodel.ConsistViewModel
import java.util.Locale

@Composable
fun VoiceLoggingDialog(
    viewModel: ConsistViewModel,
    onDismiss: () -> Unit
) {
    val voiceResult by viewModel.voiceParseResult.collectAsState()
    val isProcessing by viewModel.isVoiceProcessing.collectAsState()

    var spokenText by remember { mutableStateOf("") }
    var selectedMeal by remember { mutableStateOf("Breakfast") }
    var editableItems by remember { mutableStateOf<List<AiScannedFoodResult>>(emptyList()) }

    // Android Speech Recognizer contract
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                spokenText = spoken
                viewModel.processVoiceText(spoken)
            }
        }
    }

    LaunchedEffect(voiceResult) {
        voiceResult?.let {
            selectedMeal = it.mealType
            editableItems = it.items
        }
    }

    AlertDialog(
        onDismissRequest = {
            viewModel.dismissVoiceResult()
            onDismiss()
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Mic, contentDescription = null, tint = ConsistCyan)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Voice Food Logging", color = ConsistWhite, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 280.dp, max = 460.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (voiceResult == null) {
                    Text(
                        "Speak naturally, e.g.: \"I ate 2 idlis with one bowl of sambar for breakfast\"",
                        color = ConsistGrey,
                        fontSize = 13.sp
                    )

                    // Big Mic Button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .clip(CircleShape)
                                .background(ConsistCyan)
                                .clickable {
                                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                                        putExtra(RecognizerIntent.EXTRA_PROMPT, "Describe what you ate...")
                                    }
                                    speechLauncher.launch(intent)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = "Tap to Speak", tint = ConsistBlack, modifier = Modifier.size(38.dp))
                        }
                    }

                    Text("Or type your meal description:", color = ConsistGrey, fontSize = 12.sp)

                    OutlinedTextField(
                        value = spokenText,
                        onValueChange = { spokenText = it },
                        placeholder = { Text("e.g. 2 eggs and 1 cup oats") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ConsistCyan,
                            unfocusedBorderColor = ConsistBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (isProcessing) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            CircularProgressIndicator(color = ConsistCyan, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Parsing nutrition with AI...", color = ConsistCyan, fontSize = 13.sp)
                        }
                    } else if (spokenText.isNotBlank()) {
                        Button(
                            onClick = { viewModel.processVoiceText(spokenText) },
                            colors = ButtonDefaults.buttonColors(containerColor = ConsistCyan, contentColor = ConsistBlack),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Parse Meal", fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    // Result confirmation view
                    Text("Recognized Foods & Quantities:", color = ConsistCyan, fontWeight = FontWeight.Bold, fontSize = 14.sp)

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(editableItems) { idx, item ->
                            ConsistCard(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.foodName, color = ConsistWhite, fontWeight = FontWeight.Bold)
                                        Text("${item.calories.toInt()} kcal • P: ${item.protein.toInt()}g • C: ${item.carbs.toInt()}g • F: ${item.fat.toInt()}g", color = ConsistCyan, fontSize = 12.sp)
                                        Text("Portion: ${item.servingDescription} (${item.estimatedGrams.toInt()}g)", color = ConsistGrey, fontSize = 11.sp)
                                    }
                                    IconButton(onClick = {
                                        editableItems = editableItems.filterIndexed { i, _ -> i != idx }
                                    }) {
                                        Icon(Icons.Default.Close, contentDescription = "Remove", tint = ConsistError, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }

                    ConsistPrimaryButton(
                        text = "Confirm & Save to $selectedMeal",
                        onClick = {
                            viewModel.confirmVoiceLog(selectedMeal, editableItems)
                            onDismiss()
                        }
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = {
                viewModel.dismissVoiceResult()
                onDismiss()
            }) {
                Text("Cancel", color = ConsistGrey)
            }
        },
        containerColor = ConsistCharcoalDark
    )
}
