package com.example.mydailyroutine

/**
 * How long a UI test waits for the app to reach a state before it calls the run a failure.
 *
 * One number, because twelve copies of `10000` are twelve separate decisions that nobody ever
 * revisits together. What the number is is patience for a shared CI emulator: switching mode here
 * also runs a database query for the new range and composes a month of cells, and the machine is
 * running an emulator, a Gradle daemon and the build at once.
 *
 * It is not a claim about how fast the app is. A screen that is genuinely broken still fails the
 * test — it just takes longer to say so, which is the right trade when the alternative is a suite
 * that cries wolf.
 */
internal const val UiWaitMillis = 20_000L
