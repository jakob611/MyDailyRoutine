package com.example.mydailyroutine.domain.health

import com.example.mydailyroutine.domain.model.ResolvedTimelineItem
import com.example.mydailyroutine.domain.model.RoutineCategory
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

enum class RecoveryStatus { INSERTED, FOCUS_SPLIT, FOCUS_MOVED, ALREADY_HANDLED, NO_SPACE }
data class RecoveryResult(val status: RecoveryStatus, val minutes: Int = 0, val date: LocalDate? = null)
data class BlockChange(val original: ResolvedTimelineItem.Block, val start: LocalDateTime, val end: LocalDateTime)
data class RecoveryPlan(
    val start: LocalDateTime,
    val end: LocalDateTime,
    val change: BlockChange? = null,
    val continuation: BlockChange? = null,
) {
    val minutes: Int get() = Duration.between(start, end).toMinutes().toInt()
}
sealed interface RecoveryDecision {
    data class Insert(val plan: RecoveryPlan) : RecoveryDecision
    data object AlreadyHandled : RecoveryDecision
    data object NoSpace : RecoveryDecision
}

/**
 * Conservative, deterministic recovery insertion. Never moves school, projects, personal commitments,
 * completed work, or exams; never destroys study minutes. A focus occurrence may be split or moved
 * only when the complete resulting interval is free. Otherwise use a real free slot, or do nothing.
 * The repository re-evaluates the warning and applies the complete plan in one Room transaction.
 */
class RecoveryPlanner {
    fun recommendedMinutes(warning: HealthWarning, config: HealthConfig, items: List<ResolvedTimelineItem>): Int = when (warning.type) {
        WarningType.HIGH_COGNITIVE_LOAD, WarningType.BURNOUT_RISK -> HealthConfig.COGNITIVE_RECOVERY_MINUTES
        WarningType.INSUFFICIENT_TRANSITION -> config.transitionMinutes.coerceAtLeast(HealthConfig.SUGGESTED_BREAK_MINUTES)
        WarningType.FRAGMENTED_TIME -> {
            val following = items.filterIsInstance<ResolvedTimelineItem.Block>()
                .filter { it.category.isDeepWork && it.startMinute > warning.atMinute }
                .minOfOrNull { it.startMinute }
            ((following ?: (warning.atMinute + 15)) - warning.atMinute).coerceAtLeast(1)
        }
        else -> HealthConfig.SUGGESTED_BREAK_MINUTES
    }

    fun plan(date: LocalDate, items: List<ResolvedTimelineItem>, warning: HealthWarning, config: HealthConfig): RecoveryDecision {
        val blocks = items.filterIsInstance<ResolvedTimelineItem.Block>().filterNot { it.isSuppressed }.sortedBy { if (it.date == date) 0 else 1 }.distinctBy { it.occurrenceKey }
        val minutes = recommendedMinutes(warning, config, items.filter { it.date == date })
        val dayStart = date.atStartOfDay()
        val dayEnd = date.plusDays(1).atStartOfDay()
        var target = dayStart.plusMinutes(warning.atMinute.toLong())
        fun free(start: LocalDateTime, end: LocalDateTime, excluding: ResolvedTimelineItem.Block? = null): Boolean =
            start < end && end <= date.plusDays(2).atStartOfDay() && blocks.none {
                it.occurrenceKey != excluding?.occurrenceKey && it.startsAt < end && it.endsAt > start
            } && items.filterIsInstance<ResolvedTimelineItem.Milestone>().none {
                it.isExam && !it.isCompleted && it.dueTime != null && it.date.atTime(it.dueTime) >= start && it.date.atTime(it.dueTime) < end
            }
        fun alreadyResting(at: LocalDateTime): Boolean = blocks.any {
            it.category == RoutineCategory.REST_BUFFER && Duration.between(it.startsAt, it.endsAt).toMinutes() >= minutes &&
                it.startsAt <= at && it.endsAt >= at
        }

        if (warning.type == WarningType.INSUFFICIENT_TRANSITION) {
            val study = blocks.firstOrNull { it.key in warning.relatedItemKeys && it.category.isDeepWork && !it.isCompleted }
            val school = study?.let { focus -> blocks.filter { it.category == RoutineCategory.SCHOOL && it.startsAt <= focus.startsAt }.maxByOrNull { it.endsAt } }
            if (study != null && school != null) {
                target = school.endsAt
                if (alreadyResting(target)) return RecoveryDecision.AlreadyHandled
                val end = target.plusMinutes(minutes.toLong())
                val shiftedStart = maxOf(study.startsAt, end)
                val shiftedEnd = study.endsAt.plus(Duration.between(study.startsAt, shiftedStart))
                if (shiftedStart.toLocalDate() == study.occurrenceDate && free(target, end, study) && free(shiftedStart, shiftedEnd, study)) {
                    return RecoveryDecision.Insert(RecoveryPlan(target, end, BlockChange(study, shiftedStart, shiftedEnd)))
                }
            }
        }

        val focus = blocks.firstOrNull {
            it.key in warning.relatedItemKeys && it.category.isDeepWork && !it.isCompleted &&
                (warning.type == WarningType.CONCENTRATION_LIMIT || (it.startsAt < target && it.endsAt > target))
        }
        if (focus != null && warning.type != WarningType.FRAGMENTED_TIME && warning.type != WarningType.INSUFFICIENT_TRANSITION) {
            if (warning.type == WarningType.CONCENTRATION_LIMIT) target = maxOf(dayStart, focus.startsAt.plusMinutes(config.focusLimitMinutes.toLong()))
            if (alreadyResting(target)) return RecoveryDecision.AlreadyHandled
            val restEnd = target.plusMinutes(minutes.toLong())
            val continuedEnd = focus.endsAt.plusMinutes(minutes.toLong())
            if (target > focus.startsAt && target < focus.endsAt && free(target, continuedEnd, focus)) {
                return RecoveryDecision.Insert(RecoveryPlan(target, restEnd,
                    BlockChange(focus, focus.startsAt, target), BlockChange(focus, restEnd, continuedEnd)))
            }
        }

        // School is fixed. Recovery can follow it, but must not cut a lesson into fictitious pieces.
        while (true) {
            val covering = blocks.filter { it.category == RoutineCategory.SCHOOL && it.startsAt <= target && it.endsAt > target }.maxByOrNull { it.endsAt }
            if (covering == null) break
            target = covering.endsAt
        }
        if (alreadyResting(target)) return RecoveryDecision.AlreadyHandled
        val firstMinute = Duration.between(dayStart, maxOf(dayStart, target)).toMinutes().toInt()
        if (firstMinute >= 1440) return RecoveryDecision.NoSpace
        val occupied = blocks.mapNotNull {
            val start = maxOf(it.startsAt, dayStart)
            val end = minOf(it.endsAt, dayEnd)
            if (start >= end) null else MinuteInterval(Duration.between(dayStart, start).toMinutes().toInt(), Duration.between(dayStart, end).toMinutes().toInt())
        } + items.filterIsInstance<ResolvedTimelineItem.Milestone>().filter { it.date == date && it.isExam && !it.isCompleted && it.dueTime != null }.map {
            val start = it.dueTime!!.toSecondOfDay() / 60
            MinuteInterval(start, start + 1)
        }
        val slot = Intervals.subtract(listOf(MinuteInterval(firstMinute, 1440)), occupied).firstOrNull { it.duration >= minutes }
            ?: return RecoveryDecision.NoSpace
        val start = dayStart.plusMinutes(slot.start.toLong())
        return RecoveryDecision.Insert(RecoveryPlan(start, start.plusMinutes(minutes.toLong())))
    }
}
