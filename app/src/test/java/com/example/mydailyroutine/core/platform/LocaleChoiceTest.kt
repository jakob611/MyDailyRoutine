package com.example.mydailyroutine.core.platform

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Two translations, one rule: English gets English, everything else gets Slovenian. The rule is here
 * rather than in a phone because a third language must never be "half English": the default resource
 * set is Slovenian, so an untranslated phone reads a complete language instead of a mixed one.
 */
class LocaleChoiceTest {
    @Test fun englishGetsEnglish() {
        assertEquals("en", uiLocaleFor("en").language)
    }

    @Test fun slovenianGetsSlovenian() {
        assertEquals("sl", uiLocaleFor("sl").language)
    }

    @Test fun anUntranslatedLanguageFallsBackToSlovenian() {
        // The one that matters: a German phone must not read English strings with Slovenian dates.
        assertEquals("sl", uiLocaleFor("de").language)
        assertEquals("sl", uiLocaleFor("fr").language)
        assertEquals("sl", uiLocaleFor("").language)
    }
}
