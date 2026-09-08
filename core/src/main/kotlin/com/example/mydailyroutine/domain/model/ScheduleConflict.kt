package com.example.mydailyroutine.domain.model

/** Typed domain conflict; Android maps it to local, actionable copy instead of leaking exceptions. */
enum class ScheduleConflictReason { ACTIVE_EXECUTION, PREVIOUS_STAGE, PROTECTED_TIME, CLOCK_CHANGED }
class ScheduleConflict(val reason: ScheduleConflictReason) : IllegalStateException(reason.name)
