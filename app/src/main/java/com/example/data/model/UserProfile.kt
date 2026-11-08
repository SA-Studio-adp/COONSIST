package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Int = 1,
    val name: String = "",
    val age: Int = 25,
    val gender: String = "Male", // "Male", "Female", "Other"
    val heightCm: Float = 175f,
    val currentWeightKg: Float = 72f,
    val targetWeightKg: Float = 75f,
    val activityLevel: String = "Moderately Active", // "Sedentary", "Lightly Active", "Moderately Active", "Very Active", "Extra Active"
    val exerciseFrequency: String = "3-4 days/week",
    val fitnessExperience: String = "Intermediate", // "Beginner", "Intermediate", "Advanced"
    val primaryGoal: String = "Muscle gain", // "Muscle gain", "Strength improvement", "Healthy weight gain", "Fat loss", "Endurance", "Weight maintenance", "General fitness"
    val dietaryPreference: String = "Non-vegetarian", // "Vegetarian", "Non-vegetarian", "Vegan"
    val eatsEggs: Boolean = true, // for vegetarians who eat eggs
    val allergies: String = "None",
    val foodIntolerances: String = "None",
    val foodsToAvoid: String = "None",
    val dislikedFoods: String = "None",
    val preferredCuisines: String = "Indian, Kerala, Continental",
    val foodBudget: String = "Medium", // "Low", "Medium", "High"
    val mealsPerDay: Int = 4,
    val targetCalories: Int = 2400,
    val targetProteinGrams: Float = 145f,
    val targetCarbsGrams: Float = 270f,
    val targetFatGrams: Float = 65f,
    val targetWaterMl: Int = 3000,
    val isOnboarded: Boolean = false,
    val currentStreak: Int = 1,
    val lastActiveDate: String = "",
    // Privacy, Consent & Backend Security Settings
    val aiCloudConsentGiven: Boolean = false,
    val secureBackendUrl: String = "",
    val secureBackendToken: String = "",
    val localOnlyMode: Boolean = true
)
