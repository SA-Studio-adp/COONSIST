package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.calculation.CustomFoodDraft
import com.example.data.calculation.ServingCalculator
import com.example.data.local.ConsistDatabase
import com.example.data.repository.ConsistRepository
import com.example.data.repository.SaveResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Runs the real Room stack (in-memory SQLite) through Robolectric. Not executed in the authoring sandbox. */
@RunWith(RobolectricTestRunner::class)
class CustomFoodRoomTest {
    private lateinit var db: ConsistDatabase
    private lateinit var repo: ConsistRepository
    private val bar = CustomFoodDraft("Protein Bar", 1f, "piece", 40f, 160f, 10f, 15f, 6f)

    @Before fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, ConsistDatabase::class.java).allowMainThreadQueries().build()
        repo = ConsistRepository(db)
    }
    @After fun tearDown() = db.close()

    @Test fun saveListSearchLogDeleteKeepsHistory() = runBlocking {
        val saved = (repo.saveCustomFood(bar) as SaveResult.Success).value
        assertTrue(saved.id > 0)
        assertEquals(1, repo.customFoods.first().size)
        assertEquals("Protein Bar", repo.searchFoods("protein").first().first().name)
        val opt = ServingCalculator.defaultOption(saved)
        repeat(2) { assertTrue(repo.logFood(saved, "Lunch", "2026-11-08", 2f, opt) is SaveResult.Success) }
        assertEquals(1, repo.recentFoods.first().size)
        assertTrue(repo.saveCustomFood(bar.copy(name = " protein  bar ")) is SaveResult.Failure) // duplicate
        assertTrue(repo.deleteCustomFood(saved) is SaveResult.Success)
        assertEquals(0, repo.customFoods.first().size)
        assertEquals(2, repo.getDiaryEntriesForDate("2026-11-08").first().size)
    }
}
