package com.example

import com.example.data.calculation.NutritionCalculator
import com.example.data.local.PreloadedFoodData
import com.example.data.model.RecipeIngredient
import org.junit.Assert.*
import org.junit.Test

class NutritionCalculationUnitTest {

    @Test
    fun testPer100gNutrientScaling() {
        // 165 kcal per 100g chicken breast. For 200g, should be exactly 330 kcal.
        val cal = NutritionCalculator.calculateNutrient(165f, 200f)
        assertEquals(330f, cal, 0.01f)

        // For 50g, should be exactly 82.5 kcal.
        val halfCal = NutritionCalculator.calculateNutrient(165f, 50f)
        assertEquals(82.5f, halfCal, 0.01f)

        // Zero weight returns 0
        assertEquals(0f, NutritionCalculator.calculateNutrient(165f, 0f), 0.01f)
    }

    @Test
    fun testKeralaFoodServingCalculations() {
        // Idli: 132 kcal, 4.5g P, 27.5g C, 0.5g F per 100g
        // 1 piece is 40g
        val idliNutrition = NutritionCalculator.calculateTotalNutrition(
            caloriesPer100g = 132f,
            proteinPer100g = 4.5f,
            carbsPer100g = 27.5f,
            fatPer100g = 0.5f,
            fiberPer100g = 1.8f,
            sugarPer100g = 0.3f,
            sodiumMgPer100g = 120f,
            weightGrams = 40f
        )

        assertEquals(52.8f, idliNutrition.calories, 0.01f)
        assertEquals(1.8f, idliNutrition.protein, 0.01f)
        assertEquals(11.0f, idliNutrition.carbs, 0.01f)
        assertEquals(0.2f, idliNutrition.fat, 0.01f)
    }

    @Test
    fun testRawVsCookedDistinction() {
        val rawOats = PreloadedFoodData.items.find { it.name.contains("Oats") && it.state == "Raw" }
        assertNotNull(rawOats)
        // 100g raw oats has ~389 kcal
        assertTrue(rawOats!!.caloriesPer100g > 350f)

        val cookedRice = PreloadedFoodData.items.find { it.name.contains("Rice") && it.state == "Cooked" }
        assertNotNull(cookedRice)
        // Cooked rice absorbs water and is ~110-130 kcal per 100g
        assertTrue(cookedRice!!.caloriesPer100g < 140f)
    }

    @Test
    fun testRecipeCalculationDeterministicSummation() {
        val ingredients = listOf(
            RecipeIngredient(recipeId = 1, foodName = "Chicken", weightGrams = 200f, calories = 330f, protein = 62f, carbs = 0f, fat = 7.2f),
            RecipeIngredient(recipeId = 1, foodName = "Rice", weightGrams = 300f, calories = 390f, protein = 8.1f, carbs = 84.6f, fat = 0.9f)
        )
        // 2 servings: Total calories = 720 / 2 = 360 kcal per serving
        val perServing = NutritionCalculator.calculateRecipePerServing(ingredients, servings = 2)
        assertEquals(360f, perServing.calories, 0.01f)
        assertEquals(35.05f, perServing.protein, 0.01f)
        assertEquals(42.3f, perServing.carbs, 0.01f)
    }

    @Test
    fun testFindVerifiedFoodMatch() {
        val catalog = PreloadedFoodData.items
        val matchedIdli = NutritionCalculator.findVerifiedFoodMatch("Steamed hot idlis", catalog)
        assertNotNull(matchedIdli)
        assertEquals("Idli (Steamed Rice & Urad Dal Cake)", matchedIdli!!.name)

        val matchedChicken = NutritionCalculator.findVerifiedFoodMatch("grilled chicken breast fillet", catalog)
        assertNotNull(matchedChicken)
        assertEquals("Chicken Breast (Skinless, Grilled/Boiled)", matchedChicken!!.name)

        val unknownFood = NutritionCalculator.findVerifiedFoodMatch("Exotic Unknown Berry X", catalog)
        assertNull(unknownFood)
    }

    @Test
    fun testMifflinStJeorEquation() {
        // Male: 70kg, 175cm, 25 years old
        // BMR = (10 * 70) + (6.25 * 175) - (5 * 25) + 5
        // BMR = 700 + 1093.75 - 125 + 5 = 1673.75
        val maleBMR = NutritionCalculator.calculateBMR(70f, 175f, 25, "Male")
        assertEquals(1673.75f, maleBMR, 0.01f)

        // Female: 60kg, 165cm, 28 years old
        // BMR = (10 * 60) + (6.25 * 165) - (5 * 28) - 161
        // BMR = 600 + 1031.25 - 140 - 161 = 1330.25
        val femaleBMR = NutritionCalculator.calculateBMR(60f, 165f, 28, "Female")
        assertEquals(1330.25f, femaleBMR, 0.01f)
    }

    @Test
    fun testTDEEAndMinorDeficitProtection() {
        // For a minor (<18), fat loss goal should NOT apply aggressive energy deficits
        val minorTargets = NutritionCalculator.calculatePersonalizedTargets(
            weightKg = 65f,
            heightCm = 170f,
            age = 16,
            gender = "Male",
            activityLevel = "Moderately Active",
            goal = "Fat loss"
        )

        // Since age < 18, target calories should equal TDEE (no deficit)
        assertEquals(minorTargets.tdee, minorTargets.targetCalories)
        assertTrue(minorTargets.note.contains("minor", ignoreCase = true) || minorTargets.note.contains("under-18", ignoreCase = true))
    }

    @Test
    fun testExerciseCalorieBurnFormula() {
        // MET = 6.0, Weight = 75kg, Duration = 60 minutes
        // Calories = 6.0 * 75 * (60/60) = 450 kcal
        val burn = NutritionCalculator.calculateExerciseCalories(6.0f, 75f, 60)
        assertEquals(450, burn)

        // 30 minutes = 225 kcal
        val burnHalf = NutritionCalculator.calculateExerciseCalories(6.0f, 75f, 30)
        assertEquals(225, burnHalf)
    }
}
