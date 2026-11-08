package com.example.data.repository

import com.example.data.model.FoodDiaryEntry
import com.example.data.model.FoodItem

/** Outcome of a database write. Success is only ever returned after the write has been verified. */
sealed interface SaveResult<out T> {
    data class Success<T>(val value: T, val warnings: List<String> = emptyList()) : SaveResult<T>
    data class Failure(val message: String, val fieldErrors: Map<String, String> = emptyMap()) : SaveResult<Nothing>
}

/** A previously eaten food, resolved from saved diary history, with the entry it came from. */
data class RecentFood(val food: FoodItem, val lastEntry: FoodDiaryEntry)
