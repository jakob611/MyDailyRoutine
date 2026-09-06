package com.example.mydailyroutine.data

import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import com.example.mydailyroutine.data.preferences.HealthPreferenceCodec
import com.example.mydailyroutine.domain.health.HealthConfig
import org.junit.Assert.*
import org.junit.Test

class HealthPreferenceCodecTest {
    @Test fun defaultsArePreservedForExistingInstalls() { assertEquals(HealthConfig(), HealthPreferenceCodec.read(emptyPreferences())) }
    @Test fun allLimitsAndSwitchesRoundTrip() {
        val expected = HealthConfig(60, 150, 45, 240, 100, 30, 80, false, true, false, true, false, true)
        val values = mutablePreferencesOf()
        HealthPreferenceCodec.write(values, expected)
        assertEquals(expected, HealthPreferenceCodec.read(values))
    }
    @Test fun corruptValuesFallBackWithoutMutatingStoredData() {
        val values = mutablePreferencesOf(intPreferencesKey("health_focus") to -1)
        assertEquals(HealthConfig(), HealthPreferenceCodec.read(values))
        assertEquals(-1, values[intPreferencesKey("health_focus")])
    }
    @Test fun invertedFragmentBoundsFallBack() {
        val values = mutablePreferencesOf(intPreferencesKey("health_fragmented_min") to 90, intPreferencesKey("health_fragmented_max") to 30)
        assertEquals(HealthConfig(), HealthPreferenceCodec.read(values))
    }
}
