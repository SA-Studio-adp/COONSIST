package com.example

import android.graphics.Bitmap
import com.example.data.ai.AiBackendNotConfiguredException
import com.example.data.ai.AiConsentRequiredException
import com.example.data.ai.ConsistAiService
import com.example.data.ai.FoodRecognitionException
import com.example.data.model.UserProfile
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SecurityAndAiUnitTest {

    @Test
    fun testBackendUrlRejectsInsecureHttp() {
        try {
            ConsistAiService.validateBackendUrl("http://insecure-api.consist.com")
            fail("Expected SecurityException for insecure http:// connection")
        } catch (e: SecurityException) {
            assertTrue(e.message?.contains("HTTPS", ignoreCase = true) == true)
        }
    }

    @Test
    fun testBackendUrlRejectsEmptyUrl() {
        try {
            ConsistAiService.validateBackendUrl("")
            fail("Expected AiBackendNotConfiguredException for empty URL")
        } catch (e: AiBackendNotConfiguredException) {
            assertTrue(e.message?.contains("not configured", ignoreCase = true) == true)
        }
    }

    @Test
    fun testBackendUrlAcceptsValidHttps() {
        val validated = ConsistAiService.validateBackendUrl("https://api.consist-app.com/")
        assertEquals("https://api.consist-app.com", validated)
    }

    @Test
    fun testScanRequiresUserConsent() {
        val unconsentedProfile = UserProfile(
            aiCloudConsentGiven = false,
            secureBackendUrl = "https://api.consist-app.com"
        )
        val dummyBitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)

        try {
            runBlocking {
                ConsistAiService.scanMealImage(dummyBitmap, unconsentedProfile)
            }
            fail("Expected AiConsentRequiredException when user consent is false")
        } catch (e: AiConsentRequiredException) {
            assertTrue(e.message?.contains("consent", ignoreCase = true) == true)
        }
    }

    @Test
    fun testScanRequiresConfiguredBackend() {
        val consentedProfileNoBackend = UserProfile(
            aiCloudConsentGiven = true,
            secureBackendUrl = ""
        )
        val dummyBitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)

        try {
            runBlocking {
                ConsistAiService.scanMealImage(dummyBitmap, consentedProfileNoBackend)
            }
            fail("Expected AiBackendNotConfiguredException when backend URL is missing")
        } catch (e: AiBackendNotConfiguredException) {
            assertTrue(e.message?.contains("not configured", ignoreCase = true) == true)
        }
    }

    @Test
    fun testOfflineCoachUsesLocalSafeAdvice() {
        val offlineProfile = UserProfile(
            localOnlyMode = true,
            secureBackendUrl = ""
        )
        val reply = runBlocking {
            ConsistAiService.askCoach(
                userMessage = "What is my protein target?",
                profile = offlineProfile,
                todayCalories = 1200f,
                todayProtein = 80f,
                todayCarbs = 140f,
                todayFat = 40f
            )
        }
        assertNotNull(reply)
        assertTrue(reply.contains("LOCAL") || reply.contains("protein target"))
        // Verifies no remote calls or crashes occur offline
    }
}
