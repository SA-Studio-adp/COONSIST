package com.example.data.calculation

import com.example.data.model.FoodDiaryEntry
import com.example.data.model.FoodItem
import kotlin.math.round

/**
 * One selectable unit for a food, e.g. "medium dosa" (75 g each) or "g" (1 g each).
 * Nutrition is always derived from [gramsPerUnit] and the food's per-100 g values - never from AI.
 */
data class ServingOption(
    val key: String,
    val label: String,
    val gramsPerUnit: Float,
    /** Amount the + / - buttons change the quantity by. */
    val step: Float,
    /** Smallest value the - button will go down to (typed values may be lower but must be > 0). */
    val minQuantity: Float,
    val maxQuantity: Float
)

sealed interface QuantityParse {
    data class Valid(val quantity: Float) : QuantityParse
    data class Invalid(val reason: String) : QuantityParse
}

object ServingCalculator {

    const val MAX_GRAMS = 5000f
    private const val GRAM_STEP = 10f
    private const val UNIT_STEP = 0.5f
    private const val MIN_TYPED_UNITS = 0.1f
    private const val MIN_TYPED_GRAMS = 1f
    private const val MAX_UNITS = 100f

    private val servingLabelRegex =
        Regex("""^\s*(\d+(?:\.\d+)?(?:/\d+)?)\s+([^()]+?)\s*(?:\(.*\))?\s*$""")

    /** "1/2" -> 0.5, "2" -> 2.0, "1.5" -> 1.5. Null if not parseable or zero. */
    private fun parseCount(token: String): Float? {
        val v = if (token.contains('/')) {
            val (a, b) = token.split('/', limit = 2)
            val n = a.toFloatOrNull() ?: return null
            val d = b.toFloatOrNull() ?: return null
            if (d == 0f) return null
            n / d
        } else token.toFloatOrNull()
        return if (v != null && v > 0f && v.isFinite()) v else null
    }

    /**
     * The food's own serving, derived from its label and stored serving weight:
     *  "1 medium dosa (75g)" -> unit "medium dosa", 75 g each
     *  "1/2 cup (40g)"       -> unit "cup", 80 g each
     *  "2 tbsp (32g)"        -> unit "tbsp", 16 g each
     *  "100g cube"           -> unit "serving", defaultWeightGrams each
     */
    fun servingOptionFor(food: FoodItem): ServingOption {
        val totalGrams = if (food.defaultWeightGrams > 0f) food.defaultWeightGrams else 100f
        val match = servingLabelRegex.matchEntire(food.defaultServingUnit)
        val count = match?.groupValues?.get(1)?.let { parseCount(it) }
        val noun = match?.groupValues?.get(2)?.trim()?.takeIf { it.isNotEmpty() }
        return if (count != null && noun != null) {
            ServingOption("serving", noun, totalGrams / count, UNIT_STEP, UNIT_STEP, MAX_UNITS)
        } else {
            ServingOption("serving", "serving", totalGrams, UNIT_STEP, UNIT_STEP, MAX_UNITS)
        }
    }

    /** Base measured unit: grams, or millilitres (1 ml = 1 g) for foods defined in ml. */
    fun baseOption(food: FoodItem): ServingOption {
        val unit = if (food.servingUnitType.equals("ml", ignoreCase = true)) "ml" else "g"
        return ServingOption(unit, unit, 1f, GRAM_STEP, GRAM_STEP, MAX_GRAMS)
    }

    fun optionsFor(food: FoodItem): List<ServingOption> {
        val serving = servingOptionFor(food)
        val base = baseOption(food)
        // If the food itself is just "100 g" style, avoid showing a duplicate-looking unit.
        return if (serving.gramsPerUnit == 1f) listOf(base) else listOf(serving, base)
    }

    fun defaultOption(food: FoodItem): ServingOption = optionsFor(food).first()

    /** The unit an existing diary entry was logged in, so the edit dialog can offer the same counter. */
    fun optionForEntry(e: FoodDiaryEntry): ServingOption {
        val isBase = e.servingUnit == "g" || e.servingUnit == "ml"
        return if (isBase) {
            ServingOption(e.servingUnit, e.servingUnit, 1f, GRAM_STEP, GRAM_STEP, MAX_GRAMS)
        } else {
            val per = if (e.servingGramsPerUnit > 0f) e.servingGramsPerUnit else 1f
            ServingOption("serving", e.servingUnit, per, UNIT_STEP, UNIT_STEP, MAX_UNITS)
        }
    }

    /** Rounds away float noise (0.30000001 -> 0.3). */
    fun tidy(q: Float): Float = round(q * 100f) / 100f

    fun gramsFor(quantity: Float, option: ServingOption): Float = quantity * option.gramsPerUnit

    fun increment(quantity: Float, option: ServingOption): Float =
        tidy((quantity + option.step).coerceAtMost(option.maxQuantity))

    /** Never produces zero or a negative: if the step would go below the minimum, quantity is unchanged. */
    fun decrement(quantity: Float, option: ServingOption): Float {
        val next = tidy(quantity - option.step)
        val floor = if (option.key == "serving") MIN_TYPED_UNITS else MIN_TYPED_GRAMS
        return if (next >= floor) next else quantity
    }

    /** Validates direct numeric entry. Accepts "1,5" as 1.5. */
    fun parseQuantity(text: String, option: ServingOption): QuantityParse {
        val v = text.trim().replace(',', '.').toFloatOrNull()
            ?: return QuantityParse.Invalid("Enter a number")
        if (!v.isFinite()) return QuantityParse.Invalid("Enter a valid number")
        val min = if (option.key == "serving") MIN_TYPED_UNITS else MIN_TYPED_GRAMS
        if (v <= 0f) return QuantityParse.Invalid("Quantity must be greater than 0")
        if (v < min) return QuantityParse.Invalid("Minimum is ${formatQuantity(min)}")
        if (v > option.maxQuantity) return QuantityParse.Invalid("Maximum is ${formatQuantity(option.maxQuantity)}")
        return QuantityParse.Valid(tidy(v))
    }

    /** Quantity converted when the user switches unit, keeping the same weight. */
    fun convertQuantity(quantity: Float, from: ServingOption, to: ServingOption): Float {
        val grams = gramsFor(quantity, from)
        val raw = grams / to.gramsPerUnit
        return tidy(raw.coerceIn(0.01f, to.maxQuantity))
    }

    fun nutritionFor(food: FoodItem, quantity: Float, option: ServingOption): CalculatedNutrition =
        NutritionCalculator.calculateFromFoodItem(food, gramsFor(quantity, option))

    /** Nutrition of exactly one of the food's own servings (for "Calories per dosa: ..."). */
    fun perServing(food: FoodItem): CalculatedNutrition =
        nutritionFor(food, 1f, servingOptionFor(food))

    fun formatQuantity(q: Float): String {
        val t = tidy(q)
        return if (t == t.toInt().toFloat()) t.toInt().toString() else t.toString()
    }

    /**
     * Rescales a diary entry to a new quantity using the entry's OWN stored snapshot
     * (never the live food definition), so editing a food later can't change history.
     * Returns null if the entry can't be rescaled or the quantity is invalid.
     */
    fun rescaleEntry(entry: FoodDiaryEntry, newQuantity: Float): FoodDiaryEntry? {
        if (!newQuantity.isFinite() || newQuantity <= 0f) return null
        if (entry.weightGrams <= 0f || entry.quantity <= 0f) return null
        val perUnitGrams = if (entry.servingGramsPerUnit > 0f) entry.servingGramsPerUnit else entry.weightGrams / entry.quantity
        val newGrams = newQuantity * perUnitGrams
        if (newGrams <= 0f || newGrams > MAX_GRAMS) return null
        val r = newGrams / entry.weightGrams
        val unitLabel = if (entry.servingUnit == "g" || entry.servingUnit == "ml") entry.servingUnit else entry.servingUnit
        return entry.copy(
            quantity = newQuantity,
            weightGrams = newGrams,
            servingGramsPerUnit = perUnitGrams,
            calories = entry.calories * r,
            protein = entry.protein * r,
            carbs = entry.carbs * r,
            fat = entry.fat * r,
            fiber = entry.fiber * r,
            sugar = entry.sugar * r,
            sodiumMg = entry.sodiumMg * r,
            servingDescription = "${formatQuantity(newQuantity)} $unitLabel"
        )
    }
}
