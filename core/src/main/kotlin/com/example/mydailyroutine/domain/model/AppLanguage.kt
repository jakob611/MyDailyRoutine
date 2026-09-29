package com.example.mydailyroutine.domain.model

/**
 * The reader's explicit choice of interface language, stored as a BCP-47 primary subtag.
 *
 * `null` is a real value, not an accident: it means *no choice was made*, and the app then follows
 * the device — an English device speaks English, everything else speaks Slovenian. An explicit
 * choice always wins over the device, and it is the only place in the domain that validates a tag:
 * an unknown value must read as "no choice" everywhere it is used, never as a third language,
 * because exactly two complete translations ship.
 */
object AppLanguage {
    const val SLOVENIAN = "sl"
    const val ENGLISH = "en"

    /** The two complete translations. Anything else is not a language, it is "follow the device". */
    val valid: Set<String> = setOf(SLOVENIAN, ENGLISH)

    /** `null` stays `null`; a valid tag stays as-is; anything else becomes `null`. */
    fun normalize(tag: String?): String? = tag?.takeIf { it in valid }
}
