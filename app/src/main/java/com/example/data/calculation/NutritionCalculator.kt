package com.example.data.calculation

import com.example.data.model.FoodItem
import com.example.data.model.RecipeIngredient
import kotlin.math.max
import kotlin.math.roundToInt

data class CalculatedNutrition(
    val calories: Float,
    val protein: Float,
    val carbs: Float,
    val fat: Float,
    val fiber: Float = 0f,
    val sugar: Float = 0f,
    val sodiumMg: Float = 0f,
    val potassiumMg: Float = 0f,
    val calciumMg: Float = 0f,
    val ironMg: Float = 0f,
    val isDatabaseVerified: Boolean = true,
    val dataSource: String = "USDA / IFCT"
)

data class MacroTargets(
    val targetCalories: Int,
    val targetProteinGrams: Float,
    val targetCarbsGrams: Float,
    val targetFatGrams: Float,
    val targetWaterMl: Int,
    val bmr: Int,
    val tdee: Int,
    val note: String = ""
)

object NutritionCalculator {

    /**
     * Deterministic Nutrient Scaling:
     * Nutrient amount = (Nutrient value per 100 g * Actual food weight in grams) / 100.0
     * Internal calculations preserve float/double precision and only format on presentation.
     */
    fun calculateNutrient(per100g: Float, weightGrams: Float): Float {
        if (weightGrams <= 0f || per100g <= 0f) return 0f
        return (per100g * weightGrams) / 100f
    }

    fun calculateTotalNutrition(
        caloriesPer100g: Float,
        proteinPer100g: Float,
        carbsPer100g: Float,
        fatPer100g: Float,
        fiberPer100g: Float = 0f,
        sugarPer100g: Float = 0f,
        sodiumMgPer100g: Float = 0f,
        potassiumMgPer100g: Float = 0f,
        calciumMgPer100g: Float = 0f,
        ironMgPer100g: Float = 0f,
        weightGrams: Float,
        isVerified: Boolean = true,
        source: String = "USDA / IFCT"
    ): CalculatedNutrition {
        return CalculatedNutrition(
            calories = calculateNutrient(caloriesPer100g, weightGrams),
            protein = calculateNutrient(proteinPer100g, weightGrams),
            carbs = calculateNutrient(carbsPer100g, weightGrams),
            fat = calculateNutrient(fatPer100g, weightGrams),
            fiber = calculateNutrient(fiberPer100g, weightGrams),
            sugar = calculateNutrient(sugarPer100g, weightGrams),
            sodiumMg = calculateNutrient(sodiumMgPer100g, weightGrams),
            potassiumMg = calculateNutrient(potassiumMgPer100g, weightGrams),
            calciumMg = calculateNutrient(calciumMgPer100g, weightGrams),
            ironMg = calculateNutrient(ironMgPer100g, weightGrams),
            isDatabaseVerified = isVerified,
            dataSource = source
        )
    }

    /**
     * Calculate nutrition directly from a verified FoodItem entity and specified gram weight.
     * Respects cooking state (Raw, Cooked, Boiled, Fried).
     */
    fun calculateFromFoodItem(food: FoodItem, weightGrams: Float): CalculatedNutrition {
        return calculateTotalNutrition(
            caloriesPer100g = food.caloriesPer100g,
            proteinPer100g = food.proteinPer100g,
            carbsPer100g = food.carbsPer100g,
            fatPer100g = food.fatPer100g,
            fiberPer100g = food.fiberPer100g,
            sugarPer100g = food.sugarPer100g,
            sodiumMgPer100g = food.sodiumMgPer100g,
            potassiumMgPer100g = food.potassiumMgPer100g,
            calciumMgPer100g = food.calciumMgPer100g,
            ironMgPer100g = food.ironMgPer100g,
            weightGrams = weightGrams,
            isVerified = food.isVerified,
            source = "${food.source} (${food.state})"
        )
    }

    /**
     * Matches detected or voice food names against verified local database catalog.
     * Prevents AI hallucinations from overriding verified nutrient values.
     */
    fun findVerifiedFoodMatch(detectedName: String, catalog: List<FoodItem>): FoodItem? {
        val query = detectedName.trim().lowercase()
        // Exact match
        catalog.find { it.name.lowercase() == query }?.let { return it }

        // Core name match (e.g. "Idli", "Sambar", "Dosa", "Chicken Breast", "Matta Rice")
        val keywords = listOf(
            "idli", "dosa", "appam", "puttu", "sambar", "matta rice", "white rice",
            "chapati", "moong dal", "cherupayar", "kadala", "paneer", "curd",
            "soy chunks", "tofu", "egg", "egg white", "chicken breast", "chicken curry",
            "fish curry", "fish fry", "avial", "thoran", "beef fry", "oats", "whey",
            "peanut butter", "almonds", "banana", "apple", "sweet potato", "broccoli"
        )
        for (kw in keywords) {
            if (query.contains(kw)) {
                val matched = catalog.find { it.name.lowercase().contains(kw) }
                if (matched != null) return matched
            }
        }
        return null
    }

    /**
     * Deterministic Recipe calculation across all ingredients and serving count.
     */
    fun calculateRecipePerServing(
        ingredients: List<RecipeIngredient>,
        servings: Int
    ): CalculatedNutrition {
        val safeServings = max(1, servings)
        var totalCals = 0.0
        var totalPro = 0.0
        var totalCarb = 0.0
        var totalFat = 0.0

        for (ing in ingredients) {
            totalCals += ing.calories.toDouble()
            totalPro += ing.protein.toDouble()
            totalCarb += ing.carbs.toDouble()
            totalFat += ing.fat.toDouble()
        }

        return CalculatedNutrition(
            calories = (totalCals / safeServings).toFloat(),
            protein = (totalPro / safeServings).toFloat(),
            carbs = (totalCarb / safeServings).toFloat(),
            fat = (totalFat / safeServings).toFloat(),
            isDatabaseVerified = true,
            dataSource = "Recipe Summation ($safeServings servings)"
        )
    }

    /**
     * BMR using the Mifflin-St Jeor Equation:
     * Men: BMR = (10 × weight in kg) + (6.25 × height in cm) - (5 × age in years) + 5
     * Women: BMR = (10 × weight in kg) + (6.25 × height in cm) - (5 × age in years) - 161
     */
    fun calculateBMR(weightKg: Float, heightCm: Float, age: Int, gender: String): Float {
        val base = (10f * weightKg) + (6.25f * heightCm) - (5f * age)
        return if (gender.equals("Female", ignoreCase = true)) {
            base - 161f
        } else {
            base + 5f
        }
    }

    fun getActivityMultiplier(activityLevel: String): Float {
        return when (activityLevel.lowercase()) {
            "sedentary" -> 1.2f
            "lightly active" -> 1.375f
            "moderately active" -> 1.55f
            "very active" -> 1.725f
            "extra active" -> 1.9f
            else -> 1.55f
        }
    }

    /**
     * Personalized Nutrition & Macro calculation with safe floors and minor protections.
     */
    fun calculatePersonalizedTargets(
        weightKg: Float,
        heightCm: Float,
        age: Int,
        gender: String,
        activityLevel: String,
        goal: String
    ): MacroTargets {
        val bmr = calculateBMR(weightKg, heightCm, age, gender)
        val multiplier = getActivityMultiplier(activityLevel)
        val tdee = bmr * multiplier

        val isMinor = age < 18
        var targetCalories = tdee

        when (goal.lowercase()) {
            "fat loss" -> {
                targetCalories = if (isMinor) {
                    // Under-18 protection: No energy deficits during adolescent growth
                    tdee
                } else {
                    tdee - 450f
                }
            }
            "muscle gain" -> targetCalories = tdee + 350f
            "strength improvement" -> targetCalories = tdee + 200f
            "healthy weight gain" -> targetCalories = tdee + 450f
            "endurance" -> targetCalories = tdee + 250f
            "weight maintenance", "general fitness" -> targetCalories = tdee
            else -> targetCalories = tdee
        }

        // Apply health safety floors
        val minCalorieFloor = if (gender.equals("Female", ignoreCase = true)) 1250f else 1550f
        if (targetCalories < minCalorieFloor) {
            targetCalories = minCalorieFloor
        }

        // Protein calculation:
        // Muscle Gain / Strength / Fat Loss: 1.8 - 2.2 g/kg
        // General / Maintenance: 1.4 - 1.6 g/kg
        val proteinMultiplier = when (goal.lowercase()) {
            "muscle gain", "strength improvement" -> 2.0f
            "fat loss" -> 2.0f // Preserve lean body mass
            "endurance" -> 1.6f
            else -> 1.5f
        }
        val targetProtein = weightKg * proteinMultiplier

        // Fat calculation: 0.9g per kg bodyweight
        val targetFat = max(weightKg * 0.9f, (targetCalories * 0.25f) / 9f)

        // Carbs calculation: Remaining calories / 4 kcal per gram
        val caloriesFromProtein = targetProtein * 4f
        val caloriesFromFat = targetFat * 9f
        val remainingCaloriesForCarbs = max(0f, targetCalories - caloriesFromProtein - caloriesFromFat)
        val targetCarbs = remainingCaloriesForCarbs / 4f

        val targetWater = max(2500, (weightKg * 38f).roundToInt())

        val safetyNote = if (isMinor && goal.equals("Fat loss", ignoreCase = true)) {
            "Energy deficit prevented for under-18 users to protect adolescent growth."
        } else {
            "Mifflin-St Jeor TDEE with deterministic macronutrient allocation."
        }

        return MacroTargets(
            targetCalories = targetCalories.roundToInt(),
            targetProteinGrams = (targetProtein * 10f).roundToInt() / 10f,
            targetCarbsGrams = (targetCarbs * 10f).roundToInt() / 10f,
            targetFatGrams = (targetFat * 10f).roundToInt() / 10f,
            targetWaterMl = targetWater,
            bmr = bmr.roundToInt(),
            tdee = tdee.roundToInt(),
            note = safetyNote
        )
    }

    fun calculateExerciseCalories(met: Float, weightKg: Float, durationMinutes: Int): Int {
        if (durationMinutes <= 0 || weightKg <= 0f) return 0
        val hours = durationMinutes / 60f
        return (met * weightKg * hours).roundToInt()
    }
}
