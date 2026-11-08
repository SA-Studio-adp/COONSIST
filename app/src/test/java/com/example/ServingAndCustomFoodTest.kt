package com.example

import com.example.data.calculation.*
import com.example.data.local.PreloadedFoodData
import com.example.data.model.FoodDiaryEntry
import com.example.data.model.FoodItem
import org.junit.Assert.*
import org.junit.Test

class ServingAndCustomFoodTest {

    private fun food(name: String, kcal100: Float, grams: Float, label: String, unitType: String = "g") = FoodItem(
        name = name, category = "Test", defaultServingUnit = label, defaultWeightGrams = grams,
        caloriesPer100g = kcal100, proteinPer100g = 0f, carbsPer100g = 0f, fatPer100g = 0f, servingUnitType = unitType
    )

    // ---- Quantity counter ----
    @Test fun illustrativeDosa_75kcalPerDosa_fourDosa_is300() {
        val dosa = food("Dosa", 187.5f, 40f, "1 dosa (40g)")   // 75 kcal per 40 g dosa
        val opt = ServingCalculator.servingOptionFor(dosa)
        assertEquals("dosa", opt.label)
        assertEquals(75f, ServingCalculator.perServing(dosa).calories, 0.01f)
        assertEquals(300f, ServingCalculator.nutritionFor(dosa, 4f, opt).calories, 0.01f)
    }

    @Test fun realDatabaseDosa_usesStoredValues() {
        val dosa = PreloadedFoodData.items.first { it.name.startsWith("Dosa") }
        val opt = ServingCalculator.servingOptionFor(dosa)
        // 168 kcal/100 g x 75 g = 126 kcal per medium dosa
        assertEquals(126f, ServingCalculator.perServing(dosa).calories, 0.01f)
        assertEquals(504f, ServingCalculator.nutritionFor(dosa, 4f, opt).calories, 0.01f)
        assertEquals(63f, ServingCalculator.nutritionFor(dosa, 0.5f, opt).calories, 0.01f)
    }

    @Test fun servingLabelParsing() {
        assertEquals(80f, ServingCalculator.servingOptionFor(food("a", 1f, 40f, "1/2 cup (40g)")).gramsPerUnit, 0.001f)
        assertEquals("cup", ServingCalculator.servingOptionFor(food("a", 1f, 40f, "1/2 cup (40g)")).label)
        assertEquals(16f, ServingCalculator.servingOptionFor(food("a", 1f, 32f, "2 tbsp (32g)")).gramsPerUnit, 0.001f)
        val cube = ServingCalculator.servingOptionFor(food("a", 1f, 100f, "100g cube"))
        assertEquals("serving", cube.label); assertEquals(100f, cube.gramsPerUnit, 0.001f)
        assertEquals("piece with gravy", ServingCalculator.servingOptionFor(food("a", 1f, 160f, "1 piece with gravy (160g)")).label)
    }

    @Test fun everyPreloadedFoodHasUsableServing() {
        for (f in PreloadedFoodData.items) {
            val o = ServingCalculator.defaultOption(f)
            assertTrue("${f.name} grams/unit", o.gramsPerUnit > 0f)
            assertTrue("${f.name} nutrition", ServingCalculator.nutritionFor(f, 1f, o).calories >= 0f)
        }
    }

    @Test fun counterNeverGoesZeroOrNegative() {
        val o = ServingCalculator.servingOptionFor(food("a", 100f, 50f, "1 piece (50g)"))
        assertEquals(1.5f, ServingCalculator.increment(1f, o), 0f)
        assertEquals(0.5f, ServingCalculator.decrement(1f, o), 0f)
        assertEquals(0.5f, ServingCalculator.decrement(0.5f, o), 0f)   // unchanged, not 0
        val g = ServingCalculator.baseOption(food("a", 100f, 50f, "50g"))
        assertEquals(10f, ServingCalculator.decrement(10f, g), 0f)     // unchanged, not 0
        assertEquals(ServingCalculator.MAX_GRAMS, ServingCalculator.increment(ServingCalculator.MAX_GRAMS, g), 0f)
    }

    @Test fun directEntryValidation() {
        val o = ServingCalculator.servingOptionFor(food("a", 100f, 50f, "1 piece (50g)"))
        assertTrue(ServingCalculator.parseQuantity("0", o) is QuantityParse.Invalid)
        assertTrue(ServingCalculator.parseQuantity("-2", o) is QuantityParse.Invalid)
        assertTrue(ServingCalculator.parseQuantity("abc", o) is QuantityParse.Invalid)
        assertTrue(ServingCalculator.parseQuantity("", o) is QuantityParse.Invalid)
        assertTrue(ServingCalculator.parseQuantity("NaN", o) is QuantityParse.Invalid)
        assertTrue(ServingCalculator.parseQuantity("1000", o) is QuantityParse.Invalid)
        assertEquals(1.5f, (ServingCalculator.parseQuantity("1,5", o) as QuantityParse.Valid).quantity, 0f)
        assertEquals(0.25f, (ServingCalculator.parseQuantity("0.25", o) as QuantityParse.Valid).quantity, 0f)
    }

    @Test fun unitSwitchKeepsSameWeight() {
        val dosa = food("Dosa", 168f, 75f, "1 medium dosa (75g)")
        val serving = ServingCalculator.servingOptionFor(dosa)
        val grams = ServingCalculator.baseOption(dosa)
        assertEquals(150f, ServingCalculator.convertQuantity(2f, serving, grams), 0.01f)
        assertEquals(2f, ServingCalculator.convertQuantity(150f, grams, serving), 0.01f)
    }

    @Test fun mlFoodsUseMlAsBaseUnit() {
        val milk = food("Milk", 60f, 250f, "1 glass (250ml)", unitType = "ml")
        assertEquals("ml", ServingCalculator.baseOption(milk).label)
    }

    // ---- Diary rescale ----
    private fun entry(q: Float, perUnit: Float, kcal: Float) = FoodDiaryEntry(
        foodName = "Dosa", mealType = "Breakfast", date = "2026-11-07", weightGrams = q * perUnit,
        calories = kcal, protein = 10f, carbs = 20f, fat = 5f, fiber = 2f, quantity = q, servingUnit = "medium dosa", servingGramsPerUnit = perUnit
    )

    @Test fun editingDiaryQuantityRescalesFromSnapshot() {
        val e = entry(4f, 75f, 504f)
        val two = ServingCalculator.rescaleEntry(e, 2f)!!
        assertEquals(252f, two.calories, 0.01f); assertEquals(150f, two.weightGrams, 0.01f)
        assertEquals(5f, two.protein, 0.01f); assertEquals("2 medium dosa", two.servingDescription)
        assertNull(ServingCalculator.rescaleEntry(e, 0f))
        assertNull(ServingCalculator.rescaleEntry(e, -1f))
    }

    @Test fun editingFoodDefinitionDoesNotChangeHistory() {
        val original = CustomFoodRules.toFoodItem(CustomFoodDraft("My Bar", 1f, "piece", 40f, 200f, 10f, 20f, 8f))
        val loggedKcal = ServingCalculator.nutritionFor(original, 2f, ServingCalculator.servingOptionFor(original)).calories
        val logged = entry(2f, 40f, loggedKcal).copy(foodStableId = original.stableId)
        val edited = original.copy(caloriesPer100g = 9999f)   // user edits the definition later
        assertEquals(400f, logged.calories, 0.01f)             // history snapshot untouched
        assertEquals(original.stableId, edited.stableId)       // identity is stable across edits
    }

    // ---- Custom food rules ----
    private fun draft(
        name: String = "Protein Bar", q: Float = 1f, unit: String = "piece", w: Float = 40f,
        kcal: Float = 160f, p: Float = 10f, c: Float = 15f, f: Float = 6f
    ) = CustomFoodDraft(name, q, unit, w, kcal, p, c, f)

    @Test fun validDraftPasses() = assertTrue(CustomFoodRules.validate(draft()) is CustomFoodValidation.Valid)

    @Test fun blankNameRejected() {
        val r = CustomFoodRules.validate(draft(name = "   "))
        assertTrue(r is CustomFoodValidation.Invalid && r.errors.containsKey("name"))
    }

    @Test fun negativesAndZeroWeightRejected() {
        assertTrue(CustomFoodRules.validate(draft(kcal = -1f)) is CustomFoodValidation.Invalid)
        assertTrue(CustomFoodRules.validate(draft(w = 0f)) is CustomFoodValidation.Invalid)
        assertTrue(CustomFoodRules.validate(draft(q = 0f)) is CustomFoodValidation.Invalid)
        assertTrue(CustomFoodRules.validate(draft(p = -3f)) is CustomFoodValidation.Invalid)
    }

    @Test fun physicallyImpossibleValuesRejected() {
        assertTrue(CustomFoodRules.validate(draft(w = 30f, p = 20f, c = 20f, f = 20f, kcal = 500f)) is CustomFoodValidation.Invalid)
        assertTrue(CustomFoodRules.validate(draft(w = 30f, kcal = 1000f, p = 1f, c = 1f, f = 1f)) is CustomFoodValidation.Invalid)
    }

    @Test fun macroMismatchIsWarningNotError() {
        val r = CustomFoodRules.validate(draft(kcal = 300f))   // macros imply ~154 kcal; 7.5 kcal/g is still possible
        assertTrue(r is CustomFoodValidation.Valid && r.warnings.isNotEmpty())
        assertNull(CustomFoodRules.macroMismatchWarning(160f, 10f, 15f, 6f))
    }

    @Test fun savedFoodIsPer100gAndMarkedUnverifiedCustom() {
        val f = CustomFoodRules.toFoodItem(draft(kcal = 160f, w = 40f))
        assertEquals(400f, f.caloriesPer100g, 0.01f)
        assertTrue(f.isCustom); assertFalse(f.isVerified)
        assertEquals("Custom", f.category); assertEquals("User Custom", f.source)
        assertEquals(40f, f.defaultWeightGrams, 0f)
        assertEquals(160f, ServingCalculator.perServing(f).calories, 0.01f)
        assertEquals("piece", ServingCalculator.servingOptionFor(f).label)
    }

    @Test fun stableIdsAreUniquePerFoodAndKeptOnEdit() {
        val a = CustomFoodRules.toFoodItem(draft()); val b = CustomFoodRules.toFoodItem(draft())
        assertNotEquals(a.stableId, b.stableId)
        val edited = CustomFoodRules.toFoodItem(draft(kcal = 170f), stableId = a.stableId, existingId = 7L, createdAt = a.createdAt)
        assertEquals(a.stableId, edited.stableId); assertEquals(7L, edited.id)
    }

    @Test fun draftRoundTrip() {
        val back = CustomFoodRules.toDraft(CustomFoodRules.toFoodItem(draft()))
        assertEquals("Protein Bar", back.name); assertEquals(160f, back.calories, 0.01f)
        assertEquals(10f, back.protein, 0.01f); assertEquals(40f, back.servingWeightGrams, 0.01f)
        assertEquals("piece", back.servingUnit); assertEquals(1f, back.servingQuantity, 0.01f)
    }

    @Test fun gramAndMlFoodsKeepBaseUnit() {
        // A "50 g" food defaults to ONE serving (50 g), with grams available as the alternative unit.
        val g = CustomFoodRules.toFoodItem(CustomFoodDraft("Oats mix", 50f, "g", 50f, 190f, 7f, 33f, 3f))
        assertEquals("serving", ServingCalculator.defaultOption(g).label)
        assertEquals(50f, ServingCalculator.defaultOption(g).gramsPerUnit, 0f)
        assertEquals("g", ServingCalculator.optionsFor(g).last().label)
        val ml = CustomFoodRules.toFoodItem(CustomFoodDraft("Juice", 200f, "ml", 200f, 90f, 1f, 21f, 0f))
        assertEquals("ml", ServingCalculator.optionsFor(ml).last().label); assertEquals("ml", ml.servingUnitType)
        assertEquals(90f, ServingCalculator.perServing(ml).calories, 0.01f)
    }

    @Test fun nameNormalizationForDuplicateDetection() {
        assertEquals("Protein Bar", CustomFoodRules.normalizeName("  Protein   Bar "))
    }
}
