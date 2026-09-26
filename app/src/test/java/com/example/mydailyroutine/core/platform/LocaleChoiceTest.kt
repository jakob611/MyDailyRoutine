package com.example.mydailyroutine.core.platform

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Two translations, two rules: an explicit reader choice wins; without one, English gets English
 * and everything else gets Slovenian. The rules are here rather than in a phone because a third
 * language must never be "half English": the default resource set is Slovenian, so an untranslated
 * phone reads a complete language instead of a mixed one.
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

    @Test fun anExplicitChoiceWinsOverTheDevice() {
        // A Slovenian phone with an English choice, and an English phone with a Slovenian choice:
        // the reader's tap is the first rule, the device is only the fallback.
        assertEquals("en", uiLocaleFor("sl", "en").language)
        assertEquals("sl", uiLocaleFor("en", "sl").language)
        assertEquals("en", uiLocaleFor("de", "en").language)
        assertEquals("sl", uiLocaleFor("fr", "sl").language)
    }

    @Test fun aChoiceOutsideTheTwoTranslationsFallsBackToTheDevice() {
        // The store is trusted for the key, not for its grammar: a third tag must read as "no
        // choice", never as a language the app does not speak.
        assertEquals("en", uiLocaleFor("en", "fr").language)
        assertEquals("sl", uiLocaleFor("de", "de").language)
        assertEquals("en", uiLocaleFor("en", null).language)
        assertEquals("sl", uiLocaleFor("de", null).language)
    }
}
