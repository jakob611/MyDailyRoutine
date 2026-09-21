package com.example.mydailyroutine.core.platform

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalTime

/**
 * Where a failure goes when the reader cannot see it.
 *
 * The app is local-first: there is no crash reporting, no network and no analyst. A caught exception
 * used to be written into a generic "could not save" message, which told the person tapping the
 * screen nothing and told whoever is debugging it even less. From here a failure has a name, reaches
 * `logcat`, and stays in a small in-memory list that the Data settings tab can show on the device —
 * so "it did not save" turns into "the last thing that went wrong was X, ten minutes ago" without any
 * data ever leaving the phone. The list is intentionally short and lives only in memory.
 */
object Diagnostics {
    const val Tag = "DailyRoutine"

    /** Enough history to see a pattern, little enough to read on a phone. */
    private const val Capacity = 12

    /**
     * `14:23:45`. Deliberately not a date pattern: this is a log line, not something the interface
     * shows as a date, and `java.time`'s own ISO output saves the app from owning a second date format.
     */
    private fun stamp(): String = LocalTime.now().toString().take(8)

    private val entries = mutableListOf<String>()
    private val _recent = MutableStateFlow<List<String>>(emptyList())

    /** The newest [Capacity] failures, newest first. In memory only; cleared by restarting the app. */
    val recent: StateFlow<List<String>> = _recent.asStateFlow()

    /** Records a failure the reader should never have to see, but a developer one day must. */
    fun warn(topic: String, error: Throwable? = null) {
        Log.w(Tag, topic, error)
        record("$topic: ${error?.let { it::class.java.simpleName } ?: "unknown"}")
    }

    /** Records something that is not an exception but is still worth knowing about. */
    fun note(topic: String, detail: String) {
        Log.i(Tag, "$topic: $detail")
        record("$topic: $detail")
    }

    fun clear() {
        entries.clear()
        _recent.value = emptyList()
    }

    private fun record(line: String) {
        synchronized(entries) {
            entries.add(0, "${LocalTime.now().format(clock)} · $line")
            while (entries.size > Capacity) entries.removeAt(entries.lastIndex)
            _recent.value = entries.toList()
        }
    }
}
