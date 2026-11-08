package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.calculation.*
import com.example.data.model.FoodDiaryEntry
import com.example.data.model.FoodItem
import com.example.ui.theme.*
import com.example.ui.viewmodel.ConsistViewModel
import com.example.ui.viewmodel.CustomFoodUiState

private val fieldColors
    @Composable get() = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = ConsistCyan,
        unfocusedBorderColor = ConsistBorder,
        errorBorderColor = ConsistError
    )

/** Shows one-shot food messages (save success/failure, deletes) as toasts. Call once from FoodScreen. */
@Composable
fun FoodMessageToaster(viewModel: ConsistViewModel) {
    val message by viewModel.foodMessage.collectAsState()
    val context = LocalContext.current
    LaunchedEffect(message) {
        message?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.consumeFoodMessage()
        }
    }
}

// ------------------------------------------------------------------------------------------
// Universal quantity counter:  [ - ]  [ 1.5 ]  [ + ]
// Stateless on purpose: the caller owns the text, so it can disable "Add" while the text is invalid.
// ------------------------------------------------------------------------------------------
@Composable
fun QuantityCounter(
    text: String,
    option: ServingOption,
    onTextChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val parsed = ServingCalculator.parseQuantity(text, option)
    val current = (parsed as? QuantityParse.Valid)?.quantity
    val error = (parsed as? QuantityParse.Invalid)?.reason

    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledIconButton(
                onClick = { current?.let { onTextChange(ServingCalculator.formatQuantity(ServingCalculator.decrement(it, option))) } },
                enabled = current != null && ServingCalculator.decrement(current, option) != current,
                modifier = Modifier.size(48.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = ConsistCharcoalElevated, contentColor = ConsistCyan,
                    disabledContainerColor = ConsistCharcoalCard, disabledContentColor = ConsistGreyDark
                )
            ) { Icon(Icons.Default.Remove, contentDescription = "Decrease quantity") }

            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                singleLine = true,
                isError = error != null,
                textStyle = TextStyle(color = ConsistWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = fieldColors,
                modifier = Modifier.width(110.dp)
            )

            FilledIconButton(
                onClick = { onTextChange(ServingCalculator.formatQuantity(ServingCalculator.increment(current ?: 0f, option))) },
                modifier = Modifier.size(48.dp),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = ConsistCyan, contentColor = ConsistBlack)
            ) { Icon(Icons.Default.Add, contentDescription = "Increase quantity") }

            Text(option.label, color = ConsistGrey, fontSize = 13.sp, modifier = Modifier.weight(1f))
        }
        if (error != null) Text(error, color = ConsistError, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun SelectableChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (selected) ConsistCyan else ConsistCharcoalElevated,
        border = BorderStroke(1.dp, if (selected) ConsistCyan else ConsistBorder),
        modifier = Modifier.heightIn(min = 36.dp).clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
            Text(label, color = if (selected) ConsistBlack else ConsistWhite, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun MyFoodBadge() {
    Surface(shape = RoundedCornerShape(6.dp), color = ConsistCyanContainer) {
        Text("My Food", color = ConsistCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
    }
}

private fun fmt1(v: Float): String = (Math.round(v * 10f) / 10f).let { if (it == it.toInt().toFloat()) it.toInt().toString() else it.toString() }

@Composable
private fun NutritionPreview(food: FoodItem, n: CalculatedNutrition) {
    Surface(color = ConsistCharcoalElevated, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("${n.calories.toInt()} kcal", color = ConsistCyan, fontWeight = FontWeight.Bold, fontSize = 22.sp)
            Text("Protein ${fmt1(n.protein)} g  •  Carbs ${fmt1(n.carbs)} g  •  Fat ${fmt1(n.fat)} g", color = ConsistWhite, fontSize = 13.sp)
            val extras = buildList {
                if (n.fiber > 0f) add("Fibre ${fmt1(n.fiber)} g")
                if (n.sugar > 0f) add("Sugar ${fmt1(n.sugar)} g")
                if (n.sodiumMg > 0f) add("Sodium ${n.sodiumMg.toInt()} mg")
            }
            if (extras.isNotEmpty()) Text(extras.joinToString("  •  "), color = ConsistGrey, fontSize = 12.sp)
            Text(
                if (food.isVerified) "Verified data: ${food.source} (${food.state})" else "Not database-verified: ${if (food.isCustom) "values entered by you" else food.source}",
                color = if (food.isVerified) ConsistGrey else ConsistWarning, fontSize = 11.sp
            )
        }
    }
}

// ------------------------------------------------------------------------------------------
// Food picker: Search | My Foods | Recent | Favourites, then portion editor with the counter
// ------------------------------------------------------------------------------------------
@Composable
fun FoodPickerDialog(
    mealType: String,
    viewModel: ConsistViewModel,
    onDismiss: () -> Unit,
    onCreateCustomFood: () -> Unit,
    onEditCustomFood: (FoodItem) -> Unit
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.foodSearchResults.collectAsState()
    val customFoods by viewModel.customFoods.collectAsState()
    val recentFoods by viewModel.recentFoods.collectAsState()
    val favoriteFoods by viewModel.favoriteFoods.collectAsState()

    var tab by remember { mutableStateOf(0) }
    var selected by remember { mutableStateOf<FoodItem?>(null) }
    var pendingDelete by remember { mutableStateOf<FoodItem?>(null) }
    val tabs = listOf("Search", "My Foods", "Recent", "Favourites")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ConsistCharcoalDark,
        title = {
            Column {
                Text("Add Food to $mealType", color = ConsistWhite, fontWeight = FontWeight.Bold)
                Text("Nutrition is calculated from stored food data", color = ConsistCyan, fontSize = 12.sp)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().height(460.dp)) {
                val food = selected
                if (food != null) {
                    PortionEditor(
                        food = food,
                        mealType = mealType,
                        onBack = { selected = null },
                        onAdd = { qty, option ->
                            viewModel.logFood(food, mealType, qty, option)
                            selected = null
                            onDismiss()
                        }
                    )
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = 6.dp)) {
                        tabs.forEachIndexed { i, label -> SelectableChip(label, tab == i) { tab = i } }
                    }
                    if (tab == 0 || tab == 1) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            placeholder = { Text(if (tab == 1) "Search My Foods" else "Search food (Idli, Chicken, Oats...)") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = ConsistCyan) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = ConsistGrey)
                                }
                            },
                            singleLine = true,
                            colors = fieldColors,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    when (tab) {
                        0 -> FoodList(searchResults, emptyText = "No foods found. Tap \"New custom food\" to add it once and reuse it forever.", onPick = { selected = it })
                        1 -> {
                            val q = searchQuery.trim()
                            val mine = if (q.isEmpty()) customFoods else customFoods.filter { it.name.contains(q, ignoreCase = true) }
                            FoodList(
                                mine,
                                emptyText = if (customFoods.isEmpty()) "No custom foods yet. Tap \"New custom food\" - you only enter it once." else "No My Foods match \"$q\".",
                                onPick = { selected = it },
                                trailing = { f ->
                                    Row {
                                        IconButton(onClick = { viewModel.toggleFavorite(f) }) {
                                            Icon(if (f.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, contentDescription = if (f.isFavorite) "Remove favourite" else "Mark favourite", tint = if (f.isFavorite) ConsistCyan else ConsistGrey)
                                        }
                                        IconButton(onClick = { onEditCustomFood(f) }) { Icon(Icons.Default.Edit, contentDescription = "Edit ${f.name}", tint = ConsistGrey) }
                                        IconButton(onClick = { pendingDelete = f }) { Icon(Icons.Default.DeleteOutline, contentDescription = "Delete ${f.name}", tint = ConsistError) }
                                    }
                                }
                            )
                        }
                        2 -> FoodList(
                            recentFoods.map { it.food },
                            emptyText = "Foods you log will appear here so you can add them again in two taps.",
                            subtitleOverride = { f -> recentFoods.firstOrNull { it.food === f }?.lastEntry?.let { "Last: ${it.servingDescription} on ${it.date}" } },
                            onPick = { selected = it }
                        )
                        else -> FoodList(
                            favoriteFoods,
                            emptyText = "Mark foods as favourites with the heart in My Foods.",
                            onPick = { selected = it },
                            trailing = { f ->
                                IconButton(onClick = { viewModel.toggleFavorite(f) }) { Icon(Icons.Default.Favorite, contentDescription = "Remove favourite", tint = ConsistCyan) }
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (selected == null) TextButton(onClick = onCreateCustomFood) { Text("+ New custom food", color = ConsistCyan, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close", color = ConsistGrey) } }
    )

    pendingDelete?.let { f ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            containerColor = ConsistCharcoalDark,
            title = { Text("Delete \"${f.name}\"?", color = ConsistWhite, fontWeight = FontWeight.Bold) },
            text = { Text("This removes it from My Foods. Meals you already logged with it stay in your diary exactly as they were.", color = ConsistGrey) },
            confirmButton = {
                Button(onClick = { viewModel.deleteCustomFood(f); pendingDelete = null }, colors = ButtonDefaults.buttonColors(containerColor = ConsistError, contentColor = ConsistBlack)) { Text("Delete", fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancel", color = ConsistGrey) } }
        )
    }
}

@Composable
private fun ColumnScope.FoodList(
    foods: List<FoodItem>,
    emptyText: String,
    onPick: (FoodItem) -> Unit,
    trailing: (@Composable (FoodItem) -> Unit)? = null,
    subtitleOverride: ((FoodItem) -> String?)? = null
) {
    if (foods.isEmpty()) {
        Text(emptyText, color = ConsistGrey, fontSize = 13.sp, modifier = Modifier.padding(vertical = 16.dp))
        return
    }
    val unique = foods.distinctBy { it.stableId + "#" + it.id }   // LazyColumn keys must be unique
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().weight(1f)) {
        items(unique, key = { it.stableId + "#" + it.id }) { food ->
            val serving = ServingCalculator.servingOptionFor(food)
            val perServing = ServingCalculator.perServing(food)
            Surface(
                color = ConsistCharcoalCard, shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, ConsistBorder),
                modifier = Modifier.fillMaxWidth().clickable { onPick(food) }
            ) {
                Row(modifier = Modifier.padding(start = 10.dp, top = 8.dp, bottom = 8.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(food.name, color = ConsistWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f, fill = false))
                            if (food.isCustom) MyFoodBadge()
                        }
                        Text("${perServing.calories.toInt()} kcal per ${serving.label}  •  ${food.caloriesPer100g.toInt()} kcal/100g", color = ConsistGrey, fontSize = 12.sp)
                        Text(subtitleOverride?.invoke(food) ?: "${food.category} • ${food.state}", color = ConsistCyan, fontSize = 11.sp)
                    }
                    if (trailing != null) trailing(food) else Icon(Icons.Default.Add, contentDescription = "Select ${food.name}", tint = ConsistCyan, modifier = Modifier.padding(end = 8.dp))
                }
            }
        }
    }
}

@Composable
private fun PortionEditor(
    food: FoodItem,
    mealType: String,
    onBack: () -> Unit,
    onAdd: (Float, ServingOption) -> Unit
) {
    val options = remember(food.stableId, food.id) { ServingCalculator.optionsFor(food) }
    var optionKey by remember(food.stableId, food.id) { mutableStateOf(options.first().key) }
    val option = options.firstOrNull { it.key == optionKey } ?: options.first()
    var text by remember(food.stableId, food.id) { mutableStateOf("1") }   // default: one serving
    val parsed = ServingCalculator.parseQuantity(text, option)
    val quantity = (parsed as? QuantityParse.Valid)?.quantity
    val perServing = ServingCalculator.perServing(food)
    val serving = ServingCalculator.servingOptionFor(food)

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(food.name, color = ConsistCyan, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.weight(1f, fill = false))
            if (food.isCustom) MyFoodBadge()
        }
        Text("${perServing.calories.toInt()} kcal per ${serving.label} (${serving.gramsPerUnit.toInt()} g)  •  ${food.caloriesPer100g.toInt()} kcal per 100 g", color = ConsistGrey, fontSize = 13.sp)

        if (options.size > 1) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                options.forEach { o ->
                    SelectableChip(o.label, o.key == optionKey) {
                        // switching unit keeps the same weight, so totals don't jump
                        if (quantity != null && o.key != optionKey) text = ServingCalculator.formatQuantity(ServingCalculator.convertQuantity(quantity, option, o))
                        else if (quantity == null) text = "1"
                        optionKey = o.key
                    }
                }
            }
        }

        QuantityCounter(text = text, option = option, onTextChange = { text = it })

        if (quantity != null) {
            NutritionPreview(food, ServingCalculator.nutritionFor(food, quantity, option))
            Text("= ${ServingCalculator.gramsFor(quantity, option).toInt()} g total", color = ConsistGrey, fontSize = 12.sp)
        }

        Button(
            onClick = { if (quantity != null) onAdd(quantity, option) },
            enabled = quantity != null,
            colors = ButtonDefaults.buttonColors(containerColor = ConsistCyan, contentColor = ConsistBlack, disabledContainerColor = ConsistCharcoalElevated, disabledContentColor = ConsistGreyDark),
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
        ) { Text("Add to Food Diary ($mealType)", fontWeight = FontWeight.Bold) }

        TextButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Back", color = ConsistGrey) }
    }
}

// ------------------------------------------------------------------------------------------
// Create / edit a custom food. "Save" and "Save & add" are separate operations.
// ------------------------------------------------------------------------------------------
@Composable
fun CustomFoodEditorDialog(
    existing: FoodItem?,
    mealType: String,
    viewModel: ConsistViewModel,
    onDismiss: () -> Unit
) {
    val ui by viewModel.customFoodUi.collectAsState()
    val initial = remember(existing?.stableId) { existing?.let { CustomFoodRules.toDraft(it) } }

    // 2-decimal formatting: editing a food must never silently change its stored values
    fun n(v: Float?): String = if (v == null || v == 0f) "" else ServingCalculator.formatQuantity(v)
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var servingQty by remember { mutableStateOf(initial?.let { ServingCalculator.formatQuantity(it.servingQuantity) } ?: "1") }
    var unit by remember { mutableStateOf(initial?.servingUnit ?: "piece") }
    var weight by remember { mutableStateOf(initial?.let { ServingCalculator.formatQuantity(it.servingWeightGrams) } ?: "") }
    var calories by remember { mutableStateOf(n(initial?.calories)) }
    var protein by remember { mutableStateOf(n(initial?.protein)) }
    var carbs by remember { mutableStateOf(n(initial?.carbs)) }
    var fat by remember { mutableStateOf(n(initial?.fat)) }
    var fiber by remember { mutableStateOf(n(initial?.fiber)) }
    var sugar by remember { mutableStateOf(n(initial?.sugar)) }
    var sodium by remember { mutableStateOf(n(initial?.sodiumMg)) }
    var localErrors by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    // Leave the dialog only after the ViewModel confirms a verified save; always start from a clean state.
    DisposableEffect(Unit) { onDispose { viewModel.resetCustomFoodUi() } }
    LaunchedEffect(ui) {
        if (ui is CustomFoodUiState.Saved) { viewModel.resetCustomFoodUi(); onDismiss() }
    }

    val saving = ui is CustomFoodUiState.Saving
    val remoteErrors = (ui as? CustomFoodUiState.Error)?.fieldErrors.orEmpty()
    val errors = remoteErrors + localErrors
    val baseUnit = unit == "g" || unit == "ml"

    fun num(s: String): Float? = if (s.isBlank()) 0f else s.trim().replace(',', '.').toFloatOrNull()?.takeIf { it.isFinite() }

    /** Returns a draft, or records field errors and returns null. */
    fun buildDraft(): CustomFoodDraft? {
        val e = linkedMapOf<String, String>()
        val q = servingQty.trim().replace(',', '.').toFloatOrNull()
        if (q == null) e["serving"] = "Enter the serving size"
        val w = if (baseUnit) q else weight.trim().replace(',', '.').toFloatOrNull()
        if (w == null) e["weight"] = "Enter the weight of one serving in grams"
        if (calories.isBlank() || num(calories) == null) e["calories"] = "Enter calories"
        listOf("protein" to protein, "carbs" to carbs, "fat" to fat, "fiber" to fiber, "sugar" to sugar, "sodium" to sodium)
            .forEach { (k, v) -> if (num(v) == null) e[k] = "Enter a number" }
        if (e.isNotEmpty()) { localErrors = e; return null }
        localErrors = emptyMap()
        return CustomFoodDraft(
            name = name, servingQuantity = q!!, servingUnit = unit, servingWeightGrams = w!!,
            calories = num(calories)!!, protein = num(protein)!!, carbs = num(carbs)!!, fat = num(fat)!!,
            fiber = num(fiber)!!, sugar = num(sugar)!!, sodiumMg = num(sodium)!!
        )
    }

    @Composable
    fun field(label: String, value: String, key: String, onChange: (String) -> Unit, decimal: Boolean = true, modifier: Modifier = Modifier) {
        OutlinedTextField(
            value = value, onValueChange = { onChange(it); localErrors = localErrors - key },
            label = { Text(label) }, singleLine = true, isError = errors.containsKey(key),
            supportingText = if (errors.containsKey(key)) { { Text(errors[key] ?: "", color = ConsistError) } } else null,
            keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Text),
            colors = fieldColors, modifier = modifier
        )
    }

    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        containerColor = ConsistCharcoalDark,
        title = { Text(if (existing == null) "New custom food" else "Edit custom food", color = ConsistWhite, fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                (ui as? CustomFoodUiState.Error)?.let {
                    Surface(color = ConsistErrorTint, shape = RoundedCornerShape(8.dp)) { Text(it.message, color = ConsistError, fontSize = 13.sp, modifier = Modifier.padding(10.dp)) }
                }
                field("Food name", name, "name", { name = it }, decimal = false, modifier = Modifier.fillMaxWidth())
                Text("One serving is...", color = ConsistGrey, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                    listOf("piece", "g", "ml", "cup", "bowl", "tbsp", "slice").forEach { u -> SelectableChip(u, unit == u) { unit = u; localErrors = localErrors - "serving" - "weight" } }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    field(if (baseUnit) "Serving size ($unit)" else "How many $unit", servingQty, "serving", { servingQty = it }, modifier = Modifier.weight(1f))
                    if (!baseUnit) field("Weight of it (g)", weight, "weight", { weight = it }, modifier = Modifier.weight(1f))
                }
                Text("Nutrition for that one serving", color = ConsistGrey, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    field("Calories", calories, "calories", { calories = it }, modifier = Modifier.weight(1f))
                    field("Protein (g)", protein, "protein", { protein = it }, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    field("Carbs (g)", carbs, "carbs", { carbs = it }, modifier = Modifier.weight(1f))
                    field("Fat (g)", fat, "fat", { fat = it }, modifier = Modifier.weight(1f))
                }
                Text("Optional", color = ConsistGrey, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    field("Fibre (g)", fiber, "fiber", { fiber = it }, modifier = Modifier.weight(1f))
                    field("Sugar (g)", sugar, "sugar", { sugar = it }, modifier = Modifier.weight(1f))
                }
                field("Sodium (mg)", sodium, "sodium", { sodium = it }, modifier = Modifier.fillMaxWidth())
                if (baseUnit && errors.containsKey("weight")) Text(errors["weight"]!!, color = ConsistError, fontSize = 12.sp)
            }
        },
        confirmButton = {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (existing == null) {
                    Button(
                        onClick = { buildDraft()?.let { viewModel.saveCustomFood(it, logToMeal = mealType) } },
                        enabled = !saving,
                        colors = ButtonDefaults.buttonColors(containerColor = ConsistCyan, contentColor = ConsistBlack),
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) { Text(if (saving) "Saving..." else "Save & add to $mealType", fontWeight = FontWeight.Bold) }
                    OutlinedButton(
                        onClick = { buildDraft()?.let { viewModel.saveCustomFood(it, logToMeal = null) } },
                        enabled = !saving,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ConsistCyan),
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) { Text("Save to My Foods only", fontWeight = FontWeight.Bold) }
                } else {
                    Button(
                        onClick = { buildDraft()?.let { viewModel.updateCustomFood(existing, it) } },
                        enabled = !saving,
                        colors = ButtonDefaults.buttonColors(containerColor = ConsistCyan, contentColor = ConsistBlack),
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) { Text(if (saving) "Saving..." else "Save changes", fontWeight = FontWeight.Bold) }
                }
            }
        },
        dismissButton = { TextButton(onClick = { if (!saving) onDismiss() }) { Text("Cancel", color = ConsistGrey) } }
    )
}

private val ConsistErrorTint = androidx.compose.ui.graphics.Color(0x33FF5252)

// ------------------------------------------------------------------------------------------
// Edit an existing diary entry's quantity (in the unit it was logged in), with live totals
// ------------------------------------------------------------------------------------------
@Composable
fun EditDiaryEntryDialog(
    entry: FoodDiaryEntry,
    onDismiss: () -> Unit,
    onSave: (Float) -> Unit
) {
    val option = remember(entry.id) { ServingCalculator.optionForEntry(entry) }
    var text by remember(entry.id) { mutableStateOf(ServingCalculator.formatQuantity(entry.quantity)) }
    val parsed = ServingCalculator.parseQuantity(text, option)
    val quantity = (parsed as? QuantityParse.Valid)?.quantity
    val preview = quantity?.let { ServingCalculator.rescaleEntry(entry, it) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ConsistCharcoalDark,
        title = { Text("Edit ${entry.foodName}", color = ConsistWhite, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                QuantityCounter(text = text, option = option, onTextChange = { text = it })
                if (preview != null) {
                    Surface(color = ConsistCharcoalElevated, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("${preview.calories.toInt()} kcal", color = ConsistCyan, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            Text("Protein ${fmt1(preview.protein)} g  •  Carbs ${fmt1(preview.carbs)} g  •  Fat ${fmt1(preview.fat)} g", color = ConsistWhite, fontSize = 13.sp)
                            if (preview.fiber > 0f) Text("Fibre ${fmt1(preview.fiber)} g", color = ConsistGrey, fontSize = 12.sp)
                            Text("${preview.weightGrams.toInt()} g total", color = ConsistGrey, fontSize = 12.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (quantity != null) { onSave(quantity); onDismiss() } },
                enabled = quantity != null,
                colors = ButtonDefaults.buttonColors(containerColor = ConsistCyan, contentColor = ConsistBlack, disabledContainerColor = ConsistCharcoalElevated, disabledContentColor = ConsistGreyDark),
                modifier = Modifier.heightIn(min = 48.dp)
            ) { Text("Save & Recalculate", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = ConsistGrey) } }
    )
}
