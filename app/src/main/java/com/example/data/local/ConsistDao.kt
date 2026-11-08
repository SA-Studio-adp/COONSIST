package com.example.data.local

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    fun getUserProfile(): Flow<UserProfile?>

    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    suspend fun getUserProfileOnce(): UserProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveUserProfile(profile: UserProfile)

    @Update
    suspend fun updateUserProfile(profile: UserProfile)
}

@Dao
interface FoodDao {
    @Query("SELECT * FROM food_database ORDER BY name ASC")
    fun getAllFoods(): Flow<List<FoodItem>>

    // "My Food" (custom) results are listed first, then verified database foods.
    @Query("SELECT * FROM food_database WHERE name LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%' ORDER BY isCustom DESC, isVerified DESC, name ASC")
    fun searchFoods(query: String): Flow<List<FoodItem>>

    @Query("SELECT * FROM food_database WHERE isCustom = 1 AND ownerUserId = :userId ORDER BY isFavorite DESC, name COLLATE NOCASE ASC")
    fun getCustomFoods(userId: Int): Flow<List<FoodItem>>

    @Query("SELECT * FROM food_database WHERE isFavorite = 1 ORDER BY isCustom DESC, name COLLATE NOCASE ASC")
    fun getFavoriteFoods(): Flow<List<FoodItem>>

    @Query("SELECT * FROM food_database WHERE isCustom = 1 AND ownerUserId = :userId AND LOWER(TRIM(name)) = LOWER(TRIM(:name)) LIMIT 1")
    suspend fun findCustomFoodByName(userId: Int, name: String): FoodItem?

    @Query("SELECT * FROM food_database WHERE stableId = :stableId LIMIT 1")
    suspend fun getFoodByStableId(stableId: String): FoodItem?

    /** ABORT (not REPLACE): a clash must fail loudly instead of silently overwriting another food. */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertFoodStrict(food: FoodItem): Long

    @Query("UPDATE food_database SET isFavorite = :favorite, updatedAt = :now WHERE id = :id")
    suspend fun setFavorite(id: Long, favorite: Boolean, now: Long): Int

    /** Only custom foods can be deleted; built-in foods are protected. Returns rows deleted. */
    @Query("DELETE FROM food_database WHERE id = :id AND isCustom = 1")
    suspend fun deleteCustomFood(id: Long): Int

    @Query("SELECT * FROM food_database WHERE barcode = :barcode LIMIT 1")
    suspend fun getFoodByBarcode(barcode: String): FoodItem?

    @Query("SELECT * FROM food_database WHERE id = :id LIMIT 1")
    suspend fun getFoodById(id: Long): FoodItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFood(food: FoodItem): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertFoods(foods: List<FoodItem>)

    @Update
    suspend fun updateFood(food: FoodItem): Int

    @Query("SELECT COUNT(*) FROM food_database")
    suspend fun getFoodCount(): Int
}

@Dao
interface DiaryDao {
    @Query("SELECT * FROM food_diary WHERE date = :date ORDER BY timestamp ASC")
    fun getEntriesForDate(date: String): Flow<List<FoodDiaryEntry>>

    @Query("SELECT * FROM food_diary WHERE date = :date AND mealType = :mealType ORDER BY timestamp ASC")
    fun getEntriesForMeal(date: String, mealType: String): Flow<List<FoodDiaryEntry>>

    @Query("SELECT * FROM food_diary ORDER BY timestamp DESC")
    fun getAllEntries(): Flow<List<FoodDiaryEntry>>

    /** Most recent entry per distinct food, from saved diary history (not memory). */
    @Query("SELECT * FROM food_diary WHERE id IN (SELECT MAX(id) FROM food_diary GROUP BY COALESCE(foodStableId, LOWER(foodName))) ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentEntries(limit: Int): Flow<List<FoodDiaryEntry>>

    @Query("SELECT * FROM food_diary WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    fun getEntriesBetween(startDate: String, endDate: String): Flow<List<FoodDiaryEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: FoodDiaryEntry): Long

    @Update
    suspend fun updateEntry(entry: FoodDiaryEntry)

    @Delete
    suspend fun deleteEntry(entry: FoodDiaryEntry)

    @Query("DELETE FROM food_diary WHERE id = :id")
    suspend fun deleteEntryById(id: Long)
}

@Dao
interface WaterDao {
    @Query("SELECT * FROM water_entries WHERE date = :date ORDER BY timestamp ASC")
    fun getWaterEntriesForDate(date: String): Flow<List<WaterEntry>>

    @Query("SELECT COALESCE(SUM(amountMl), 0) FROM water_entries WHERE date = :date")
    fun getTotalWaterForDate(date: String): Flow<Int>

    @Query("SELECT * FROM water_entries ORDER BY timestamp DESC")
    fun getAllWaterEntries(): Flow<List<WaterEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWater(entry: WaterEntry): Long

    @Delete
    suspend fun deleteWater(entry: WaterEntry)
}

@Dao
interface WeightDao {
    @Query("SELECT * FROM weight_entries ORDER BY date DESC, timestamp DESC")
    fun getAllWeightEntries(): Flow<List<WeightEntry>>

    @Query("SELECT * FROM weight_entries ORDER BY date DESC, timestamp DESC LIMIT 1")
    fun getLatestWeight(): Flow<WeightEntry?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeight(entry: WeightEntry): Long

    @Delete
    suspend fun deleteWeight(entry: WeightEntry)
}

@Dao
interface WorkoutDao {
    @Query("SELECT * FROM exercises ORDER BY targetMuscle ASC, name ASC")
    fun getAllExercises(): Flow<List<Exercise>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertExercises(exercises: List<Exercise>)

    @Query("SELECT COUNT(*) FROM exercises")
    suspend fun getExerciseCount(): Int

    @Query("SELECT * FROM workout_sessions WHERE date = :date ORDER BY timestamp DESC")
    fun getWorkoutsForDate(date: String): Flow<List<WorkoutSession>>

    @Query("SELECT * FROM workout_sessions ORDER BY date DESC, timestamp DESC")
    fun getAllWorkouts(): Flow<List<WorkoutSession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkoutSession(session: WorkoutSession): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkoutSets(sets: List<WorkoutSet>)

    @Query("SELECT * FROM workout_sets WHERE sessionId = :sessionId ORDER BY setNumber ASC")
    fun getSetsForSession(sessionId: Long): Flow<List<WorkoutSet>>
}

@Dao
interface RecipeDao {
    @Query("SELECT * FROM recipes ORDER BY name ASC")
    fun getAllRecipes(): Flow<List<Recipe>>

    @Query("SELECT * FROM recipes WHERE id = :id LIMIT 1")
    suspend fun getRecipeById(id: Long): Recipe?

    @Query("SELECT * FROM recipe_ingredients WHERE recipeId = :recipeId")
    fun getIngredientsForRecipe(recipeId: Long): Flow<List<RecipeIngredient>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipe(recipe: Recipe): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIngredients(ingredients: List<RecipeIngredient>)

    @Delete
    suspend fun deleteRecipe(recipe: Recipe)
}

@Dao
interface DietPlanDao {
    @Query("SELECT * FROM diet_plans WHERE isActive = 1 ORDER BY id DESC LIMIT 1")
    fun getActiveDietPlan(): Flow<DietPlan?>

    @Query("SELECT * FROM diet_meal_items WHERE planId = :planId ORDER BY id ASC")
    fun getItemsForPlan(planId: Long): Flow<List<DietMealItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDietPlan(plan: DietPlan): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDietMealItems(items: List<DietMealItem>)

    @Update
    suspend fun updateDietMealItem(item: DietMealItem)

    @Query("DELETE FROM diet_meal_items WHERE id = :id")
    suspend fun deleteDietMealItem(id: Long)
}
