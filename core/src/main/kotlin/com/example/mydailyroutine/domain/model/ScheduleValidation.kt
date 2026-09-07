package com.example.mydailyroutine.domain.model

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle

object ScheduleValidation {
    // Match Material DatePicker's supported navigation range; domain resolution itself stays date-agnostic.
    val firstUiDate: LocalDate = LocalDate.of(1900, 1, 1)
    val lastUiDate: LocalDate = LocalDate.of(2100, 12, 31)
    private val timeFormat = DateTimeFormatter.ofPattern("H:mm").withResolverStyle(ResolverStyle.STRICT)

    fun title(value: String) {
        require(value.isNotBlank() && value.trim().length <= 120) { "Use a title of 1–120 characters." }
    }

    fun minutePrecision(time: LocalTime) {
        require(time.second == 0 && time.nano == 0) { "Use whole-minute times." }
    }

    fun times(start: LocalTime, end: LocalTime) {
        minutePrecision(start)
        minutePrecision(end)
        require(start != end) { "Start and end must differ. An earlier end means the next day." }
    }

    fun subject(subject: Subject) {
        require(subject.id >= 0)
        title(subject.name)
        require(subject.defaultDurationMinutes in 1..1439) { "Duration must be 1–1439 minutes." }
        require(subject.colorHex in 0xFF000000L..0xFFFFFFFFL) { "Choose an opaque ARGB color." }
    }

    fun routine(block: RoutineBlueprint) {
        require(block.id >= 0)
        require(block.subjectId == null || block.subjectId > 0)
        val duration = nominalMinutes(block.startTime, block.endTime)
        require(block.minDurationMinutes in 0..duration && (block.category.isBuffer || block.minDurationMinutes > 0))
        require(block.elasticity.isFinite() && block.elasticity in 0.0..1_000_000.0)
        require(block.priorityWeight.isFinite() && block.priorityWeight > 0.0 && block.priorityWeight <= 1_000_000.0)
        require(block.rawDurationMinutes in 1..1439)
        require(block.category != RoutineCategory.SCHOOL || (block.isFixedCommitment && block.elasticity == 0.0 && block.minDurationMinutes == duration))
        title(block.title)
        times(block.startTime, block.endTime)
        require(block.validFrom == null || block.validUntil == null || block.validUntil >= block.validFrom) {
            "The repeat end must not precede its start."
        }
    }

    fun exception(override: EventOverride, base: RoutineBlueprint) {
        require(override.routineBlockId == base.id && base.occursOn(override.overrideDate)) {
            "This template does not occur on that date."
        }
        require(override.dayShift in 0..MAX_OCCURRENCE_SHIFT_DAYS)
        override.customTitle?.let(::title)
        times(override.customStartTime ?: base.startTime, override.customEndTime ?: base.endTime)
    }

    fun milestone(milestone: Milestone) {
        require(milestone.id >= 0)
        require(milestone.subjectId == null || milestone.subjectId > 0)
        require(milestone.estimatedEffortHours.isFinite() && milestone.estimatedEffortHours in 0.0..1000.0)
        require(!milestone.isTerminalExam || milestone.isExam)
        title(milestone.title)
        milestone.dueTime?.let(::minutePrecision)
    }

    fun parseTime(value: String): LocalTime? = runCatching {
        LocalTime.parse(value.trim(), timeFormat).also(::minutePrecision)
    }.getOrNull()

    fun parseDate(value: String): LocalDate? = runCatching { LocalDate.parse(value.trim()) }.getOrNull()
        ?.takeIf { it in firstUiDate..lastUiDate }
}
