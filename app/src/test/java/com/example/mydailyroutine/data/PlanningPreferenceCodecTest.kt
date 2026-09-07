package com.example.mydailyroutine.data

import androidx.datastore.preferences.core.*
import com.example.mydailyroutine.data.preferences.PlanningPreferenceCodec
import com.example.mydailyroutine.domain.planning.PlanningConfig
import org.junit.Assert.*
import org.junit.Test

class PlanningPreferenceCodecTest {
    @Test fun defaultsAreSafe() { assertEquals(PlanningConfig(),PlanningPreferenceCodec.read(emptyPreferences())) }
    @Test fun everySettingRoundTrips() {
        val config=PlanningConfig(240,480,1200,870,60,30,60,60)
        val values=mutablePreferencesOf();PlanningPreferenceCodec.write(values,config)
        assertEquals(config,PlanningPreferenceCodec.read(values))
    }
    @Test fun malformedPersistedRangeFallsBackWithoutOverwriting() {
        val key=intPreferencesKey("planning_capacity");val values=mutablePreferencesOf(key to -1)
        assertEquals(PlanningConfig(),PlanningPreferenceCodec.read(values));assertEquals(-1,values[key])
    }
}
