package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "diet_plans")
data class DietPlan(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val planType: String = "Daily", // "Daily" or "7-Day"
    val targetGoal: String,
    val dietaryType: String, // "Vegetarian", "Non-vegetarian", "Vegan"
    val totalCalories: Float,
    val totalProtein: Float,
    val totalCarbs: Float,
    val totalFat: Float,
    val isBudgetFriendly: Boolean = true,
    val shoppingListJson: String = "",
    val isActive: Boolean = true,
    val createdDate: String
)

@Entity(tableName = "diet_meal_items")
data class DietMealItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val planId: Long,
    val dayOfWeek: String = "Monday", // Monday..Sunday or "Everyday"
    val mealName: String, // "Breakfast", "Morning Snack", "Lunch", "Evening Snack", "Dinner"
    val foodItemName: String,
    val portionDescription: String,
    val weightGrams: Float,
    val calories: Float,
    val protein: Float,
    val carbs: Float,
    val fat: Float,
    val prepInstructions: String = "",
    val alternatives: String = ""
)
