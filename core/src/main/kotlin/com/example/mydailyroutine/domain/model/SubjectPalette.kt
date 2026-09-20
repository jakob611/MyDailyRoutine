package com.example.mydailyroutine.domain.model

/**
 * Subject colours are persisted as ARGB longs, so the import/backup boundary needs a palette that
 * does not depend on Android or Compose. The Compose design system mirrors this list for new users.
 */
object SubjectPalette {
    const val Default = 0xFF67E8F9L

    val swatches = listOf(
        0xFF67E8F9L, // timer cyan
        0xFF34D399L, // success green
        0xFF2DD4BFL, // primary teal
        0xFFFB7185L, // error rose
        0xFFA78BFAL, // focus violet
        0xFFFBBF24L, // warning amber
    )
}
