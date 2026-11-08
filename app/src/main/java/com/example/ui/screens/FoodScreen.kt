package com.example.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.FoodDiaryEntry
import com.example.data.model.FoodItem
import com.example.ui.components.ConsistCard
import com.example.ui.components.ConsistPrimaryButton
import com.example.ui.components.MetricBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.ConsistViewModel

@Composable
fun FoodScreen(
    viewModel: ConsistViewModel,
    onNavigateToAiScan: () -> Unit,
    onOpenVoiceLog: () -> Unit,
    onNavigateToRecipeBuilder: () -> Unit,
    onNavigateToDietPlan: () -> Unit
) {
    val selectedDate by viewModel.selectedDate.collectAsState()
    val diaryEntries by viewModel.currentDayEntries.collectAsState()
    val dailySummary by viewModel.dailySummary.collectAsState()
    val profile by viewModel.userProfile.collectAsState()
    val searchResults by viewModel.foodSearchResults.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var showSearchDialog by remember { mutableStateOf(false) }
    var selectedMealForAdd by remember { mutableStateOf("Breakfast") }
    var showCustomFoodDialog by remember { mutableStateOf(false) }

    var customFoodToEdit by remember { mutableStateOf<FoodItem?>(null) }

    // Diary entry being edited (quantity counter dialog)
    var entryToEdit by remember { mutableStateOf<FoodDiaryEntry?>(null) }

    FoodMessageToaster(viewModel)

    val meals = listOf("Breakfast", "Morning Snack", "Lunch", "Evening Snack", "Dinner", "Other")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ConsistBlack)
            .statusBarsPadding()
    ) {
        // Date Selector Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.stepDate(-1) }) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Day", tint = ConsistCyan)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "FOOD DIARY",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = ConsistCyan,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                )
                Text(
                    text = selectedDate,
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = ConsistWhite,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            IconButton(onClick = { viewModel.stepDate(1) }) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Next Day", tint = ConsistCyan)
            }
        }

        // Daily Macro Bar
        Surface(
            color = ConsistCharcoalDark,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, ConsistBorder)
        ) {
            Row(
                modifier = Modifier.padding(vertical = 10.dp, horizontal = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Calories", style = MaterialTheme.typography.labelSmall.copy(color = ConsistGrey))
                    Text("${dailySummary.calories.toInt()} / ${profile?.targetCalories ?: 2400}", style = MaterialTheme.typography.bodyMedium.copy(color = ConsistCyan, fontWeight = FontWeight.Bold))
                }
                Column {
                    Text("Protein", style = MaterialTheme.typography.labelSmall.copy(color = ConsistGrey))
                    Text("${dailySummary.protein.toInt()}g", style = MaterialTheme.typography.bodyMedium.copy(color = ConsistWhite, fontWeight = FontWeight.Bold))
                }
                Column {
                    Text("Carbs", style = MaterialTheme.typography.labelSmall.copy(color = ConsistGrey))
                    Text("${dailySummary.carbs.toInt()}g", style = MaterialTheme.typography.bodyMedium.copy(color = ConsistWhite, fontWeight = FontWeight.Bold))
                }
                Column {
                    Text("Fat", style = MaterialTheme.typography.labelSmall.copy(color = ConsistGrey))
                    Text("${dailySummary.fat.toInt()}g", style = MaterialTheme.typography.bodyMedium.copy(color = ConsistWhite, fontWeight = FontWeight.Bold))
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Action Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onNavigateToAiScan,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ConsistCyan)
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("AI Scan", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = onOpenVoiceLog,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ConsistCyan)
            ) {
                Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Voice", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = onNavigateToRecipeBuilder,
                modifier = Modifier.weight(1.1f),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ConsistCyan)
            ) {
                Icon(Icons.Default.Restaurant, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Recipes", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Meal Sections List
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(meals) { meal ->
                val mealEntries = diaryEntries.filter { it.mealType.equals(meal, ignoreCase = true) }
                val mealCalories = mealEntries.sumOf { it.calories.toDouble() }.toFloat()
                val mealProtein = mealEntries.sumOf { it.protein.toDouble() }.toFloat()
                val mealCarbs = mealEntries.sumOf { it.carbs.toDouble() }.toFloat()
                val mealFat = mealEntries.sumOf { it.fat.toDouble() }.toFloat()

                ConsistCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Meal Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = meal.uppercase(),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = ConsistWhite,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            if (mealEntries.isNotEmpty()) {
                                Text(
                                    text = "${mealCalories.toInt()} kcal • P: ${mealProtein.toInt()}g • C: ${mealCarbs.toInt()}g • F: ${mealFat.toInt()}g",
                                    style = MaterialTheme.typography.bodySmall.copy(color = ConsistGrey)
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                selectedMealForAdd = meal
                                showSearchDialog = true
                            }
                        ) {
                            Icon(
                                Icons.Default.AddCircle,
                                contentDescription = "Add Food to $meal",
                                tint = ConsistCyan,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    if (mealEntries.isEmpty()) {
                        Text(
                            text = "No food logged for $meal",
                            style = MaterialTheme.typography.bodySmall.copy(color = ConsistGreyDark),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        Spacer(modifier = Modifier.height(8.dp))
                        mealEntries.forEach { entry ->
                            HorizontalDivider(color = ConsistBorder, thickness = 0.5.dp)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = entry.foodName,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = ConsistWhite,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    )
                                    Text(
                                        text = "${entry.servingDescription} (${entry.weightGrams.toInt()}g) • ${entry.calories.toInt()} kcal",
                                        style = MaterialTheme.typography.bodySmall.copy(color = ConsistCyan)
                                    )
                                    Text(
                                        text = "P: ${entry.protein.toInt()}g • C: ${entry.carbs.toInt()}g • F: ${entry.fat.toInt()}g",
                                        style = MaterialTheme.typography.labelSmall.copy(color = ConsistGrey)
                                    )
                                    if (entry.isAiEstimated) {
                                        Text(
                                            text = "🤖 ${entry.confidenceNote}",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = Color(0xFF80DEEA),
                                                fontSize = 10.sp
                                            )
                                        )
                                    }
                                }

                                Row {
                                    IconButton(
                                        onClick = {
                                            entryToEdit = entry
                                        }
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit quantity", tint = ConsistGrey, modifier = Modifier.size(18.dp))
                                    }
                                    IconButton(
                                        onClick = { viewModel.deleteDiaryEntry(entry) }
                                    ) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete Food", tint = ConsistError, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Edit diary entry quantity (+/- counter, live totals)
    entryToEdit?.let { entry ->
        EditDiaryEntryDialog(
            entry = entry,
            onDismiss = { entryToEdit = null },
            onSave = { quantity -> viewModel.updateDiaryEntryQuantity(entry, quantity) }
        )
    }

    // Food picker: Search | My Foods | Recent | Favourites, then quantity counter
    if (showSearchDialog) {
        FoodPickerDialog(
            mealType = selectedMealForAdd,
            viewModel = viewModel,
            onDismiss = {
                showSearchDialog = false
                viewModel.setSearchQuery("")
            },
            onCreateCustomFood = {
                customFoodToEdit = null
                showCustomFoodDialog = true
            },
            onEditCustomFood = { food ->
                customFoodToEdit = food
                showCustomFoodDialog = true
            }
        )
    }

    // Create / edit custom food. Saving is verified in Room before the dialog closes.
    if (showCustomFoodDialog) {
        CustomFoodEditorDialog(
            existing = customFoodToEdit,
            mealType = selectedMealForAdd,
            viewModel = viewModel,
            onDismiss = {
                showCustomFoodDialog = false
                customFoodToEdit = null
            }
        )
    }
}
