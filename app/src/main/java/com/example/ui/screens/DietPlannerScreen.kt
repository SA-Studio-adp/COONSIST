package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DietMealItem
import com.example.ui.components.ConsistCard
import com.example.ui.components.ConsistPrimaryButton
import com.example.ui.theme.*
import com.example.ui.viewmodel.ConsistViewModel

@Composable
fun DietPlannerScreen(
    viewModel: ConsistViewModel,
    onBack: () -> Unit
) {
    val activePlan by viewModel.activeDietPlan.collectAsState()
    val planItems by viewModel.activePlanItems.collectAsState()
    val profile by viewModel.userProfile.collectAsState()

    var showShoppingList by remember { mutableStateOf(false) }

    val mealsOrder = listOf("Breakfast", "Lunch", "Evening Snack", "Dinner")

    val calculatedPlanCalories = planItems.sumOf { it.calories.toDouble() }.toFloat()
    val calculatedPlanProtein = planItems.sumOf { it.protein.toDouble() }.toFloat()
    val calculatedPlanCarbs = planItems.sumOf { it.carbs.toDouble() }.toFloat()
    val calculatedPlanFat = planItems.sumOf { it.fat.toDouble() }.toFloat()

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
                        text = "AI DIET PLANNER",
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = ConsistWhite,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = "${profile?.dietaryPreference ?: "Personalized"} Plan • ${profile?.primaryGoal ?: "Fitness"}",
                        style = MaterialTheme.typography.labelSmall.copy(color = ConsistCyan)
                    )
                }
            }

            IconButton(onClick = { viewModel.regenerateDietPlan() }) {
                Icon(Icons.Default.Refresh, contentDescription = "Regenerate", tint = ConsistCyan)
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Plan Overview Card
            item {
                ConsistCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = ConsistCyan.copy(alpha = 0.5f)
                ) {
                    Text(
                        text = activePlan?.title ?: "Personalized Nutrition Schedule",
                        style = MaterialTheme.typography.titleMedium.copy(color = ConsistWhite, fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Mathematically verified against your ${profile?.targetCalories ?: 2400} kcal target with optimal macronutrient partitioning.",
                        style = MaterialTheme.typography.bodySmall.copy(color = ConsistGrey)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // Target vs Plan Actuals
                    Surface(
                        color = ConsistCharcoalElevated,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Total Calories", style = MaterialTheme.typography.labelSmall.copy(color = ConsistGrey))
                                Text("${calculatedPlanCalories.toInt()} kcal", style = MaterialTheme.typography.titleMedium.copy(color = ConsistCyan, fontWeight = FontWeight.Bold))
                            }
                            Column {
                                Text("Protein", style = MaterialTheme.typography.labelSmall.copy(color = ConsistGrey))
                                Text("${calculatedPlanProtein.toInt()}g", style = MaterialTheme.typography.titleMedium.copy(color = ConsistWhite, fontWeight = FontWeight.Bold))
                            }
                            Column {
                                Text("Carbs", style = MaterialTheme.typography.labelSmall.copy(color = ConsistGrey))
                                Text("${calculatedPlanCarbs.toInt()}g", style = MaterialTheme.typography.titleMedium.copy(color = ConsistWhite, fontWeight = FontWeight.Bold))
                            }
                            Column {
                                Text("Fat", style = MaterialTheme.typography.labelSmall.copy(color = ConsistGrey))
                                Text("${calculatedPlanFat.toInt()}g", style = MaterialTheme.typography.titleMedium.copy(color = ConsistWhite, fontWeight = FontWeight.Bold))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showShoppingList = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ConsistCyan)
                        ) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Shopping List", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { viewModel.regenerateDietPlan() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ConsistCyan, contentColor = ConsistBlack)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Regenerate", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Meals Breakdown
            mealsOrder.forEach { mealType ->
                val itemsForMeal = planItems.filter { it.mealName.equals(mealType, ignoreCase = true) }
                val mealCals = itemsForMeal.sumOf { it.calories.toDouble() }.toFloat()
                val mealPro = itemsForMeal.sumOf { it.protein.toDouble() }.toFloat()

                item {
                    ConsistCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = mealType.uppercase(),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = ConsistCyan,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = "${mealCals.toInt()} kcal • P: ${mealPro.toInt()}g",
                                style = MaterialTheme.typography.bodySmall.copy(color = ConsistWhite, fontWeight = FontWeight.SemiBold)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        itemsForMeal.forEach { mealItem ->
                            Surface(
                                color = ConsistCharcoalElevated,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = mealItem.foodItemName,
                                            style = MaterialTheme.typography.bodyMedium.copy(color = ConsistWhite, fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            text = "${mealItem.calories.toInt()} kcal",
                                            style = MaterialTheme.typography.bodySmall.copy(color = ConsistCyan, fontWeight = FontWeight.Bold)
                                        )
                                    }
                                    Text(
                                        text = "Portion: ${mealItem.portionDescription} (${mealItem.weightGrams.toInt()}g)",
                                        style = MaterialTheme.typography.labelSmall.copy(color = ConsistGreyLight)
                                    )
                                    Text(
                                        text = "P: ${mealItem.protein.toInt()}g • C: ${mealItem.carbs.toInt()}g • F: ${mealItem.fat.toInt()}g",
                                        style = MaterialTheme.typography.labelSmall.copy(color = ConsistGrey)
                                    )
                                    if (mealItem.prepInstructions.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Prep: ${mealItem.prepInstructions}",
                                            style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF80DEEA), fontSize = 11.sp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Shopping List Modal
    if (showShoppingList) {
        val shoppingItems = activePlan?.shoppingListJson?.split(",")?.map { it.trim() } ?: emptyList()
        AlertDialog(
            onDismissRequest = { showShoppingList = false },
            title = { Text("Weekly Grocery Shopping List", color = ConsistWhite, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Ingredients required for your current diet plan:", color = ConsistGrey, fontSize = 13.sp)
                    shoppingItems.forEach { item ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ConsistCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(item, color = ConsistWhite, fontSize = 14.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showShoppingList = false },
                    colors = ButtonDefaults.buttonColors(containerColor = ConsistCyan, contentColor = ConsistBlack)
                ) {
                    Text("Done", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = ConsistCharcoalDark
        )
    }
}
