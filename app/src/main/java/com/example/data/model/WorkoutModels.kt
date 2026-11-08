package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exercises")
data class Exercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val targetMuscle: String, // "Chest", "Back", "Legs", "Shoulders", "Arms", "Core", "Cardio"
    val equipment: String = "Barbell / Dumbbell",
    val description: String = "",
    val metValue: Float = 5.0f // Metabolic Equivalent of Task for calorie calculations
)

@Entity(tableName = "workout_sessions")
data class WorkoutSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val date: String, // "YYYY-MM-DD"
    val durationMinutes: Int,
    val caloriesBurned: Int,
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "workout_sets")
data class WorkoutSet(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val exerciseName: String,
    val setNumber: Int,
    val reps: Int,
    val weightKg: Float,
    val rpe: Int = 8 // Rate of Perceived Exertion
)
