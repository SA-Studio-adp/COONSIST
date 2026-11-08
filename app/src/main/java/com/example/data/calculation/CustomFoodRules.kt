package com.example.data.calculation

import com.example.data.model.FoodItem
import java.util.UUID
import kotlin.math.abs
import kotlin.math.max

/**
 * What the user types in the "Create custom food" form. All nutrient values are
 * for ONE serving of [servingQuantity] [servingUnit] weighing [servingWeightGrams].
 */
data class CustomFoodDraft(
    val name: String,
    val servingQuantity: Float = 1f,
    /** Display unit, e.g. "g", "ml", "piece", "cup", "bowl", "slice". */
    val servingUnit: String = "g",
    /** Weight of the whole serving in grams (equals servingQuantity when unit is g/ml). */
    val servingWeightGrams: Float = 100f,
    val calories: Float,
    val protein: Float = 0f,
    val carbs: Float = 0f,
    val fat: Float = 0f,
    val fiber: Float = 0f,
    val sugar: Float = 0f,
    val sodiumMg: Float = 0f
)

sealed interface CustomFoodValidation {
    /** [warnings] never block saving; they are shown so the user can double-check. */
    data class Valid(val warnings: List<String> = emptyList()) : CustomFoodValidation
    /** Field key -> message. Keys: name, serving, weight, calories, protein, carbs, fat, fiber, sugar, sodium. */
    data class Invalid(val errors: Map<String, String>) : CustomFoodValidation
}

object CustomFoodRules {

    const val MAX_NAME_LENGTH = 80
    private const val MAX_KCAL_PER_GRAM = 9.5f // fat is ~9 kcal/g; anything above is a typo

    fun normalizeName(name: String): String = name.trim().replace(Regex("\\s+"), " ")

    fun isMl(unit: String) = unit.trim().equals("ml", ignoreCase = true)

    fun validate(d: CustomFoodDraft): CustomFoodValidation {
        val errors = linkedMapOf<String, String>()
        val warnings = mutableListOf<String>()

        val name = normalizeName(d.name)
        if (name.isEmpty()) errors["name"] = "Enter a food name"
        else if (name.length > MAX_NAME_LENGTH) errors["name"] = "Name is too long (max $MAX_NAME_LENGTH)"

        if (!d.servingQuantity.isFinite() || d.servingQuantity <= 0f) errors["serving"] = "Serving size must be greater than 0"
        else if (d.servingQuantity > 10_000f) errors["serving"] = "Serving size is too large"

        if (d.servingUnit.isBlank()) errors["serving"] = "Choose a serving unit"

        if (!d.servingWeightGrams.isFinite() || d.servingWeightGrams <= 0f) errors["weight"] = "Weight of one serving (g) must be greater than 0"
        else if (d.servingWeightGrams > ServingCalculator.MAX_GRAMS) errors["weight"] = "Weight must be ${ServingCalculator.MAX_GRAMS.toInt()} g or less"

        fun nonNeg(key: String, label: String, v: Float) {
            if (!v.isFinite() || v < 0f) errors[key] = "$label can't be negative"
        }
        nonNeg("calories", "Calories", d.calories)
        nonNeg("protein", "Protein", d.protein)
        nonNeg("carbs", "Carbohydrates", d.carbs)
        nonNeg("fat", "Fat", d.fat)
        nonNeg("fiber", "Fibre", d.fiber)
        nonNeg("sugar", "Sugar", d.sugar)
        nonNeg("sodium", "Sodium", d.sodiumMg)

        if (errors.isNotEmpty()) return CustomFoodValidation.Invalid(errors)

        val w = d.servingWeightGrams
        // Physical sanity checks: can't have more macro grams than the serving weighs, or >9.5 kcal per gram.
        if (d.protein + d.carbs + d.fat > w * 1.02f) {
            errors["weight"] = "Protein + carbs + fat (${fmt(d.protein + d.carbs + d.fat)} g) exceed the serving weight (${fmt(w)} g). Check the values or the weight."
        }
        if (d.calories > w * MAX_KCAL_PER_GRAM) {
            errors["calories"] = "${fmt(d.calories)} kcal in ${fmt(w)} g is not possible (max ~9 kcal/g). Check the values."
        }
        if (d.fiber > d.carbs + 0.01f) warnings += "Fibre is higher than carbohydrates - check the label."
        if (d.sugar > d.carbs + 0.01f) warnings += "Sugar is higher than carbohydrates - check the label."
        if (errors.isNotEmpty()) return CustomFoodValidation.Invalid(errors)

        macroMismatchWarning(d.calories, d.protein, d.carbs, d.fat)?.let { warnings += it }
        return CustomFoodValidation.Valid(warnings)
    }

    /**
     * 4 kcal/g protein, 4 kcal/g carbs, 9 kcal/g fat. Labels round and treat fibre differently,
     * so only a large gap (>25 kcal AND >20%) is flagged. Returns null if consistent or macros not entered.
     */
    fun macroMismatchWarning(calories: Float, protein: Float, carbs: Float, fat: Float): String? {
        if (protein + carbs + fat <= 0f || calories <= 0f) return null
        val expected = 4f * protein + 4f * carbs + 9f * fat
        val diff = abs(expected - calories)
        return if (diff > 25f && diff > 0.20f * max(expected, calories)) {
            "Calories (${fmt(calories)}) don't match the macros (~${fmt(expected)} kcal from 4/4/9 kcal per g). Check the label."
        } else null
    }

    /** Converts a draft (per serving) into the per-100 g FoodItem stored in Room. */
    fun toFoodItem(
        d: CustomFoodDraft,
        ownerUserId: Int = 1,
        stableId: String = UUID.randomUUID().toString(),
        existingId: Long = 0L,
        createdAt: Long = System.currentTimeMillis(),
        now: Long = System.currentTimeMillis(),
        isFavorite: Boolean = false
    ): FoodItem {
        val w = d.servingWeightGrams
        fun per100(v: Float) = v * 100f / w
        val unit = d.servingUnit.trim()
        val label = "${ServingCalculator.formatQuantity(d.servingQuantity)} $unit (${fmt(w)}g)"
        return FoodItem(
            id = existingId,
            name = normalizeName(d.name),
            category = "Custom",
            state = "Prepared",
            defaultServingUnit = if (unit.equals("g", true) || isMl(unit)) "${fmt(w)}${unit.lowercase()}" else label,
            defaultWeightGrams = w,
            caloriesPer100g = per100(d.calories),
            proteinPer100g = per100(d.protein),
            carbsPer100g = per100(d.carbs),
            fatPer100g = per100(d.fat),
            fiberPer100g = per100(d.fiber),
            sugarPer100g = per100(d.sugar),
            sodiumMgPer100g = per100(d.sodiumMg),
            source = "User Custom",
            isVerified = false, // entered by the user, never presented as database-verified
            isFavorite = isFavorite,
            stableId = stableId,
            isCustom = true,
            ownerUserId = ownerUserId,
            servingUnitType = if (isMl(unit)) "ml" else "g",
            createdAt = createdAt,
            updatedAt = now
        )
    }

    /** Inverse of [toFoodItem]: fills the edit form from a stored food (one serving). */
    fun toDraft(food: FoodItem): CustomFoodDraft {
        val w = food.defaultWeightGrams
        fun perServing(per100: Float) = per100 * w / 100f
        val option = ServingCalculator.servingOptionFor(food)
        val isBase = option.gramsPerUnit == 1f || option.label == "serving"
        return CustomFoodDraft(
            name = food.name,
            servingQuantity = if (isBase) w else w / option.gramsPerUnit,
            servingUnit = if (isBase) (if (food.servingUnitType == "ml") "ml" else "g") else option.label,
            servingWeightGrams = w,
            calories = perServing(food.caloriesPer100g),
            protein = perServing(food.proteinPer100g),
            carbs = perServing(food.carbsPer100g),
            fat = perServing(food.fatPer100g),
            fiber = perServing(food.fiberPer100g),
            sugar = perServing(food.sugarPer100g),
            sodiumMg = perServing(food.sodiumMgPer100g)
        )
    }

    private fun fmt(v: Float): String =
        if (v == v.toInt().toFloat()) v.toInt().toString() else (Math.round(v * 10f) / 10f).toString()
}
