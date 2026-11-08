package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.ConsistCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.ConsistViewModel

@Composable
fun AiCoachScreen(
    viewModel: ConsistViewModel
) {
    val messages by viewModel.coachMessages.collectAsState()
    val isThinking by viewModel.isCoachThinking.collectAsState()
    val profile by viewModel.userProfile.collectAsState()
    val dailySummary by viewModel.dailySummary.collectAsState()

    var textInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    val quickPrompts = listOf(
        "How are my macros today?",
        "High-protein Kerala meal suggestions",
        "Explain my target calories",
        "Best post-workout meal for muscle gain",
        "Healthy food swaps for dinner"
    )

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ConsistBlack)
            .statusBarsPadding()
    ) {
        // Coach Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(ConsistCyanContainer)
                    .border(1.dp, ConsistCyan, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Psychology, contentDescription = null, tint = ConsistCyan)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "CONSIST AI COACH",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = ConsistWhite,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                )
                Text(
                    text = "Evidence-based fitness & nutrition",
                    style = MaterialTheme.typography.bodySmall.copy(color = ConsistCyan)
                )
            }
        }

        // Live Context Strip
        Surface(
            color = ConsistCharcoalDark,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            shape = RoundedCornerShape(10.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, ConsistBorder)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Goal: ${profile?.primaryGoal ?: "Muscle gain"}",
                    style = MaterialTheme.typography.labelSmall.copy(color = ConsistGreyLight)
                )
                Text(
                    text = "Today: ${dailySummary.calories.toInt()}/${profile?.targetCalories ?: 2400} kcal",
                    style = MaterialTheme.typography.labelSmall.copy(color = ConsistCyan, fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Protein: ${dailySummary.protein.toInt()}/${profile?.targetProteinGrams?.toInt() ?: 140}g",
                    style = MaterialTheme.typography.labelSmall.copy(color = ConsistWhite)
                )
            }
        }

        // Messages List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(messages) { msg ->
                val isUser = msg.sender == "USER"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                ) {
                    Surface(
                        color = if (isUser) ConsistCyanContainer else ConsistCharcoalCard,
                        shape = RoundedCornerShape(
                            topStart = 14.dp,
                            topEnd = 14.dp,
                            bottomStart = if (isUser) 14.dp else 2.dp,
                            bottomEnd = if (isUser) 2.dp else 14.dp
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isUser) ConsistCyan.copy(alpha = 0.5f) else ConsistBorder
                        ),
                        modifier = Modifier.widthIn(max = 300.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = if (isUser) "YOU" else "CONSIST COACH",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (isUser) ConsistCyan else ConsistGrey,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = msg.text,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = ConsistWhite,
                                    lineHeight = 20.sp
                                )
                            )
                        }
                    }
                }
            }

            if (isThinking) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Surface(
                            color = ConsistCharcoalCard,
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ConsistBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(color = ConsistCyan, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("CONSIST is formulating advice...", color = ConsistGrey, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // Quick Suggestion Chips
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(quickPrompts) { prompt ->
                Surface(
                    color = ConsistCharcoalCard,
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ConsistBorder),
                    modifier = Modifier.clickable {
                        viewModel.sendCoachMessage(prompt)
                    }
                ) {
                    Text(
                        text = prompt,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = ConsistCyan,
                            fontWeight = FontWeight.Medium
                        ),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Input Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .padding(bottom = 70.dp), // account for bottom bar
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = textInput,
                onValueChange = { textInput = it },
                placeholder = { Text("Ask about macros, meals, workouts...") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ConsistCyan,
                    unfocusedBorderColor = ConsistBorder,
                    cursorColor = ConsistCyan
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = {
                    if (textInput.isNotBlank()) {
                        val q = textInput
                        textInput = ""
                        viewModel.sendCoachMessage(q)
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(ConsistCyan)
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send", tint = ConsistBlack)
            }
        }
    }
}
