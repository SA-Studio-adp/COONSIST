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
import com.example.data.model.FoodItem
import com.example.data.model.Recipe
import com.example.data.model.RecipeIngredient
import com.example.ui.components.ConsistCard
import com.example.ui.components.ConsistPrimaryButton
import com.example.ui.theme.*
import com.example.ui.viewmodel.ConsistViewModel

@Composable
fun RecipeBuilderScreen(
    viewModel: ConsistViewModel,
    onBack: () -> Unit
) {
    val recipes by viewModel.allRecipes.collectAsState()
    val allFoods by viewModel.foodSearchResults.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ConsistBlack)
            .statusBarsPadding()
    ) {
        // Header
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
                        text = "RECIPE BUILDER",
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = ConsistWhite,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = "Custom & Kerala Recipe Formulator",
                        style = MaterialTheme.typography.labelSmall.copy(color = ConsistCyan)
                    )
                }
            }

            Button(
                onClick = { showCreateDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = ConsistCyan, contentColor = ConsistBlack),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("New Recipe", fontWeight = FontWeight.Bold)
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (recipes.isEmpty()) {
                item {
                    ConsistCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.RestaurantMenu, contentDescription = null, tint = ConsistCyan, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("No Custom Recipes Yet", color = ConsistWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Create authentic Kerala or fitness recipes by combining ingredients with exact weights. Nutrition is deterministically calculated per serving.",
                                color = ConsistGrey,
                                fontSize = 12.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(recipes) { recipe ->
                    ConsistCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = recipe.name,
                                    style = MaterialTheme.typography.titleMedium.copy(color = ConsistWhite, fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "${recipe.servings} serving(s) • ${recipe.cookingMethod}",
                                    style = MaterialTheme.typography.bodySmall.copy(color = ConsistCyan)
                                )
                            }
                            IconButton(onClick = { viewModel.deleteRecipe(recipe) }) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = ConsistError)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Per serving nutrition
                        Surface(
                            color = ConsistCharcoalElevated,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Per Serving", style = MaterialTheme.typography.labelSmall.copy(color = ConsistGrey))
                                    Text("${recipe.caloriesPerServing.toInt()} kcal", style = MaterialTheme.typography.bodyMedium.copy(color = ConsistCyan, fontWeight = FontWeight.Bold))
                                }
                                Column {
                                    Text("Protein", style = MaterialTheme.typography.labelSmall.copy(color = ConsistGrey))
                                    Text("${recipe.proteinPerServing.toInt()}g", style = MaterialTheme.typography.bodyMedium.copy(color = ConsistWhite, fontWeight = FontWeight.Bold))
                                }
                                Column {
                                    Text("Carbs", style = MaterialTheme.typography.labelSmall.copy(color = ConsistGrey))
                                    Text("${recipe.carbsPerServing.toInt()}g", style = MaterialTheme.typography.bodyMedium.copy(color = ConsistWhite, fontWeight = FontWeight.Bold))
                                }
                                Column {
                                    Text("Fat", style = MaterialTheme.typography.labelSmall.copy(color = ConsistGrey))
                                    Text("${recipe.fatPerServing.toInt()}g", style = MaterialTheme.typography.bodyMedium.copy(color = ConsistWhite, fontWeight = FontWeight.Bold))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Log to Diary Button
                        OutlinedButton(
                            onClick = {
                                viewModel.logRecipeToDiary(recipe, "Lunch")
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ConsistCyan),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.BookmarkAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Log 1 Serving to Today's Diary", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateRecipeDialog(
            allFoods = allFoods,
            onDismiss = { showCreateDialog = false },
            onSave = { name, servings, method, instr, isKerala, ingredients ->
                viewModel.saveRecipe(name, servings, method, instr, isKerala, ingredients)
                showCreateDialog = false
            }
        )
    }
}

@Composable
fun CreateRecipeDialog(
    allFoods: List<FoodItem>,
    onDismiss: () -> Unit,
    onSave: (String, Int, String, String, Boolean, List<RecipeIngredient>) -> Unit
) {
    var recipeName by remember { mutableStateOf("") }
    var servingsText by remember { mutableStateOf("2") }
    var cookingMethod by remember { mutableStateOf("Boiled / Simmered") }
    var instructions by remember { mutableStateOf("") }
    var isKeralaSpecial by remember { mutableStateOf(false) }

    val ingredients = remember { mutableStateListOf<RecipeIngredient>() }

    var selectedFoodForAdd by remember { mutableStateOf<FoodItem?>(allFoods.firstOrNull()) }
    var ingredientGramsText by remember { mutableStateOf("100") }

    val totalCals = ingredients.sumOf { it.calories.toDouble() }.toFloat()
    val totalPro = ingredients.sumOf { it.protein.toDouble() }.toFloat()
    val servings = servingsText.toIntOrNull() ?: 2
    val calPerServing = if (servings > 0) totalCals / servings else totalCals

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Build New Recipe", color = ConsistWhite, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 350.dp, max = 500.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = recipeName,
                    onValueChange = { recipeName = it },
                    label = { Text("Recipe Name (e.g. Kerala Sambar, Egg Curry)") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ConsistCyan, unfocusedBorderColor = ConsistBorder),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = servingsText,
                        onValueChange = { servingsText = it },
                        label = { Text("Servings") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ConsistCyan, unfocusedBorderColor = ConsistBorder),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = cookingMethod,
                        onValueChange = { cookingMethod = it },
                        label = { Text("Cooking Method") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ConsistCyan, unfocusedBorderColor = ConsistBorder),
                        modifier = Modifier.weight(1.5f)
                    )
                }

                Text("Add Ingredients:", color = ConsistCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)

                // Select food and gram amount
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = ingredientGramsText,
                        onValueChange = { ingredientGramsText = it },
                        label = { Text("Grams") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ConsistCyan, unfocusedBorderColor = ConsistBorder),
                        modifier = Modifier.width(90.dp)
                    )

                    Button(
                        onClick = {
                            selectedFoodForAdd?.let { f ->
                                val g = ingredientGramsText.toFloatOrNull() ?: 100f
                                val c = NutritionCalculator.calculateNutrient(f.caloriesPer100g, g)
                                val p = NutritionCalculator.calculateNutrient(f.proteinPer100g, g)
                                val carb = NutritionCalculator.calculateNutrient(f.carbsPer100g, g)
                                val fat = NutritionCalculator.calculateNutrient(f.fatPer100g, g)

                                ingredients.add(
                                    RecipeIngredient(
                                        recipeId = 0,
                                        foodName = f.name,
                                        weightGrams = g,
                                        calories = c,
                                        protein = p,
                                        carbs = carb,
                                        fat = fat
                                    )
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ConsistCyan, contentColor = ConsistBlack),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("+ Add Current Food", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Food selection dropdown / list
                LazyColumn(
                    modifier = Modifier.height(80.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(allFoods.take(12)) { food ->
                        val isSel = selectedFoodForAdd?.id == food.id
                        Surface(
                            color = if (isSel) ConsistCyanContainer else ConsistCharcoalCard,
                            shape = RoundedCornerShape(4.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) ConsistCyan else ConsistBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedFoodForAdd = food }
                        ) {
                            Text(
                                food.name,
                                color = if (isSel) ConsistCyan else ConsistWhite,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(6.dp)
                            )
                        }
                    }
                }

                // Added Ingredients List
                Text("Added Ingredients (${ingredients.size}):", color = ConsistGrey, fontSize = 11.sp)
                LazyColumn(
                    modifier = Modifier.height(70.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(ingredients) { ing ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${ing.foodName} (${ing.weightGrams.toInt()}g)", color = ConsistWhite, fontSize = 11.sp)
                            Text("${ing.calories.toInt()} kcal", color = ConsistCyan, fontSize = 11.sp)
                        }
                    }
                }

                // Live total preview
                Surface(
                    color = ConsistCharcoalElevated,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total: ${totalCals.toInt()} kcal", color = ConsistWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("Per Serving: ${calPerServing.toInt()} kcal", color = ConsistCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (recipeName.isNotBlank() && ingredients.isNotEmpty()) {
                        onSave(recipeName, servings, cookingMethod, instructions, isKeralaSpecial, ingredients.toList())
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = ConsistCyan, contentColor = ConsistBlack)
            ) {
                Text("Save Recipe", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = ConsistGrey) }
        },
        containerColor = ConsistCharcoalDark
    )
}
