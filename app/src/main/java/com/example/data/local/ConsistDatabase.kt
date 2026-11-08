package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE user_profile ADD COLUMN aiCloudConsentGiven INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE user_profile ADD COLUMN secureBackendUrl TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE user_profile ADD COLUMN secureBackendToken TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE user_profile ADD COLUMN localOnlyMode INTEGER NOT NULL DEFAULT 1")
    }
}

/**
 * v2 -> v3: custom foods, stable food ids, and quantity/unit on diary entries.
 * Purely additive (ALTER TABLE ADD COLUMN + backfill + index). No table is dropped or rewritten,
 * and no existing row is deleted or has its nutrition values changed.
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // --- food_database ---
        db.execSQL("ALTER TABLE food_database ADD COLUMN stableId TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE food_database ADD COLUMN isCustom INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE food_database ADD COLUMN ownerUserId INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE food_database ADD COLUMN servingUnitType TEXT NOT NULL DEFAULT 'g'")
        db.execSQL("ALTER TABLE food_database ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE food_database ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
        db.execSQL("UPDATE food_database SET isCustom = 1 WHERE category = 'Custom' OR source = 'User Custom'")
        db.execSQL(
            "UPDATE food_database SET stableId = (CASE WHEN isCustom = 1 THEN 'custom-' ELSE 'legacy-' END) || id " +
                "WHERE stableId = ''"
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_food_database_stableId` ON `food_database` (`stableId`)")

        // --- food_diary ---
        db.execSQL("ALTER TABLE food_diary ADD COLUMN foodStableId TEXT")
        db.execSQL("ALTER TABLE food_diary ADD COLUMN quantity REAL NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE food_diary ADD COLUMN servingUnit TEXT NOT NULL DEFAULT 'g'")
        db.execSQL("ALTER TABLE food_diary ADD COLUMN servingGramsPerUnit REAL NOT NULL DEFAULT 1")
        // Existing entries were logged by weight: represent them as "<weight> g" so they stay editable.
        db.execSQL("UPDATE food_diary SET quantity = weightGrams WHERE weightGrams > 0")
        db.execSQL(
            "UPDATE food_diary SET foodStableId = " +
                "(SELECT f.stableId FROM food_database f WHERE f.id = food_diary.foodId) WHERE foodId IS NOT NULL"
        )
    }
}

@Database(
    entities = [
        UserProfile::class,
        FoodItem::class,
        FoodDiaryEntry::class,
        WaterEntry::class,
        WeightEntry::class,
        Exercise::class,
        WorkoutSession::class,
        WorkoutSet::class,
        Recipe::class,
        RecipeIngredient::class,
        DietPlan::class,
        DietMealItem::class
    ],
    version = 3,
    exportSchema = false
)
abstract class ConsistDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun foodDao(): FoodDao
    abstract fun diaryDao(): DiaryDao
    abstract fun waterDao(): WaterDao
    abstract fun weightDao(): WeightDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun recipeDao(): RecipeDao
    abstract fun dietPlanDao(): DietPlanDao

    companion object {
        @Volatile
        private var INSTANCE: ConsistDatabase? = null

        fun getDatabase(context: Context): ConsistDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ConsistDatabase::class.java,
                    "consist_fitness_database"
                )
                    // No fallbackToDestructiveMigration: a missing migration must fail loudly in testing,
                    // never silently wipe a user's diary, custom foods or workout history.
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            INSTANCE?.let { database ->
                                CoroutineScope(Dispatchers.IO).launch {
                                    database.foodDao().insertFoods(PreloadedFoodData.stableItems)
                                    database.workoutDao().insertExercises(PreloadedExerciseData.items)
                                }
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
