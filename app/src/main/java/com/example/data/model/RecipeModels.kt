package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recipes")
data class Recipe(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val servings: Int = 1,
    val cookingMethod: String = "Boiled / Cooked",
    val totalWeightGrams: Float = 0f,
    // Total nutrients for the entire recipe
    val totalCalories: Float = 0f,
    val totalProtein: Float = 0f,
    val totalCarbs: Float = 0f,
    val totalFat: Float = 0f,
    val totalFiber: Float = 0f,
    // Per-serving nutrients (Total / servings)
    val caloriesPerServing: Float = 0f,
    val proteinPerServing: Float = 0f,
    val carbsPerServing: Float = 0f,
    val fatPerServing: Float = 0f,
    val isKeralaSpecial: Boolean = false,
    val instructions: String = ""
)

@Entity(tableName = "recipe_ingredients")
data class RecipeIngredient(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recipeId: Long,
    val foodId: Long? = null,
    val foodName: String,
    val weightGrams: Float,
    val calories: Float,
    val protein: Float,
    val carbs: Float,
    val fat: Float
)
