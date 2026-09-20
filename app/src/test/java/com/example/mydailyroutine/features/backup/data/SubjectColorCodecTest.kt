package com.example.mydailyroutine.features.backup.data

import org.junit.Assert.assertEquals
import org.junit.Test

class SubjectColorCodecTest {
    private val fallback = 0xFF93C5FDL

    @Test fun acceptsLegacyRgbAndCurrentArgb() {
        assertEquals(0xFFAABBCCL, SubjectColorCodec.decode("#abc", fallback))
        assertEquals(0xFF123456L, SubjectColorCodec.decode("#123456", fallback))
        assertEquals(0xFF123456L, SubjectColorCodec.decode("#FF123456", fallback))
        assertEquals(0xFF123456L, SubjectColorCodec.decode(" 123456 ", fallback))
    }

    @Test fun normalizesAlphaWithoutRecolouringUserData() {
        assertEquals(0xFF7DE2D1L, SubjectColorCodec.decode("#7DE2D1", fallback))
        assertEquals(0xFF123456L, SubjectColorCodec.decode("00123456", fallback))
    }

    @Test fun invalidOrMissingValuesUseInjectedDefault() {
        listOf("", "#", "12345", "no-color", "-12345", "FFFFFFFFF").forEach {
            assertEquals(fallback, SubjectColorCodec.decode(it, fallback))
        }
    }
}
