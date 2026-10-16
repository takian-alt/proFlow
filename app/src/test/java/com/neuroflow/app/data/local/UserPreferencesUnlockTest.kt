package com.neuroflow.app.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UserPreferencesUnlockTest {

    private val oneDayMillis = 24L * 60 * 60 * 1000L
    private val oneWeekMillis = 7L * oneDayMillis

    @Test
    fun testBrandNewInstallIsLocked() {
        val prefs = UserPreferences(
            firstAppOpenMillis = 0L,
            onboardingCompleted = false,
            dailyStreak = 0
        )
        assertFalse(prefs.isAdvancedFeaturesUnlocked())
        assertEquals(7, prefs.daysUntilAdvancedUnlock())
    }

    @Test
    fun testUpgradedExistingUserIsUnlocked() {
        val prefs = UserPreferences(
            firstAppOpenMillis = 0L,
            onboardingCompleted = true,
            dailyStreak = 3
        )
        assertTrue(prefs.isAdvancedFeaturesUnlocked())
        assertEquals(0, prefs.daysUntilAdvancedUnlock())
    }

    @Test
    fun testHighDailyStreakUnlocksImmediately() {
        val prefs = UserPreferences(
            firstAppOpenMillis = System.currentTimeMillis(),
            onboardingCompleted = true,
            dailyStreak = 7
        )
        assertTrue(prefs.isAdvancedFeaturesUnlocked())
        assertEquals(0, prefs.daysUntilAdvancedUnlock())
    }

    @Test
    fun testGradualDayProgression() {
        val now = 1_000_000_000_000L // arbitrary fixed time
        val firstOpen = now - 3 * oneDayMillis // 3 days ago

        val prefs3Days = UserPreferences(
            firstAppOpenMillis = firstOpen,
            onboardingCompleted = true,
            dailyStreak = 3
        )
        assertFalse(prefs3Days.isAdvancedFeaturesUnlocked(now))
        assertEquals(4, prefs3Days.daysUntilAdvancedUnlock(now))

        val prefsFullWeek = UserPreferences(
            firstAppOpenMillis = now - oneWeekMillis,
            onboardingCompleted = true,
            dailyStreak = 5
        )
        assertTrue(prefsFullWeek.isAdvancedFeaturesUnlocked(now))
        assertEquals(0, prefsFullWeek.daysUntilAdvancedUnlock(now))
    }
}
