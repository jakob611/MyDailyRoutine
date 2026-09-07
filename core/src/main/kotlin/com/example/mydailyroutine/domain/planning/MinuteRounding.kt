package com.example.mydailyroutine.domain.planning

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.round

/** Only erase a few arithmetic ULPs at an integer boundary; otherwise conservatively round up. */
object MinuteRounding {
    fun ceiling(value: Double, maximum: Int = Int.MAX_VALUE): Int {
        require(value.isFinite() && value >= 0 && maximum >= 0)
        val nearest = round(value)
        val normalized = if (abs(value - nearest) <= Math.ulp(value) * 4.0) nearest else value
        return ceil(normalized).coerceIn(0.0, maximum.toDouble()).toInt()
    }
}
