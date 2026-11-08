package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "food_diary")
data class FoodDiaryEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val foodId: Long? = null,
    val foodName: String,
    val mealType: String, // "Breakfast", "Morning Snack", "Lunch", "Evening Snack", "Dinner", "Other"
    val date: String, // "YYYY-MM-DD"
    val timestamp: Long = System.currentTimeMillis(),
    val weightGrams: Float,
    val servingDescription: String = "grams",
    // Mathematically computed values:
    val calories: Float,
    val protein: Float,
    val carbs: Float,
    val fat: Float,
    val fiber: Float = 0f,
    val sugar: Float = 0f,
    val sodiumMg: Float = 0f,
    val isAiEstimated: Boolean = false,
    val confidenceNote: String = "Verified Database Calculation", // e.g. "Estimated via AI Scanner - User Confirmed"
    // --- Added in DB v3 ---
    /** Stable id of the food definition this entry came from (null for AI/voice/recipe entries). */
    val foodStableId: String? = null,
    /** Quantity the user chose, in [servingUnit]s (e.g. 4 dosa, or 150 g). */
    val quantity: Float = 1f,
    /** Unit label of the quantity, e.g. "medium dosa", "g", "ml". */
    val servingUnit: String = "g",
    /** Grams represented by ONE unit of [servingUnit]. weightGrams == quantity * servingGramsPerUnit. */
    val servingGramsPerUnit: Float = 1f
)
