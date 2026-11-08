package com.example.data.ai

import android.graphics.Bitmap
import android.util.Base64
import com.example.data.calculation.NutritionCalculator
import com.example.data.local.PreloadedFoodData
import com.example.data.model.DietMealItem
import com.example.data.model.DietPlan
import com.example.data.model.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

open class AiServiceException(message: String) : Exception(message)
class AiBackendNotConfiguredException(message: String) : AiServiceException(message)
class AiConsentRequiredException(message: String) : AiServiceException(message)
class FoodRecognitionException(message: String) : AiServiceException(message)

data class AiScannedFoodResult(
    val foodName: String,
    val estimatedGrams: Float,
    val servingDescription: String,
    val calories: Float,
    val protein: Float,
    val carbs: Float,
    val fat: Float,
    val fiber: Float = 0f,
    val confidence: String = "Estimated (AI Scan)",
    val uncertaintyNote: String = "Portion size is estimated from visual scale. Adjust weight as needed.",
    val isDatabaseVerified: Boolean = false
)

data class AiScanResponse(
    val detectedItems: List<AiScannedFoodResult>,
    val mealOverview: String,
    val totalCalories: Float,
    val totalProtein: Float,
    val totalCarbs: Float,
    val totalFat: Float,
    val isAiGenerated: Boolean = true
)

data class VoiceParseResult(
    val mealType: String, // "Breakfast", "Lunch", "Dinner", "Snack"
    val items: List<AiScannedFoodResult>,
    val originalText: String
)

object ConsistAiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build()

    // Request size constraint (Max 3MB for image payload)
    private const val MAX_IMAGE_BYTES = 3 * 1024 * 1024

    /**
     * Validates that the backend URL exists and enforces HTTPS for privacy.
     */
    fun validateBackendUrl(url: String): String {
        val trimmed = url.trim()
        if (trimmed.isBlank()) {
            throw AiBackendNotConfiguredException(
                "Secure AI backend is not configured. Cloud AI features require an authenticated HTTPS backend to protect API credentials. Please configure your backend in Settings > Privacy, or use manual logging."
            )
        }
        if (trimmed.startsWith("http://", ignoreCase = true)) {
            throw SecurityException("Insecure HTTP connections are prohibited for data privacy. You must use HTTPS (e.g. https://your-server.com).")
        }
        if (!trimmed.startsWith("https://", ignoreCase = true)) {
            throw SecurityException("Invalid URL: Backend URL must start with https://")
        }
        return trimmed.removeSuffix("/")
    }

    /**
     * AI Food Scanner communicating with secure authenticated backend.
     * ZERO fake sample meals on failure.
     */
    suspend fun scanMealImage(
        bitmap: Bitmap,
        profile: UserProfile
    ): AiScanResponse = withContext(Dispatchers.IO) {
        // 1. Check user consent
        if (!profile.aiCloudConsentGiven) {
            throw AiConsentRequiredException(
                "Informed consent required: Before sending meal photos to a remote AI service, please enable AI Cloud Processing in Settings > Privacy."
            )
        }

        // 2. Validate HTTPS backend configuration
        val backendUrl = validateBackendUrl(profile.secureBackendUrl)

        // 3. Compress & check payload size
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        val imageBytes = outputStream.toByteArray()

        if (imageBytes.size > MAX_IMAGE_BYTES) {
            throw IllegalArgumentException("Image is too large (${imageBytes.size / 1024} KB). Please capture a photo under 3MB.")
        }

        val base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP)

        // 4. Construct request payload
        val requestJson = JSONObject().apply {
            put("image", base64Image)
            put("clientTimestamp", System.currentTimeMillis())
        }

        val requestBuilder = Request.Builder()
            .url("$backendUrl/api/v1/scan-meal")
            .post(requestJson.toString().toRequestBody("application/json".toMediaType()))

        if (profile.secureBackendToken.isNotBlank()) {
            requestBuilder.addHeader("Authorization", "Bearer ${profile.secureBackendToken.trim()}")
        }

        val response = try {
            client.newCall(requestBuilder.build()).execute()
        } catch (e: Exception) {
            throw AiServiceException("Failed to connect to secure AI backend: ${e.message}")
        }

        val responseBody = response.body?.string() ?: ""

        if (!response.isSuccessful || responseBody.isBlank()) {
            val errorMsg = try {
                JSONObject(responseBody).optString("error", "Server returned status ${response.code}")
            } catch (e: Exception) {
                "Server returned status ${response.code}"
            }
            throw FoodRecognitionException("AI recognition service error: $errorMsg")
        }

        parseBackendScanResponse(responseBody)
    }

    private fun parseBackendScanResponse(jsonString: String): AiScanResponse {
        try {
            val data = JSONObject(jsonString)
            val overview = data.optString("mealOverview", "Recognized Meal")
            val itemsArray = data.optJSONArray("items") ?: JSONArray()

            if (itemsArray.length() == 0) {
                throw FoodRecognitionException(
                    "No food items could be identified with sufficient confidence in this photograph. Please ensure the plate is in good lighting, or add items manually using the food database."
                )
            }

            val results = mutableListOf<AiScannedFoodResult>()
            var sumCalories = 0f
            var sumProtein = 0f
            var sumCarbs = 0f
            var sumFat = 0f

            for (i in 0 until itemsArray.length()) {
                val item = itemsArray.getJSONObject(i)
                val detectedName = item.optString("foodName", "").trim()
                if (detectedName.isBlank()) continue

                val estimatedGrams = item.optDouble("estimatedGrams", 100.0).toFloat().coerceAtLeast(1f)
                val servingDesc = item.optString("servingDescription", "${estimatedGrams.toInt()}g")

                // Step 4 of Prompt: Retrieve nutritional data from verified food database where possible
                val matchedVerifiedFood = NutritionCalculator.findVerifiedFoodMatch(detectedName, PreloadedFoodData.items)

                val cal100g: Float
                val pro100g: Float
                val carb100g: Float
                val fat100g: Float
                val fib100g: Float
                val confidenceLabel: String
                val uncertaintyNote: String
                val isVerified: Boolean

                if (matchedVerifiedFood != null) {
                    // Use authoritative verified IFCT / USDA data
                    cal100g = matchedVerifiedFood.caloriesPer100g
                    pro100g = matchedVerifiedFood.proteinPer100g
                    carb100g = matchedVerifiedFood.carbsPer100g
                    fat100g = matchedVerifiedFood.fatPer100g
                    fib100g = matchedVerifiedFood.fiberPer100g
                    confidenceLabel = "Verified Database (${matchedVerifiedFood.source})"
                    uncertaintyNote = "Visual scale estimate: ${estimatedGrams.toInt()}g. Confirm portion before saving."
                    isVerified = true
                } else {
                    // Fallback to estimated values returned by AI, labeled explicitly as an estimate
                    cal100g = item.optDouble("caloriesPer100g", 120.0).toFloat()
                    pro100g = item.optDouble("proteinPer100g", 4.0).toFloat()
                    carb100g = item.optDouble("carbsPer100g", 20.0).toFloat()
                    fat100g = item.optDouble("fatPer100g", 2.0).toFloat()
                    fib100g = item.optDouble("fiberPer100g", 1.0).toFloat()
                    confidenceLabel = "AI Estimate (Unverified in Database)"
                    uncertaintyNote = "Portion and nutrients estimated from photo. Verify ingredients before saving."
                    isVerified = false
                }

                // Deterministic Calculation: (valuePer100g * grams) / 100.0
                val cals = NutritionCalculator.calculateNutrient(cal100g, estimatedGrams)
                val p = NutritionCalculator.calculateNutrient(pro100g, estimatedGrams)
                val c = NutritionCalculator.calculateNutrient(carb100g, estimatedGrams)
                val f = NutritionCalculator.calculateNutrient(fat100g, estimatedGrams)
                val fib = NutritionCalculator.calculateNutrient(fib100g, estimatedGrams)

                sumCalories += cals
                sumProtein += p
                sumCarbs += c
                sumFat += f

                results.add(
                    AiScannedFoodResult(
                        foodName = if (matchedVerifiedFood != null) matchedVerifiedFood.name else detectedName,
                        estimatedGrams = estimatedGrams,
                        servingDescription = servingDesc,
                        calories = cals,
                        protein = p,
                        carbs = c,
                        fat = f,
                        fiber = fib,
                        confidence = confidenceLabel,
                        uncertaintyNote = uncertaintyNote,
                        isDatabaseVerified = isVerified
                    )
                )
            }

            if (results.isEmpty()) {
                throw FoodRecognitionException("Could not identify recognizable food in this image. Please log manually.")
            }

            return AiScanResponse(
                detectedItems = results,
                mealOverview = overview,
                totalCalories = sumCalories,
                totalProtein = sumProtein,
                totalCarbs = sumCarbs,
                totalFat = sumFat,
                isAiGenerated = true
            )
        } catch (e: FoodRecognitionException) {
            throw e
        } catch (e: Exception) {
            throw FoodRecognitionException("Failed to parse recognition results from server: ${e.message}")
        }
    }

    /**
     * Voice Food Logging Natural Language Parser.
     * Uses deterministic keyword parsing offline, or remote backend when configured & consented.
     */
    suspend fun parseVoiceLog(
        speechText: String,
        profile: UserProfile
    ): VoiceParseResult = withContext(Dispatchers.IO) {
        val lower = speechText.lowercase()
        var detectedMeal = "Lunch"
        if (lower.contains("breakfast") || lower.contains("morning")) detectedMeal = "Breakfast"
        else if (lower.contains("dinner") || lower.contains("night")) detectedMeal = "Dinner"
        else if (lower.contains("snack")) detectedMeal = "Evening Snack"

        // If local-only mode or backend unconfigured, perform deterministic offline NLP parsing
        if (profile.localOnlyMode || profile.secureBackendUrl.isBlank() || !profile.aiCloudConsentGiven) {
            return@withContext parseVoiceDeterministicOffline(speechText, detectedMeal)
        }

        try {
            val backendUrl = validateBackendUrl(profile.secureBackendUrl)
            val requestJson = JSONObject().apply {
                put("speechText", speechText)
                put("mealType", detectedMeal)
            }

            val requestBuilder = Request.Builder()
                .url("$backendUrl/api/v1/voice-parse")
                .post(requestJson.toString().toRequestBody("application/json".toMediaType()))

            if (profile.secureBackendToken.isNotBlank()) {
                requestBuilder.addHeader("Authorization", "Bearer ${profile.secureBackendToken.trim()}")
            }

            val response = client.newCall(requestBuilder.build()).execute()
            val body = response.body?.string() ?: ""

            if (!response.isSuccessful || body.isBlank()) {
                return@withContext parseVoiceDeterministicOffline(speechText, detectedMeal)
            }

            val parsedObj = JSONObject(body)
            val parsedMealType = parsedObj.optString("mealType", detectedMeal)
            val itemsArr = parsedObj.optJSONArray("items") ?: JSONArray()
            val itemsList = mutableListOf<AiScannedFoodResult>()

            for (i in 0 until itemsArr.length()) {
                val obj = itemsArr.getJSONObject(i)
                val rawName = obj.optString("foodName", "").trim()
                val grams = obj.optDouble("estimatedGrams", 100.0).toFloat()
                val serv = obj.optString("servingDescription", "${grams.toInt()}g")

                val matched = NutritionCalculator.findVerifiedFoodMatch(rawName, PreloadedFoodData.items)
                val cal100 = matched?.caloriesPer100g ?: obj.optDouble("caloriesPer100g", 120.0).toFloat()
                val pro100 = matched?.proteinPer100g ?: obj.optDouble("proteinPer100g", 4.0).toFloat()
                val carb100 = matched?.carbsPer100g ?: obj.optDouble("carbsPer100g", 20.0).toFloat()
                val fat100 = matched?.fatPer100g ?: obj.optDouble("fatPer100g", 2.0).toFloat()
                val fib100 = matched?.fiberPer100g ?: 0f

                itemsList.add(
                    AiScannedFoodResult(
                        foodName = matched?.name ?: rawName,
                        estimatedGrams = grams,
                        servingDescription = serv,
                        calories = NutritionCalculator.calculateNutrient(cal100, grams),
                        protein = NutritionCalculator.calculateNutrient(pro100, grams),
                        carbs = NutritionCalculator.calculateNutrient(carb100, grams),
                        fat = NutritionCalculator.calculateNutrient(fat100, grams),
                        fiber = NutritionCalculator.calculateNutrient(fib100, grams),
                        confidence = if (matched != null) "Verified Database (${matched.source})" else "Speech Parsed (Estimate)",
                        uncertaintyNote = "Quantities parsed from speech. Please confirm before saving.",
                        isDatabaseVerified = matched != null
                    )
                )
            }

            if (itemsList.isEmpty()) {
                parseVoiceDeterministicOffline(speechText, detectedMeal)
            } else {
                VoiceParseResult(mealType = parsedMealType, items = itemsList, originalText = speechText)
            }
        } catch (e: Exception) {
            parseVoiceDeterministicOffline(speechText, detectedMeal)
        }
    }

    private fun parseVoiceDeterministicOffline(speechText: String, mealType: String): VoiceParseResult {
        val lower = speechText.lowercase()
        val items = mutableListOf<AiScannedFoodResult>()

        val matches = listOf(
            Triple("idli", 40f, 132f to 4.5f),
            Triple("dosa", 75f, 168f to 3.9f),
            Triple("sambar", 180f, 68f to 3.2f),
            Triple("egg", 50f, 155f to 12.6f),
            Triple("chicken", 120f, 165f to 31.0f),
            Triple("rice", 150f, 130f to 2.7f),
            Triple("oats", 40f, 389f to 16.9f),
            Triple("paneer", 100f, 296f to 18.3f),
            Triple("curd", 150f, 61f to 3.5f),
            Triple("appam", 55f, 142f to 2.4f),
            Triple("puttu", 70f, 175f to 3.2f)
        )

        for ((keyword, baseGrams, calsPro) in matches) {
            if (lower.contains(keyword)) {
                val count = when {
                    lower.contains("one") || lower.contains(" 1 ") -> 1
                    lower.contains("two") || lower.contains(" 2 ") -> 2
                    lower.contains("three") || lower.contains(" 3 ") -> 3
                    lower.contains("four") || lower.contains(" 4 ") -> 4
                    else -> 1
                }
                val totalGrams = baseGrams * count
                val matched = NutritionCalculator.findVerifiedFoodMatch(keyword, PreloadedFoodData.items)

                val cal100 = matched?.caloriesPer100g ?: calsPro.first
                val pro100 = matched?.proteinPer100g ?: calsPro.second
                val carb100 = matched?.carbsPer100g ?: 20f
                val fat100 = matched?.fatPer100g ?: 2f

                items.add(
                    AiScannedFoodResult(
                        foodName = matched?.name ?: keyword.replaceFirstChar { it.uppercase() },
                        estimatedGrams = totalGrams,
                        servingDescription = "$count serving ($totalGrams g)",
                        calories = NutritionCalculator.calculateNutrient(cal100, totalGrams),
                        protein = NutritionCalculator.calculateNutrient(pro100, totalGrams),
                        carbs = NutritionCalculator.calculateNutrient(carb100, totalGrams),
                        fat = NutritionCalculator.calculateNutrient(fat100, totalGrams),
                        confidence = if (matched != null) "Verified IFCT Database" else "Offline Parser",
                        uncertaintyNote = "Quantities parsed from speech. Please confirm before saving.",
                        isDatabaseVerified = matched != null
                    )
                )
            }
        }

        if (items.isEmpty()) {
            items.add(
                AiScannedFoodResult(
                    foodName = speechText.take(40).trim(),
                    estimatedGrams = 100f,
                    servingDescription = "100g (Unverified)",
                    calories = 120f,
                    protein = 4f,
                    carbs = 20f,
                    fat = 2f,
                    confidence = "Speech Text (Unverified)",
                    uncertaintyNote = "Not found in database. Please edit weights and nutrients before saving.",
                    isDatabaseVerified = false
                )
            )
        }

        return VoiceParseResult(mealType = mealType, items = items, originalText = speechText)
    }

    /**
     * AI Fitness & Nutrition Conversational Coach.
     * Respects user privacy, never transmits passwords/tokens.
     * Uses offline evidence-based guidance if local-only mode is active or backend unconfigured.
     */
    suspend fun askCoach(
        userMessage: String,
        profile: UserProfile,
        todayCalories: Float,
        todayProtein: Float,
        todayCarbs: Float,
        todayFat: Float
    ): String = withContext(Dispatchers.IO) {
        // If local mode or unconfigured, provide local evidence-based guidance
        if (profile.localOnlyMode || profile.secureBackendUrl.isBlank() || !profile.aiCloudConsentGiven) {
            return@withContext getEvidenceBasedLocalCoachAdvice(userMessage, profile, todayCalories, todayProtein)
        }

        try {
            val backendUrl = validateBackendUrl(profile.secureBackendUrl)

            // Minimized, privacy-preserving payload: Only essential fitness context
            val minimalPayload = JSONObject().apply {
                put("message", userMessage.take(500))
                put("age", profile.age)
                put("goal", profile.primaryGoal)
                put("dietaryPreference", profile.dietaryPreference)
                put("targetCalories", profile.targetCalories)
                put("targetProteinGrams", profile.targetProteinGrams)
                put("consumedCaloriesToday", todayCalories.toInt())
                put("consumedProteinToday", todayProtein.toInt())
            }

            val requestBuilder = Request.Builder()
                .url("$backendUrl/api/v1/coach-chat")
                .post(minimalPayload.toString().toRequestBody("application/json".toMediaType()))

            if (profile.secureBackendToken.isNotBlank()) {
                requestBuilder.addHeader("Authorization", "Bearer ${profile.secureBackendToken.trim()}")
            }

            val response = client.newCall(requestBuilder.build()).execute()
            val body = response.body?.string() ?: ""

            if (!response.isSuccessful || body.isBlank()) {
                return@withContext getEvidenceBasedLocalCoachAdvice(userMessage, profile, todayCalories, todayProtein)
            }

            val root = JSONObject(body)
            root.optString("reply", getEvidenceBasedLocalCoachAdvice(userMessage, profile, todayCalories, todayProtein))
        } catch (e: Exception) {
            getEvidenceBasedLocalCoachAdvice(userMessage, profile, todayCalories, todayProtein)
        }
    }

    private fun getEvidenceBasedLocalCoachAdvice(
        query: String,
        profile: UserProfile,
        cals: Float,
        pro: Float
    ): String {
        val q = query.lowercase()
        val remainingCals = (profile.targetCalories - cals).toInt()
        val remainingPro = (profile.targetProteinGrams - pro).toInt()
        val prefix = if (profile.localOnlyMode || profile.secureBackendUrl.isBlank()) {
            "🔒 [OFFLINE LOCAL COACH]: "
        } else ""

        return prefix + when {
            q.contains("protein") || q.contains("macro") -> {
                "Your daily protein target is ${profile.targetProteinGrams.toInt()}g (2.0g/kg bodyweight). You have logged ${pro.toInt()}g today ($remainingPro g remaining). High-protein options for ${profile.dietaryPreference}: ${
                    if (profile.dietaryPreference == "Vegan") "Soy chunks (52g/100g), Firm tofu (8g/100g), Moong dal, and Plant protein."
                    else if (profile.dietaryPreference == "Vegetarian") "Paneer (18g/100g), Greek yogurt, Boiled eggs (if included), and Whey protein."
                    else "Chicken breast (31g/100g), Kudampuli fish curry, Boiled eggs, Paneer, and Whey protein."
                }"
            }
            q.contains("calorie") || q.contains("target") || q.contains("today") -> {
                "You have consumed ${cals.toInt()} kcal out of your ${profile.targetCalories} kcal target today (${remainingCals.coerceAtLeast(0)} kcal remaining). Energy balance is calculated via Mifflin-St Jeor equation to sustainably support ${profile.primaryGoal}."
            }
            q.contains("kerala") || q.contains("south indian") -> {
                "Kerala staples like Puttu with Kadala curry or Idli with Sambar offer fantastic complex carbohydrates and fiber. To meet your protein target, pair them with 2 boiled egg whites, green gram (cherupayar), or a whey protein shake."
            }
            q.contains("workout") || q.contains("exercise") || q.contains("chest") || q.contains("leg") -> {
                "Focus on progressive overload on compound lifts (Squats, Bench Press, Romanian Deadlifts, Pull-ups). Aim to add 1 rep or small weight increments weekly with strict form."
            }
            else -> {
                "Consistency is key! You have ${remainingCals.coerceAtLeast(0)} kcal and ${remainingPro.coerceAtLeast(0)}g protein remaining for your daily ${profile.primaryGoal} target. Drink water, prioritize 7-8 hours of sleep, and BUILD. TRAIN. REPEAT."
            }
        }
    }

    /**
     * Personalized AI Diet Plan Generator (Deterministic, Verified).
     */
    fun generateDeterministicDietPlan(profile: UserProfile, dateStr: String): Pair<DietPlan, List<DietMealItem>> {
        val isVeg = profile.dietaryPreference.equals("Vegetarian", ignoreCase = true)
        val isVegan = profile.dietaryPreference.equals("Vegan", ignoreCase = true)

        val plan = DietPlan(
            title = "${profile.primaryGoal} Precision Plan (${profile.dietaryPreference})",
            planType = "Daily",
            targetGoal = profile.primaryGoal,
            dietaryType = profile.dietaryPreference,
            totalCalories = profile.targetCalories.toFloat(),
            totalProtein = profile.targetProteinGrams,
            totalCarbs = profile.targetCarbsGrams,
            totalFat = profile.targetFatGrams,
            isBudgetFriendly = profile.foodBudget.equals("Low", ignoreCase = true) || profile.foodBudget.equals("Medium", ignoreCase = true),
            shoppingListJson = if (isVegan) "Soy Chunks, Tofu, Oats, Bananas, Moong Dal, Green Gram, Mixed Nuts" else if (isVeg) "Paneer, Eggs, Curd, Oats, Idli Podi, Sambar Veggies, Whey Protein" else "Chicken Breast, Eggs, Matta Rice, Oats, Fish, Veggies, Peanut Butter",
            isActive = true,
            createdDate = dateStr
        )

        val items = mutableListOf<DietMealItem>()

        // Breakfast
        if (isVegan) {
            items.add(DietMealItem(planId = 0, mealName = "Breakfast", foodItemName = "Rolled Oats with Soy Milk & Banana", portionDescription = "60g oats + 1 banana", weightGrams = 180f, calories = 320f, protein = 12f, carbs = 58f, fat = 5f, prepInstructions = "Cook oats in plant milk, slice banana on top."))
            items.add(DietMealItem(planId = 0, mealName = "Breakfast", foodItemName = "Steamed Idli with Sambar", portionDescription = "2 idlis + 1 cup sambar", weightGrams = 220f, calories = 200f, protein = 8f, carbs = 38f, fat = 2f, prepInstructions = "Fresh steamed idlis with homemade vegetable sambar."))
        } else if (isVeg && profile.eatsEggs) {
            items.add(DietMealItem(planId = 0, mealName = "Breakfast", foodItemName = "Whole Boiled Eggs", portionDescription = "2 whole eggs", weightGrams = 100f, calories = 155f, protein = 12.6f, carbs = 1.1f, fat = 10.6f, prepInstructions = "Hard boil for 9 minutes in salted water."))
            items.add(DietMealItem(planId = 0, mealName = "Breakfast", foodItemName = "Kerala Puttu with Kadala Curry", portionDescription = "1 piece puttu + 1 bowl kadala", weightGrams = 250f, calories = 313f, protein = 10.6f, carbs = 53.8f, fat = 6.2f, prepInstructions = "Steamed puttu layered with fresh coconut."))
        } else {
            items.add(DietMealItem(planId = 0, mealName = "Breakfast", foodItemName = "Whole Eggs & Egg Whites Scramble", portionDescription = "2 whole + 2 whites", weightGrams = 160f, calories = 207f, protein = 23.5f, carbs = 1.8f, fat = 10.8f, prepInstructions = "Lightly scramble in 1 tsp olive oil."))
            items.add(DietMealItem(planId = 0, mealName = "Breakfast", foodItemName = "Rolled Oats with Peanut Butter", portionDescription = "50g oats + 1 tbsp peanut butter", weightGrams = 65f, calories = 278f, protein = 12.5f, carbs = 36f, fat = 10f, prepInstructions = "Cook with water or milk, stir in peanut butter."))
        }

        // Lunch
        if (isVegan) {
            items.add(DietMealItem(planId = 0, mealName = "Lunch", foodItemName = "High Protein Soy Chunks Curry", portionDescription = "1 bowl (60g dry weight)", weightGrams = 180f, calories = 250f, protein = 31f, carbs = 20f, fat = 3f, prepInstructions = "Boil soy chunks, squeeze water, simmer in onion-tomato masala."))
            items.add(DietMealItem(planId = 0, mealName = "Lunch", foodItemName = "Kerala Matta Rice", portionDescription = "1.5 cups cooked", weightGrams = 200f, calories = 222f, protein = 5.2f, carbs = 47f, fat = 1.8f, prepInstructions = "Boil red matta rice until tender."))
            items.add(DietMealItem(planId = 0, mealName = "Lunch", foodItemName = "Cabbage Thoran", portionDescription = "1 bowl", weightGrams = 100f, calories = 78f, protein = 1.9f, carbs = 6.8f, fat = 4.8f, prepInstructions = "Quick stir fry with mustard seeds, curry leaves, and grated coconut."))
        } else if (isVeg) {
            items.add(DietMealItem(planId = 0, mealName = "Lunch", foodItemName = "Grilled Paneer Tikka / Cubes", portionDescription = "120g fresh paneer", weightGrams = 120f, calories = 355f, protein = 22f, carbs = 4.1f, fat = 27.6f, prepInstructions = "Marinate with curd, turmeric, chili powder and pan sear."))
            items.add(DietMealItem(planId = 0, mealName = "Lunch", foodItemName = "Moong Dal Tadka", portionDescription = "1 bowl", weightGrams = 180f, calories = 169f, protein = 11.2f, carbs = 25.4f, fat = 2.7f, prepInstructions = "Cook yellow dal with garlic, cumin, and green chilies."))
            items.add(DietMealItem(planId = 0, mealName = "Lunch", foodItemName = "Whole Wheat Chapati", portionDescription = "2 chapatis", weightGrams = 80f, calories = 211f, protein = 7.4f, carbs = 41.2f, fat = 2.2f, prepInstructions = "Freshly rolled whole wheat flatbread."))
        } else {
            items.add(DietMealItem(planId = 0, mealName = "Lunch", foodItemName = "Kerala Style Chicken Curry / Grilled", portionDescription = "150g chicken breast", weightGrams = 150f, calories = 248f, protein = 38f, carbs = 2f, fat = 8f, prepInstructions = "Simmer chicken breast in ginger-garlic and spiced gravy."))
            items.add(DietMealItem(planId = 0, mealName = "Lunch", foodItemName = "Boiled Kerala Matta Rice", portionDescription = "1.5 cups cooked", weightGrams = 200f, calories = 222f, protein = 5.2f, carbs = 47f, fat = 1.8f, prepInstructions = "Steamed red rice."))
            items.add(DietMealItem(planId = 0, mealName = "Lunch", foodItemName = "Avial (Mixed Vegetables with Coconut)", portionDescription = "1 bowl", weightGrams = 150f, calories = 142f, protein = 3.3f, carbs = 16.2f, fat = 7.8f, prepInstructions = "Traditional Kerala dish of drumsticks, carrots, beans, raw banana."))
        }

        // Evening Snack
        items.add(DietMealItem(planId = 0, mealName = "Evening Snack", foodItemName = "Whey / Plant Protein Shake with Water", portionDescription = "1 scoop (30g)", weightGrams = 30f, calories = 111f, protein = 26f, carbs = 1f, fat = 0.5f, prepInstructions = "Shake with 250ml chilled water."))
        items.add(DietMealItem(planId = 0, mealName = "Evening Snack", foodItemName = "Handful of Raw Almonds", portionDescription = "15 almonds (~20g)", weightGrams = 20f, calories = 116f, protein = 4.2f, carbs = 4.3f, fat = 10f, prepInstructions = "Crunchy whole raw almonds."))

        // Dinner
        if (isVegan) {
            items.add(DietMealItem(planId = 0, mealName = "Dinner", foodItemName = "Stir Fried Firm Tofu & Green Veggies", portionDescription = "150g tofu + broccoli", weightGrams = 250f, calories = 190f, protein = 16f, carbs = 10f, fat = 9f, prepInstructions = "Sauté in non-stick pan with light soy sauce and black pepper."))
            items.add(DietMealItem(planId = 0, mealName = "Dinner", foodItemName = "Green Gram (Cherupayar) Stew", portionDescription = "1 bowl", weightGrams = 170f, calories = 178f, protein = 11.9f, carbs = 28f, fat = 2.4f, prepInstructions = "Pressure cook whole moong with mild spices."))
        } else if (isVeg) {
            items.add(DietMealItem(planId = 0, mealName = "Dinner", foodItemName = "Spiced Paneer Bhurji with 2 Chapatis", portionDescription = "100g paneer + 2 chapatis", weightGrams = 180f, calories = 430f, protein = 22f, carbs = 43f, fat = 20f, prepInstructions = "Crumble paneer with onions, tomatoes and fresh coriander."))
            items.add(DietMealItem(planId = 0, mealName = "Dinner", foodItemName = "Fresh Cucumber Salad", portionDescription = "1 plate", weightGrams = 100f, calories = 15f, protein = 0.7f, carbs = 3.6f, fat = 0.1f, prepInstructions = "Sliced fresh cucumber with lemon juice."))
        } else {
            items.add(DietMealItem(planId = 0, mealName = "Dinner", foodItemName = "Kerala Fish Curry / Shallow Grilled Fish", portionDescription = "150g fish fillet", weightGrams = 150f, calories = 210f, protein = 28f, carbs = 1f, fat = 10f, prepInstructions = "Kudampuli meen curry or pan grilled with minimal oil."))
            items.add(DietMealItem(planId = 0, mealName = "Dinner", foodItemName = "Whole Wheat Phulka / Chapati", portionDescription = "2 pieces", weightGrams = 80f, calories = 211f, protein = 7.4f, carbs = 41.2f, fat = 2.2f, prepInstructions = "Soft roasted phulkas."))
            items.add(DietMealItem(planId = 0, mealName = "Dinner", foodItemName = "Steamed Broccoli & Carrot", portionDescription = "1 cup", weightGrams = 120f, calories = 45f, protein = 2.5f, carbs = 8f, fat = 0.5f, prepInstructions = "Steamed with a pinch of sea salt."))
        }

        return Pair(plan, items)
    }
}
