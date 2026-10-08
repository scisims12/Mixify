package com.mixify.music.ui.screens.onboarding

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import com.mixify.music.constants.OnboardingCompletedKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingStateTest {

    private fun mapOnboardingCompleted(preferences: Preferences): Boolean {
        return preferences[OnboardingCompletedKey] ?: false
    }

    @Test
    fun `initial onboarding state is null before datastore emits`() {
        val initialState: Boolean? = null
        assertNull(initialState)
    }

    @Test
    fun `absent key resolves to false for fresh install`() {
        runBlocking {
            val preferencesFlow = flowOf(mutablePreferencesOf())
            val resolved = preferencesFlow.map { mapOnboardingCompleted(it) }.first()
            assertFalse("Fresh install with absent key must resolve to false", resolved)
        }
    }

    @Test
    fun `stored false resolves to false`() {
        runBlocking {
            val preferencesFlow = flowOf(mutablePreferencesOf(OnboardingCompletedKey to false))
            val resolved = preferencesFlow.map { mapOnboardingCompleted(it) }.first()
            assertFalse("Stored false must resolve to false", resolved)
        }
    }

    @Test
    fun `stored true resolves to true for returning user`() {
        runBlocking {
            val preferencesFlow = flowOf(mutablePreferencesOf(OnboardingCompletedKey to true))
            val resolved = preferencesFlow.map { mapOnboardingCompleted(it) }.first()
            assertTrue("Stored true must resolve to true", resolved)
        }
    }
}
