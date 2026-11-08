package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "food_database",
    indices = [Index(value = ["stableId"], unique = true)]
)
data class FoodItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String, // "Indian/Kerala", "Grains", "Protein", "Dairy", "Vegetables", "Fruits", "Packaged", "Custom"
    val state: String = "Cooked", // "Raw", "Cooked", "Boiled", "Fried", "Prepared"
    val defaultServingUnit: String = "100g", // e.g. "100g", "1 piece", "1 cup (150g)", "1 bowl (200g)"
    val defaultWeightGrams: Float = 100f,
    // Nutrients per 100g (Deterministic basis)
    val caloriesPer100g: Float,
    val proteinPer100g: Float,
    val carbsPer100g: Float,
    val fatPer100g: Float,
    val fiberPer100g: Float = 0f,
    val sugarPer100g: Float = 0f,
    val sodiumMgPer100g: Float = 0f,
    val potassiumMgPer100g: Float = 0f,
    val calciumMgPer100g: Float = 0f,
    val ironMgPer100g: Float = 0f,
    val source: String = "USDA / IFCT", // "USDA", "IFCT (Indian Food Composition)", "User Custom", "AI Estimate"
    val isVerified: Boolean = true,
    val isFavorite: Boolean = false,
    val barcode: String? = null,
    // --- Added in DB v3 (custom foods / stable identity) ---
    /** Stable identifier that never changes, even if the food is edited. Diary rows reference it. */
    val stableId: String = UUID.randomUUID().toString(),
    /** True for user-created foods ("My Food"). */
    val isCustom: Boolean = false,
    /** Owning profile. The app currently has a single profile (id = 1). */
    val ownerUserId: Int = 1,
    /** Base unit the food is measured in: "g" or "ml". */
    val servingUnitType: String = "g",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
