package com.example.data.repository

import android.graphics.Bitmap
import com.example.data.ai.AiScanResponse
import com.example.data.ai.ConsistAiService
import com.example.data.ai.VoiceParseResult
import com.example.data.calculation.NutritionCalculator
import com.example.data.local.*
import com.example.data.model.*
import com.example.data.calculation.CustomFoodDraft
import com.example.data.calculation.CustomFoodRules
import com.example.data.calculation.CustomFoodValidation
import com.example.data.calculation.ServingCalculator
import com.example.data.calculation.ServingOption
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.*

class ConsistRepository(
    private val db: ConsistDatabase
) {
    private val userDao = db.userDao()
    private val foodDao = db.foodDao()
    private val diaryDao = db.diaryDao()
    private val waterDao = db.waterDao()
    private val weightDao = db.weightDao()
    private val workoutDao = db.workoutDao()
    private val recipeDao = db.recipeDao()
    private val dietPlanDao = db.dietPlanDao()

    // USER PROFILE
    val userProfile: Flow<UserProfile?> = userDao.getUserProfile()
    suspend fun getUserProfileOnce(): UserProfile? = userDao.getUserProfileOnce()
    suspend fun saveUserProfile(profile: UserProfile) = userDao.saveUserProfile(profile)

    // FOOD DATABASE
    val allFoods: Flow<List<FoodItem>> = foodDao.getAllFoods()
    fun searchFoods(query: String): Flow<List<FoodItem>> = foodDao.searchFoods(query)
    suspend fun getFoodByBarcode(barcode: String): FoodItem? = foodDao.getFoodByBarcode(barcode)
    suspend fun insertCustomFood(food: FoodItem): Long = foodDao.insertFood(food)

    // FOOD DIARY
    fun getDiaryEntriesForDate(date: String): Flow<List<FoodDiaryEntry>> = diaryDao.getEntriesForDate(date)
    fun getAllDiaryEntries(): Flow<List<FoodDiaryEntry>> = diaryDao.getAllEntries()
    fun getDiaryEntriesBetween(startDate: String, endDate: String): Flow<List<FoodDiaryEntry>> = diaryDao.getEntriesBetween(startDate, endDate)

    suspend fun logFoodItem(
        foodName: String,
        mealType: String,
        date: String,
        weightGrams: Float,
        servingDescription: String,
        cal100g: Float,
        pro100g: Float,
        carb100g: Float,
        fat100g: Float,
        fiber100g: Float = 0f,
        sugar100g: Float = 0f,
        sodium100g: Float = 0f,
        isAi: Boolean = false,
        confidenceNote: String = "Verified Database Calculation"
    ): Long {
        val calculated = NutritionCalculator.calculateTotalNutrition(
            caloriesPer100g = cal100g,
            proteinPer100g = pro100g,
            carbsPer100g = carb100g,
            fatPer100g = fat100g,
            fiberPer100g = fiber100g,
            sugarPer100g = sugar100g,
            sodiumMgPer100g = sodium100g,
            weightGrams = weightGrams
        )

        val entry = FoodDiaryEntry(
            foodName = foodName,
            mealType = mealType,
            date = date,
            weightGrams = weightGrams,
            servingDescription = servingDescription,
            calories = calculated.calories,
            protein = calculated.protein,
            carbs = calculated.carbs,
            fat = calculated.fat,
            fiber = calculated.fiber,
            sugar = calculated.sugar,
            sodiumMg = calculated.sodiumMg,
            isAiEstimated = isAi,
            confidenceNote = confidenceNote,
            quantity = weightGrams,
            servingUnit = "g",
            servingGramsPerUnit = 1f
        )
        return diaryDao.insertEntry(entry)
    }

    // ---------------------------------------------------------------------------------------
    // CUSTOM FOODS ("My Foods"), FAVOURITES, RECENTLY EATEN
    // ---------------------------------------------------------------------------------------
    /** The app has a single local profile (UserProfile.id == 1); custom foods are owned by it. */
    private val ownerId = 1

    val customFoods: Flow<List<FoodItem>> = foodDao.getCustomFoods(ownerId)
    val favoriteFoods: Flow<List<FoodItem>> = foodDao.getFavoriteFoods()

    /** Built from saved diary rows (Room), so it survives restarts and reboots. */
    val recentFoods: Flow<List<RecentFood>> = diaryDao.getRecentEntries(40).map { entries ->
        entries.mapNotNull { e -> resolveFoodForEntry(e)?.let { RecentFood(it, e) } }
    }

    /** Live definition if it still exists, otherwise a per-100 g snapshot rebuilt from the entry itself. */
    private suspend fun resolveFoodForEntry(e: FoodDiaryEntry): FoodItem? {
        e.foodStableId?.let { id -> foodDao.getFoodByStableId(id)?.let { return it } }
        if (e.weightGrams <= 0f) return null
        fun per100(v: Float) = v * 100f / e.weightGrams
        val unitGrams = if (e.servingGramsPerUnit > 0f) e.servingGramsPerUnit else 1f
        val isBase = e.servingUnit == "g" || e.servingUnit == "ml"
        return FoodItem(
            name = e.foodName,
            category = "Recent",
            state = "Logged",
            defaultServingUnit = if (isBase) "100g" else "1 ${e.servingUnit} (${unitGrams.toInt()}g)",
            defaultWeightGrams = if (isBase) 100f else unitGrams,
            caloriesPer100g = per100(e.calories),
            proteinPer100g = per100(e.protein),
            carbsPer100g = per100(e.carbs),
            fatPer100g = per100(e.fat),
            fiberPer100g = per100(e.fiber),
            sugarPer100g = per100(e.sugar),
            sodiumMgPer100g = per100(e.sodiumMg),
            source = "Previously logged",
            isVerified = false,
            stableId = e.foodStableId ?: "snapshot-${e.foodName.lowercase()}",
            servingUnitType = if (e.servingUnit == "ml") "ml" else "g"
        )
    }

    /**
     * Saves a reusable custom food. Returns Success ONLY after the row has been written and read back.
     * Never throws for expected problems (validation, duplicates, database errors).
     */
    suspend fun saveCustomFood(draft: CustomFoodDraft): SaveResult<FoodItem> {
        val validation = CustomFoodRules.validate(draft)
        if (validation is CustomFoodValidation.Invalid) {
            return SaveResult.Failure("Please fix the highlighted fields.", validation.errors)
        }
        val warnings = (validation as CustomFoodValidation.Valid).warnings
        return try {
            val name = CustomFoodRules.normalizeName(draft.name)
            if (foodDao.findCustomFoodByName(ownerId, name) != null) {
                return SaveResult.Failure(
                    "You already have a custom food called \"$name\". Open it in My Foods to edit it.",
                    mapOf("name" to "Already in My Foods")
                )
            }
            val food = CustomFoodRules.toFoodItem(draft, ownerId)
            val rowId = foodDao.insertFoodStrict(food)
            val saved = foodDao.getFoodById(rowId)
                ?: return SaveResult.Failure("The food could not be saved (it was not found after writing). Nothing was added.")
            SaveResult.Success(saved, warnings)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            SaveResult.Failure("Couldn't save this food: ${e.message ?: "database error"}. Nothing was saved.")
        }
    }

    /** Edits a custom food definition. Past diary entries keep their own nutrition snapshot and are NOT changed. */
    suspend fun updateCustomFood(existing: FoodItem, draft: CustomFoodDraft): SaveResult<FoodItem> {
        if (!existing.isCustom) return SaveResult.Failure("Built-in foods can't be edited.")
        val validation = CustomFoodRules.validate(draft)
        if (validation is CustomFoodValidation.Invalid) {
            return SaveResult.Failure("Please fix the highlighted fields.", validation.errors)
        }
        val warnings = (validation as CustomFoodValidation.Valid).warnings
        return try {
            val clash = foodDao.findCustomFoodByName(ownerId, CustomFoodRules.normalizeName(draft.name))
            if (clash != null && clash.id != existing.id) {
                return SaveResult.Failure("Another custom food already uses that name.", mapOf("name" to "Already in My Foods"))
            }
            val updated = CustomFoodRules.toFoodItem(
                draft, existing.ownerUserId, existing.stableId, existing.id,
                existing.createdAt, System.currentTimeMillis(), existing.isFavorite
            )
            if (foodDao.updateFood(updated) != 1) return SaveResult.Failure("The food could not be updated. Nothing was changed.")
            val reread = foodDao.getFoodById(existing.id)
                ?: return SaveResult.Failure("The food could not be updated.")
            SaveResult.Success(reread, warnings)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            SaveResult.Failure("Couldn't update this food: ${e.message ?: "database error"}.")
        }
    }

    /** Deletes only the reusable definition. Diary entries that used it are kept untouched. */
    suspend fun deleteCustomFood(food: FoodItem): SaveResult<Unit> = try {
        if (foodDao.deleteCustomFood(food.id) == 1) SaveResult.Success(Unit)
        else SaveResult.Failure("That food could not be deleted.")
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        SaveResult.Failure("Couldn't delete this food: ${e.message ?: "database error"}.")
    }

    suspend fun toggleFavorite(food: FoodItem): SaveResult<Boolean> = try {
        if (food.id <= 0L) SaveResult.Failure("Add this food to your diary first, then favourite it from My Foods or search.")
        else if (foodDao.setFavorite(food.id, !food.isFavorite, System.currentTimeMillis()) == 1) SaveResult.Success(!food.isFavorite)
        else SaveResult.Failure("Couldn't update favourite.")
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        SaveResult.Failure("Couldn't update favourite: ${e.message ?: "database error"}.")
    }

    /**
     * Logs [quantity] x [option] of [food] to the diary. Nutrition is computed deterministically
     * from the food's per-100 g values and stored as a snapshot on the entry.
     */
    suspend fun logFood(
        food: FoodItem,
        mealType: String,
        date: String,
        quantity: Float,
        option: ServingOption
    ): SaveResult<Long> {
        val grams = ServingCalculator.gramsFor(quantity, option)
        if (!quantity.isFinite() || quantity <= 0f || grams <= 0f || grams > ServingCalculator.MAX_GRAMS) {
            return SaveResult.Failure("Enter a valid quantity.")
        }
        return try {
            val n = ServingCalculator.nutritionFor(food, quantity, option)
            val entry = FoodDiaryEntry(
                foodId = food.id.takeIf { it > 0L },
                foodStableId = food.stableId,
                foodName = food.name,
                mealType = mealType,
                date = date,
                weightGrams = grams,
                servingDescription = "${ServingCalculator.formatQuantity(quantity)} ${option.label}",
                calories = n.calories, protein = n.protein, carbs = n.carbs, fat = n.fat,
                fiber = n.fiber, sugar = n.sugar, sodiumMg = n.sodiumMg,
                isAiEstimated = false,
                confidenceNote = if (food.isCustom) "My Food (values entered by you)" else "Verified Database (${food.source})",
                quantity = quantity,
                servingUnit = option.label,
                servingGramsPerUnit = option.gramsPerUnit
            )
            val id = diaryDao.insertEntry(entry)
            if (id <= 0L) SaveResult.Failure("The entry could not be saved.") else SaveResult.Success(id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            SaveResult.Failure("Couldn't add to your diary: ${e.message ?: "database error"}.")
        }
    }

    /** Edits an existing diary entry's quantity (in the unit it was logged in). */
    suspend fun updateDiaryEntryQuantity(entry: FoodDiaryEntry, newQuantity: Float): SaveResult<FoodDiaryEntry> {
        val updated = ServingCalculator.rescaleEntry(entry, newQuantity)
            ?: return SaveResult.Failure("Enter a valid quantity.")
        return try {
            diaryDao.updateEntry(updated)
            SaveResult.Success(updated)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            SaveResult.Failure("Couldn't update this entry: ${e.message ?: "database error"}.")
        }
    }

    suspend fun updateDiaryEntry(entry: FoodDiaryEntry) = diaryDao.updateEntry(entry)
    suspend fun deleteDiaryEntry(entry: FoodDiaryEntry) = diaryDao.deleteEntry(entry)
    suspend fun deleteDiaryEntryById(id: Long) = diaryDao.deleteEntryById(id)

    // WATER TRACKER
    fun getWaterEntriesForDate(date: String): Flow<List<WaterEntry>> = waterDao.getWaterEntriesForDate(date)
    fun getTotalWaterForDate(date: String): Flow<Int> = waterDao.getTotalWaterForDate(date)
    fun getAllWaterEntries(): Flow<List<WaterEntry>> = waterDao.getAllWaterEntries()
    suspend fun logWater(date: String, amountMl: Int): Long {
        return waterDao.insertWater(WaterEntry(date = date, amountMl = amountMl))
    }
    suspend fun deleteWater(entry: WaterEntry) = waterDao.deleteWater(entry)

    // WEIGHT TRACKER
    val allWeightEntries: Flow<List<WeightEntry>> = weightDao.getAllWeightEntries()
    val latestWeight: Flow<WeightEntry?> = weightDao.getLatestWeight()
    suspend fun logWeight(date: String, weightKg: Float, notes: String = ""): Long {
        return weightDao.insertWeight(WeightEntry(date = date, weightKg = weightKg, notes = notes))
    }
    suspend fun deleteWeight(entry: WeightEntry) = weightDao.deleteWeight(entry)

    // WORKOUT TRACKER
    val allExercises: Flow<List<Exercise>> = workoutDao.getAllExercises()
    fun getWorkoutsForDate(date: String): Flow<List<WorkoutSession>> = workoutDao.getWorkoutsForDate(date)
    val allWorkouts: Flow<List<WorkoutSession>> = workoutDao.getAllWorkouts()

    suspend fun logWorkout(
        title: String,
        date: String,
        durationMinutes: Int,
        caloriesBurned: Int,
        notes: String = "",
        sets: List<WorkoutSet> = emptyList()
    ): Long {
        val sessionId = workoutDao.insertWorkoutSession(
            WorkoutSession(
                title = title,
                date = date,
                durationMinutes = durationMinutes,
                caloriesBurned = caloriesBurned,
                notes = notes
            )
        )
        if (sets.isNotEmpty()) {
            val setsWithSession = sets.map { it.copy(sessionId = sessionId) }
            workoutDao.insertWorkoutSets(setsWithSession)
        }
        return sessionId
    }

    // RECIPES
    val allRecipes: Flow<List<Recipe>> = recipeDao.getAllRecipes()
    fun getIngredientsForRecipe(recipeId: Long): Flow<List<RecipeIngredient>> = recipeDao.getIngredientsForRecipe(recipeId)

    suspend fun saveRecipe(
        name: String,
        servings: Int,
        cookingMethod: String,
        instructions: String,
        isKeralaSpecial: Boolean,
        ingredients: List<RecipeIngredient>
    ): Long {
        var totalWeight = 0f
        var totalCal = 0f
        var totalPro = 0f
        var totalCarb = 0f
        var totalFat = 0f

        for (ing in ingredients) {
            totalWeight += ing.weightGrams
            totalCal += ing.calories
            totalPro += ing.protein
            totalCarb += ing.carbs
            totalFat += ing.fat
        }

        val safeServings = if (servings > 0) servings else 1
        val recipe = Recipe(
            name = name,
            servings = safeServings,
            cookingMethod = cookingMethod,
            totalWeightGrams = totalWeight,
            totalCalories = totalCal,
            totalProtein = totalPro,
            totalCarbs = totalCarb,
            totalFat = totalFat,
            caloriesPerServing = totalCal / safeServings,
            proteinPerServing = totalPro / safeServings,
            carbsPerServing = totalCarb / safeServings,
            fatPerServing = totalFat / safeServings,
            isKeralaSpecial = isKeralaSpecial,
            instructions = instructions
        )

        val recipeId = recipeDao.insertRecipe(recipe)
        val ingredientsWithId = ingredients.map { it.copy(recipeId = recipeId) }
        recipeDao.insertIngredients(ingredientsWithId)
        return recipeId
    }

    suspend fun deleteRecipe(recipe: Recipe) = recipeDao.deleteRecipe(recipe)

    // DIET PLANS
    val activeDietPlan: Flow<DietPlan?> = dietPlanDao.getActiveDietPlan()
    fun getPlanItems(planId: Long): Flow<List<DietMealItem>> = dietPlanDao.getItemsForPlan(planId)

    suspend fun generateAndSaveDietPlan(profile: UserProfile, dateStr: String): Long {
        val (plan, items) = ConsistAiService.generateDeterministicDietPlan(profile, dateStr)
        val planId = dietPlanDao.insertDietPlan(plan)
        val itemsWithId = items.map { it.copy(planId = planId) }
        dietPlanDao.insertDietMealItems(itemsWithId)
        return planId
    }

    suspend fun updateDietMealItem(item: DietMealItem) = dietPlanDao.updateDietMealItem(item)
    suspend fun deleteDietMealItem(id: Long) = dietPlanDao.deleteDietMealItem(id)

    // AI SERVICES
    suspend fun scanMealImage(bitmap: Bitmap, profile: UserProfile): AiScanResponse {
        return ConsistAiService.scanMealImage(bitmap, profile)
    }

    suspend fun parseVoiceLog(speechText: String, profile: UserProfile): VoiceParseResult {
        return ConsistAiService.parseVoiceLog(speechText, profile)
    }

    suspend fun askCoach(userMessage: String, profile: UserProfile, todayCals: Float, todayPro: Float, todayCarbs: Float, todayFat: Float): String {
        return ConsistAiService.askCoach(userMessage, profile, todayCals, todayPro, todayCarbs, todayFat)
    }

    companion object {
        fun getTodayDateString(): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            return sdf.format(Date())
        }
    }
}
