package com.example.mydailyroutine.features.backup.data

/** Legacy RGB backups and current ARGB backups share one opaque persisted representation.
 * The composition root supplies the default: the data layer does not depend on Compose/the theme.
 */
internal object SubjectColorCodec {
    fun decode(value: String, fallback: Long): Long {
        val raw = value.trim().removePrefix("#")
        val rgb = when (raw.length) {
            3 -> raw.map { "$it$it" }.joinToString("")
            6, 8 -> raw
            else -> return fallback
        }
        if (!rgb.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }) return fallback
        val parsed = rgb.toLongOrNull(16) ?: return fallback
        return (parsed and 0x00FFFFFFL) or 0xFF000000L
    }
}
