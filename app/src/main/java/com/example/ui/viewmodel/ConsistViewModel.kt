package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.AiScanResponse
import com.example.data.ai.AiScannedFoodResult
import com.example.data.ai.VoiceParseResult
import com.example.data.calculation.CustomFoodDraft
import com.example.data.calculation.NutritionCalculator
import com.example.data.calculation.ServingCalculator
import com.example.data.calculation.ServingOption
import com.example.data.local.ConsistDatabase
import com.example.data.model.*
import com.example.data.repository.ConsistRepository
import com.example.data.repository.RecentFood
import com.example.data.repository.SaveResult
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val sender: String, // "USER" or "COACH"
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class DailyNutritionSummary(
    val calories: Float = 0f,
    val protein: Float = 0f,
    val carbs: Float = 0f,
    val fat: Float = 0f,
    val fiber: Float = 0f,
    val sodiumMg: Float = 0f
)

class ConsistViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ConsistRepository

    init {
        val db = ConsistDatabase.getDatabase(application)
        repository = ConsistRepository(db)
    }

    private val _selectedDate = MutableStateFlow(ConsistRepository.getTodayDateString())
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    val userProfile: StateFlow<UserProfile?> = repository.userProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Diary Entries for current selected date
    val currentDayEntries: StateFlow<List<FoodDiaryEntry>> = _selectedDate
        .flatMapLatest { date -> repository.getDiaryEntriesForDate(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Calculated daily totals
    val dailySummary: StateFlow<DailyNutritionSummary> = currentDayEntries.map { entries ->
        var cal = 0f
        var pro = 0f
        var carb = 0f
        var fat = 0f
        var fib = 0f
        var sod = 0f
        for (e in entries) {
            cal += e.calories
            pro += e.protein
            carb += e.carbs
            fat += e.fat
            fib += e.fiber
            sod += e.sodiumMg
        }
        DailyNutritionSummary(cal, pro, carb, fat, fib, sod)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DailyNutritionSummary())

    // Water Intake
    val currentDayWater: StateFlow<Int> = _selectedDate
        .flatMapLatest { date -> repository.getTotalWaterForDate(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val currentDayWaterEntries: StateFlow<List<WaterEntry>> = _selectedDate
        .flatMapLatest { date -> repository.getWaterEntriesForDate(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Weight Tracking
    val allWeightEntries: StateFlow<List<WeightEntry>> = repository.allWeightEntries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val latestWeight: StateFlow<WeightEntry?> = repository.latestWeight
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Workouts for selected date
    val currentDayWorkouts: StateFlow<List<WorkoutSession>> = _selectedDate
        .flatMapLatest { date -> repository.getWorkoutsForDate(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allWorkouts: StateFlow<List<WorkoutSession>> = repository.allWorkouts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allExercises: StateFlow<List<Exercise>> = repository.allExercises
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Food Database Search
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val foodSearchResults: StateFlow<List<FoodItem>> = _searchQuery
        .debounce(250)
        .flatMapLatest { query ->
            if (query.isBlank()) repository.allFoods
            else repository.searchFoods(query)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // My Foods (custom), Favourites and Recently Eaten - all backed by Room, never by in-memory lists
    val customFoods: StateFlow<List<FoodItem>> = repository.customFoods
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteFoods: StateFlow<List<FoodItem>> = repository.favoriteFoods
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentFoods: StateFlow<List<RecentFood>> = repository.recentFoods
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _customFoodUi = MutableStateFlow<CustomFoodUiState>(CustomFoodUiState.Idle)
    val customFoodUi: StateFlow<CustomFoodUiState> = _customFoodUi.asStateFlow()

    /** One-shot user message (toast) for food actions; the UI calls [consumeFoodMessage] after showing it. */
    private val _foodMessage = MutableStateFlow<String?>(null)
    val foodMessage: StateFlow<String?> = _foodMessage.asStateFlow()

    fun consumeFoodMessage() { _foodMessage.value = null }
    fun resetCustomFoodUi() { _customFoodUi.value = CustomFoodUiState.Idle }

    // Diet Planner
    val activeDietPlan: StateFlow<DietPlan?> = repository.activeDietPlan
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val activePlanItems: StateFlow<List<DietMealItem>> = activeDietPlan
        .flatMapLatest { plan ->
            if (plan != null) repository.getPlanItems(plan.id)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Recipes
    val allRecipes: StateFlow<List<Recipe>> = repository.allRecipes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // AI Food Scanner State
    private val _scanState = MutableStateFlow<AiScanUiState>(AiScanUiState.Idle)
    val scanState: StateFlow<AiScanUiState> = _scanState.asStateFlow()

    // Voice Logging State
    private val _voiceParseResult = MutableStateFlow<VoiceParseResult?>(null)
    val voiceParseResult: StateFlow<VoiceParseResult?> = _voiceParseResult.asStateFlow()
    private val _isVoiceProcessing = MutableStateFlow(false)
    val isVoiceProcessing: StateFlow<Boolean> = _isVoiceProcessing.asStateFlow()

    // AI Coach Chat
    private val _coachMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                sender = "COACH",
                text = "Welcome to CONSIST. I'm your AI Nutrition and Fitness Coach. Ask me anything about your macros, food alternatives, Kerala & Indian meals, or training targets. BUILD. TRAIN. REPEAT."
            )
        )
    )
    val coachMessages: StateFlow<List<ChatMessage>> = _coachMessages.asStateFlow()

    private val _isCoachThinking = MutableStateFlow(false)
    val isCoachThinking: StateFlow<Boolean> = _isCoachThinking.asStateFlow()

    // Date Navigation
    fun setSelectedDate(date: String) {
        _selectedDate.value = date
    }

    fun stepDate(days: Int) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        try {
            val date = sdf.parse(_selectedDate.value) ?: Date()
            val cal = Calendar.getInstance()
            cal.time = date
            cal.add(Calendar.DAY_OF_YEAR, days)
            _selectedDate.value = sdf.format(cal.time)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Save Onboarding Profile with deterministic calculations
    fun completeOnboarding(
        name: String,
        age: Int,
        gender: String,
        heightCm: Float,
        currentWeightKg: Float,
        targetWeightKg: Float,
        activityLevel: String,
        exerciseFrequency: String,
        fitnessExperience: String,
        primaryGoal: String,
        dietaryPreference: String,
        eatsEggs: Boolean,
        allergies: String,
        foodIntolerances: String,
        foodsToAvoid: String,
        dislikedFoods: String,
        preferredCuisines: String,
        foodBudget: String,
        mealsPerDay: Int
    ) {
        viewModelScope.launch {
            val targets = NutritionCalculator.calculatePersonalizedTargets(
                weightKg = currentWeightKg,
                heightCm = heightCm,
                age = age,
                gender = gender,
                activityLevel = activityLevel,
                goal = primaryGoal
            )

            val profile = UserProfile(
                id = 1,
                name = name.ifBlank { "Athlete" },
                age = age,
                gender = gender,
                heightCm = heightCm,
                currentWeightKg = currentWeightKg,
                targetWeightKg = targetWeightKg,
                activityLevel = activityLevel,
                exerciseFrequency = exerciseFrequency,
                fitnessExperience = fitnessExperience,
                primaryGoal = primaryGoal,
                dietaryPreference = dietaryPreference,
                eatsEggs = eatsEggs,
                allergies = allergies.ifBlank { "None" },
                foodIntolerances = foodIntolerances.ifBlank { "None" },
                foodsToAvoid = foodsToAvoid.ifBlank { "None" },
                dislikedFoods = dislikedFoods.ifBlank { "None" },
                preferredCuisines = preferredCuisines,
                foodBudget = foodBudget,
                mealsPerDay = mealsPerDay,
                targetCalories = targets.targetCalories,
                targetProteinGrams = targets.targetProteinGrams,
                targetCarbsGrams = targets.targetCarbsGrams,
                targetFatGrams = targets.targetFatGrams,
                targetWaterMl = targets.targetWaterMl,
                isOnboarded = true,
                currentStreak = 1,
                lastActiveDate = ConsistRepository.getTodayDateString()
            )

            repository.saveUserProfile(profile)

            // Auto-generate initial personalized diet plan
            repository.generateAndSaveDietPlan(profile, ConsistRepository.getTodayDateString())

            // Record initial weight entry
            repository.logWeight(ConsistRepository.getTodayDateString(), currentWeightKg, "Initial check-in")
        }
    }

    fun updateProfile(profile: UserProfile) {
        viewModelScope.launch {
            val targets = NutritionCalculator.calculatePersonalizedTargets(
                weightKg = profile.currentWeightKg,
                heightCm = profile.heightCm,
                age = profile.age,
                gender = profile.gender,
                activityLevel = profile.activityLevel,
                goal = profile.primaryGoal
            )
            val updated = profile.copy(
                targetCalories = targets.targetCalories,
                targetProteinGrams = targets.targetProteinGrams,
                targetCarbsGrams = targets.targetCarbsGrams,
                targetFatGrams = targets.targetFatGrams,
                targetWaterMl = targets.targetWaterMl
            )
            repository.saveUserProfile(updated)
        }
    }

    // Food Logging
    fun logFoodItem(
        food: FoodItem,
        mealType: String,
        weightGrams: Float,
        servingDesc: String
    ) {
        viewModelScope.launch {
            repository.logFoodItem(
                foodName = food.name,
                mealType = mealType,
                date = _selectedDate.value,
                weightGrams = weightGrams,
                servingDescription = servingDesc,
                cal100g = food.caloriesPer100g,
                pro100g = food.proteinPer100g,
                carb100g = food.carbsPer100g,
                fat100g = food.fatPer100g,
                fiber100g = food.fiberPer100g,
                sugar100g = food.sugarPer100g,
                sodium100g = food.sodiumMgPer100g,
                isAi = false,
                confidenceNote = "Verified Database (${food.source})"
            )
        }
    }

    /**
     * Saves a reusable custom food to Room. If [logToMeal] is set, also adds one serving to that meal
     * on the selected date ("save and log" in one flow). The UI is told "Saved" only after the write is verified.
     */
    fun saveCustomFood(draft: CustomFoodDraft, logToMeal: String? = null) {
        if (_customFoodUi.value is CustomFoodUiState.Saving) return // ignore double taps
        _customFoodUi.value = CustomFoodUiState.Saving
        viewModelScope.launch {
            when (val saved = repository.saveCustomFood(draft)) {
                is SaveResult.Failure -> _customFoodUi.value = CustomFoodUiState.Error(saved.message, saved.fieldErrors)
                is SaveResult.Success -> {
                    val food = saved.value
                    val notes = saved.warnings.toMutableList()
                    var message = "Saved \"${food.name}\" to My Foods"
                    if (logToMeal != null) {
                        when (val logged = repository.logFood(
                            food, logToMeal, _selectedDate.value, 1f, ServingCalculator.defaultOption(food)
                        )) {
                            is SaveResult.Success -> message += " and added to $logToMeal"
                            is SaveResult.Failure -> notes += "It was saved, but could not be added to your diary: ${logged.message}"
                        }
                    }
                    if (notes.isNotEmpty()) message += "\n" + notes.joinToString("\n") { "Note: $it" }
                    _foodMessage.value = message
                    _customFoodUi.value = CustomFoodUiState.Saved(food)
                }
            }
        }
    }

    fun updateCustomFood(existing: FoodItem, draft: CustomFoodDraft) {
        if (_customFoodUi.value is CustomFoodUiState.Saving) return
        _customFoodUi.value = CustomFoodUiState.Saving
        viewModelScope.launch {
            when (val r = repository.updateCustomFood(existing, draft)) {
                is SaveResult.Failure -> _customFoodUi.value = CustomFoodUiState.Error(r.message, r.fieldErrors)
                is SaveResult.Success -> {
                    _foodMessage.value = "Updated \"${r.value.name}\". Past diary entries are unchanged."
                    _customFoodUi.value = CustomFoodUiState.Saved(r.value)
                }
            }
        }
    }

    fun deleteCustomFood(food: FoodItem) {
        viewModelScope.launch {
            when (val r = repository.deleteCustomFood(food)) {
                is SaveResult.Success -> _foodMessage.value = "Deleted \"${food.name}\" from My Foods. Your diary history is kept."
                is SaveResult.Failure -> _foodMessage.value = r.message
            }
        }
    }

    fun toggleFavorite(food: FoodItem) {
        viewModelScope.launch {
            val r = repository.toggleFavorite(food)
            if (r is SaveResult.Failure) _foodMessage.value = r.message
        }
    }

    /** Adds [quantity] x [option] of [food] to the diary for the selected date. */
    fun logFood(food: FoodItem, mealType: String, quantity: Float, option: ServingOption) {
        viewModelScope.launch {
            when (val r = repository.logFood(food, mealType, _selectedDate.value, quantity, option)) {
                is SaveResult.Success -> _foodMessage.value =
                    "Added ${ServingCalculator.formatQuantity(quantity)} ${option.label} of ${food.name} to $mealType"
                is SaveResult.Failure -> _foodMessage.value = r.message
            }
        }
    }

    fun updateDiaryEntryQuantity(entry: FoodDiaryEntry, newQuantity: Float) {
        viewModelScope.launch {
            val r = repository.updateDiaryEntryQuantity(entry, newQuantity)
            if (r is SaveResult.Failure) _foodMessage.value = r.message
        }
    }

    fun deleteDiaryEntry(entry: FoodDiaryEntry) {
        viewModelScope.launch {
            repository.deleteDiaryEntry(entry)
        }
    }

    fun updateDiaryEntryWeight(entry: FoodDiaryEntry, newWeightGrams: Float) {
        val perUnit = if (entry.servingGramsPerUnit > 0f) entry.servingGramsPerUnit else 1f
        updateDiaryEntryQuantity(entry, newWeightGrams / perUnit)
    }

    // Water Logging
    fun addWater(amountMl: Int) {
        viewModelScope.launch {
            repository.logWater(_selectedDate.value, amountMl)
        }
    }

    fun deleteWaterEntry(entry: WaterEntry) {
        viewModelScope.launch {
            repository.deleteWater(entry)
        }
    }

    // Weight Logging
    fun logWeight(weightKg: Float, notes: String = "") {
        viewModelScope.launch {
            repository.logWeight(_selectedDate.value, weightKg, notes)
            userProfile.value?.let { currentProfile ->
                val updatedProfile = currentProfile.copy(currentWeightKg = weightKg)
                updateProfile(updatedProfile)
            }
        }
    }

    // Workout Logging
    fun logWorkout(
        title: String,
        durationMinutes: Int,
        exercise: Exercise?,
        sets: List<WorkoutSet> = emptyList(),
        notes: String = ""
    ) {
        viewModelScope.launch {
            val weight = userProfile.value?.currentWeightKg ?: 70f
            val met = exercise?.metValue ?: 5.0f
            val burned = NutritionCalculator.calculateExerciseCalories(met, weight, durationMinutes)
            repository.logWorkout(
                title = title,
                date = _selectedDate.value,
                durationMinutes = durationMinutes,
                caloriesBurned = burned,
                notes = notes,
                sets = sets
            )
        }
    }

    // Search
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // AI Scanner
    fun scanImage(bitmap: Bitmap) {
        viewModelScope.launch {
            _scanState.value = AiScanUiState.Loading
            val profile = userProfile.value ?: UserProfile()
            try {
                val response = repository.scanMealImage(bitmap, profile)
                _scanState.value = AiScanUiState.Success(response, bitmap)
            } catch (e: com.example.data.ai.AiConsentRequiredException) {
                _scanState.value = AiScanUiState.Error(
                    message = e.message ?: "Informed consent is required for AI cloud processing.",
                    isConsentRequired = true
                )
            } catch (e: com.example.data.ai.AiBackendNotConfiguredException) {
                _scanState.value = AiScanUiState.Error(
                    message = e.message ?: "Secure AI backend is not configured.",
                    isBackendUnconfigured = true
                )
            } catch (e: Exception) {
                _scanState.value = AiScanUiState.Error(
                    message = e.message ?: "Unable to recognize food. Please try again or add foods manually."
                )
            }
        }
    }

    fun resetScanState() {
        _scanState.value = AiScanUiState.Idle
    }

    fun grantAiConsent() {
        viewModelScope.launch {
            userProfile.value?.let { currentProfile ->
                val updated = currentProfile.copy(aiCloudConsentGiven = true, localOnlyMode = false)
                repository.saveUserProfile(updated)
            }
        }
    }

    fun updatePrivacySettings(
        aiCloudConsent: Boolean,
        backendUrl: String,
        backendToken: String,
        localOnly: Boolean
    ) {
        viewModelScope.launch {
            userProfile.value?.let { currentProfile ->
                val updated = currentProfile.copy(
                    aiCloudConsentGiven = aiCloudConsent,
                    secureBackendUrl = backendUrl.trim(),
                    secureBackendToken = backendToken.trim(),
                    localOnlyMode = localOnly
                )
                repository.saveUserProfile(updated)
            }
        }
    }

    fun confirmSaveScannedMeal(mealType: String, items: List<AiScannedFoodResult>) {
        viewModelScope.launch {
            for (item in items) {
                repository.logFoodItem(
                    foodName = item.foodName,
                    mealType = mealType,
                    date = _selectedDate.value,
                    weightGrams = item.estimatedGrams,
                    servingDescription = item.servingDescription,
                    cal100g = if (item.estimatedGrams > 0) (item.calories * 100f) / item.estimatedGrams else 100f,
                    pro100g = if (item.estimatedGrams > 0) (item.protein * 100f) / item.estimatedGrams else 0f,
                    carb100g = if (item.estimatedGrams > 0) (item.carbs * 100f) / item.estimatedGrams else 0f,
                    fat100g = if (item.estimatedGrams > 0) (item.fat * 100f) / item.estimatedGrams else 0f,
                    fiber100g = if (item.estimatedGrams > 0) (item.fiber * 100f) / item.estimatedGrams else 0f,
                    isAi = !item.isDatabaseVerified,
                    confidenceNote = "${item.confidence}: ${item.uncertaintyNote}"
                )
            }
            _scanState.value = AiScanUiState.Idle
        }
    }

    // Voice Logging
    fun processVoiceText(text: String) {
        viewModelScope.launch {
            _isVoiceProcessing.value = true
            val profile = userProfile.value ?: UserProfile()
            try {
                val result = repository.parseVoiceLog(text, profile)
                _voiceParseResult.value = result
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isVoiceProcessing.value = false
            }
        }
    }

    fun dismissVoiceResult() {
        _voiceParseResult.value = null
    }

    fun confirmVoiceLog(mealType: String, items: List<AiScannedFoodResult>) {
        viewModelScope.launch {
            for (item in items) {
                repository.logFoodItem(
                    foodName = item.foodName,
                    mealType = mealType,
                    date = _selectedDate.value,
                    weightGrams = item.estimatedGrams,
                    servingDescription = item.servingDescription,
                    cal100g = if (item.estimatedGrams > 0) (item.calories * 100f) / item.estimatedGrams else 100f,
                    pro100g = if (item.estimatedGrams > 0) (item.protein * 100f) / item.estimatedGrams else 0f,
                    carb100g = if (item.estimatedGrams > 0) (item.carbs * 100f) / item.estimatedGrams else 0f,
                    fat100g = if (item.estimatedGrams > 0) (item.fat * 100f) / item.estimatedGrams else 0f,
                    fiber100g = if (item.estimatedGrams > 0) (item.fiber * 100f) / item.estimatedGrams else 0f,
                    isAi = true,
                    confidenceNote = item.confidence
                )
            }
            _voiceParseResult.value = null
        }
    }

    // AI Coach
    fun sendCoachMessage(userText: String) {
        if (userText.isBlank()) return
        val userMsg = ChatMessage(sender = "USER", text = userText)
        _coachMessages.value = _coachMessages.value + userMsg

        viewModelScope.launch {
            _isCoachThinking.value = true
            val profile = userProfile.value ?: UserProfile()
            val summary = dailySummary.value
            val response = repository.askCoach(
                userMessage = userText,
                profile = profile,
                todayCals = summary.calories,
                todayPro = summary.protein,
                todayCarbs = summary.carbs,
                todayFat = summary.fat
            )
            _coachMessages.value = _coachMessages.value + ChatMessage(sender = "COACH", text = response)
            _isCoachThinking.value = false
        }
    }

    // Diet Planner
    fun regenerateDietPlan() {
        viewModelScope.launch {
            userProfile.value?.let { profile ->
                repository.generateAndSaveDietPlan(profile, ConsistRepository.getTodayDateString())
            }
        }
    }

    fun deleteDietMealItem(id: Long) {
        viewModelScope.launch {
            repository.deleteDietMealItem(id)
        }
    }

    // Recipes
    fun saveRecipe(
        name: String,
        servings: Int,
        method: String,
        instructions: String,
        isKeralaSpecial: Boolean,
        ingredients: List<RecipeIngredient>
    ) {
        viewModelScope.launch {
            repository.saveRecipe(name, servings, method, instructions, isKeralaSpecial, ingredients)
        }
    }

    fun deleteRecipe(recipe: Recipe) {
        viewModelScope.launch {
            repository.deleteRecipe(recipe)
        }
    }

    fun logRecipeToDiary(recipe: Recipe, mealType: String) {
        viewModelScope.launch {
            repository.logFoodItem(
                foodName = recipe.name,
                mealType = mealType,
                date = _selectedDate.value,
                weightGrams = if (recipe.servings > 0) recipe.totalWeightGrams / recipe.servings else 150f,
                servingDescription = "1 serving",
                cal100g = if (recipe.totalWeightGrams > 0) (recipe.totalCalories * 100f) / recipe.totalWeightGrams else 150f,
                pro100g = if (recipe.totalWeightGrams > 0) (recipe.totalProtein * 100f) / recipe.totalWeightGrams else 10f,
                carb100g = if (recipe.totalWeightGrams > 0) (recipe.totalCarbs * 100f) / recipe.totalWeightGrams else 20f,
                fat100g = if (recipe.totalWeightGrams > 0) (recipe.totalFat * 100f) / recipe.totalWeightGrams else 5f,
                isAi = false,
                confidenceNote = "Recipe Calculation (per serving)"
            )
        }
    }
}

sealed interface CustomFoodUiState {
    object Idle : CustomFoodUiState
    object Saving : CustomFoodUiState
    data class Saved(val food: FoodItem) : CustomFoodUiState
    data class Error(val message: String, val fieldErrors: Map<String, String> = emptyMap()) : CustomFoodUiState
}

sealed interface AiScanUiState {
    object Idle : AiScanUiState
    object Loading : AiScanUiState
    data class Success(val response: AiScanResponse, val imageBitmap: Bitmap) : AiScanUiState
    data class Error(
        val message: String,
        val isBackendUnconfigured: Boolean = false,
        val isConsentRequired: Boolean = false
    ) : AiScanUiState
}
