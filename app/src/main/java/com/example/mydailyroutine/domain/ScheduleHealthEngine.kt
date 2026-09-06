package com.example.mydailyroutine.domain

import java.time.Duration
import java.time.LocalTime

class ScheduleHealthEngine {
    fun evaluate(items: List<ResolvedTimelineItem>): List<HealthWarning> {
        val routines = items
            .filter { it.kind == TimelineItemKind.ROUTINE && it.category != null }
            .sortedWith(compareBy<ResolvedTimelineItem> { it.startTime }.thenBy { it.endTime })
        if (routines.isEmpty()) return emptyList()

        val warnings = mutableListOf<HealthWarning>()

        // A single deep-work session is not allowed to silently grow past 90 minutes.
        routines.filter { it.category == RoutineCategory.FOCUS_STUDY }
            .filter { it.durationMinutes > 90 }
            .forEach { item ->
                warnings += HealthWarning(
                    type = WarningType.CONCENTRATION_LIMIT,
                    message = "Continuous deep focus past 90 min causes sharp cognitive decline. Insert a 10–15 min recovery block.",
                    relatedItemId = item.stableId,
                )
            }

        // Count cognitive work until an explicit recovery block of at least 20 minutes.
        var cognitiveMinutes = 0
        routines.forEach { item ->
            when {
                item.category == RoutineCategory.REST_BREAK && item.durationMinutes >= 20 -> {
                    cognitiveMinutes = 0
                }
                item.category == RoutineCategory.FOCUS_STUDY || item.category == RoutineCategory.SCHOOL -> {
                    cognitiveMinutes += item.durationMinutes
                    if (cognitiveMinutes > 180) {
                        warnings += HealthWarning(
                            type = WarningType.HIGH_COGNITIVE_LOAD,
                            message = "Mental stamina exhausted. Schedule a physical or screen-free break.",
                            relatedItemId = item.stableId,
                        )
                        cognitiveMinutes = 0
                    }
                }
            }
        }

        val schools = routines.filter { it.category == RoutineCategory.SCHOOL }
        val focus = routines.filter { it.category == RoutineCategory.FOCUS_STUDY }
        schools.forEach { school ->
            focus.filter { it.startTime >= school.endTime }
                .minByOrNull { it.startTime }
                ?.let { nextFocus ->
                    val gap = minutesBetween(school.endTime, nextFocus.startTime)
                    if (gap in 0L..29L) {
                        warnings += HealthWarning(
                            type = WarningType.INSUFFICIENT_TRANSITION,
                            message = "No buffer after school. Allow at least 30 minutes for a nutritional and cognitive reset.",
                            relatedItemId = nextFocus.stableId,
                        )
                    }
                }
        }

        val deepWorkMinutes = focus.sumOf(ResolvedTimelineItem::durationMinutes)
        if (deepWorkMinutes > 300) {
            warnings += HealthWarning(
                type = WarningType.BURNOUT_RISK,
                message = "Total daily deep work exceeds human sustained limits (~4–5h). Diminishing returns detected.",
            )
        }

        findSittingSpans(routines).filter { it.durationMinutes > 120 }.forEach { span ->
            warnings += HealthWarning(
                type = WarningType.PHYSICAL_RESET,
                message = "Take a walk or implement the 20-20-20 visual rule to prevent eye strain and fatigue.",
                relatedItemId = span.relatedItemId,
            )
        }

        routines.windowed(size = 2, step = 1).forEach { pair ->
            val first = pair[0]
            val second = pair[1]
            if (first.category == RoutineCategory.FOCUS_STUDY && second.category == RoutineCategory.FOCUS_STUDY) {
                val gap = minutesBetween(first.endTime, second.startTime)
                if (gap in 45L..90L) {
                    warnings += HealthWarning(
                        type = WarningType.FRAGMENTED_TIME,
                        message = "Fragmented dead window. Either consolidate blocks or convert this into intentional recovery.",
                        relatedItemId = second.stableId,
                    )
                }
            }
        }

        return warnings.distinctBy { Triple(it.type, it.relatedItemId, it.message) }
    }

    private fun findSittingSpans(routines: List<ResolvedTimelineItem>): List<SittingSpan> {
        val cognitive = routines.filter {
            it.category == RoutineCategory.FOCUS_STUDY || it.category == RoutineCategory.SCHOOL
        }
        if (cognitive.isEmpty()) return emptyList()

        val spans = mutableListOf<SittingSpan>()
        var start = cognitive.first().startTime
        var end = cognitive.first().endTime
        var related = cognitive.first().stableId
        cognitive.drop(1).forEach { item ->
            val gap = minutesBetween(end, item.startTime)
            if (gap in 0L..15L) {
                if (item.endTime > end) end = item.endTime
            } else {
                spans += SittingSpan(start, end, related)
                start = item.startTime
                end = item.endTime
                related = item.stableId
            }
        }
        spans += SittingSpan(start, end, related)
        return spans
    }

    private fun minutesBetween(start: LocalTime, end: LocalTime): Long =
        Duration.between(start, end).toMinutes().coerceAtLeast(0)

    private data class SittingSpan(
        val start: LocalTime,
        val end: LocalTime,
        val relatedItemId: String,
    ) {
        val durationMinutes: Long get() = Duration.between(start, end).toMinutes().coerceAtLeast(0)
    }
}

data class HealthWarning(
    val type: WarningType,
    val message: String,
    val relatedItemId: String? = null,
    val severity: WarningSeverity = WarningSeverity.MEDIUM,
)

enum class WarningType {
    CONCENTRATION_LIMIT,
    HIGH_COGNITIVE_LOAD,
    INSUFFICIENT_TRANSITION,
    BURNOUT_RISK,
    PHYSICAL_RESET,
    FRAGMENTED_TIME,
}

enum class WarningSeverity {
    LOW,
    MEDIUM,
    HIGH,
}
