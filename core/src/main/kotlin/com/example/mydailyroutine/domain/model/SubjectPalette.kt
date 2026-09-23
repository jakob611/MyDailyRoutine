package com.example.mydailyroutine.domain.model

/**
 * Subject colours are persisted as ARGB longs, so the import/backup boundary needs a palette that
 * does not depend on Android or Compose. The Compose design system mirrors this list for new users.
 *
 * Sixteen swatches, not six: a school year easily runs to a dozen subjects, and a colour that repeats
 * is a colour that stops identifying anything. The order is the order the picker shows them in — hue
 * first, so scanning the grid is like walking the colour wheel, with the muted slate last because it
 * is the one that reads as "none of the above". Anything outside this list is still allowed: the
 * editor takes any hex value, this is only the shelf of ready ones.
 */
object SubjectPalette {
    const val Default = 0xFF67E8F9L

    val swatches = listOf(
        0xFF67E8F9L, // cyan
        0xFF2DD4BFL, // teal
        0xFF34D399L, // emerald
        0xFF4ADE80L, // green
        0xFFA3E635L, // lime
        0xFFFDE047L, // yellow
        0xFFFBBF24L, // amber
        0xFFFB923CL, // orange
        0xFFF87171L, // red
        0xFFFB7185L, // rose
        0xFFF472B6L, // pink
        0xFFE879F9L, // fuchsia
        0xFFA78BFAL, // violet
        0xFF818CF8L, // indigo
        0xFF60A5FAL, // blue
        0xFF94A3B8L, // slate
    )

    /** The first swatch no other subject has claimed, so a new subject arrives with its own colour. */
    fun firstFree(taken: Collection<Long>): Long = swatches.firstOrNull { it !in taken } ?: Default
}
