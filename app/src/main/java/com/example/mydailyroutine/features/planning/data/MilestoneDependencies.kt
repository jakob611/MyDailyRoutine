package com.example.mydailyroutine.features.planning.data

import com.example.mydailyroutine.core.database.RoutineDatabase
import com.example.mydailyroutine.domain.model.ResolvedTimelineItem
import com.example.mydailyroutine.domain.repository.TimelineRepository
import com.example.mydailyroutine.domain.repository.TimelineResolver
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

internal data class StageBounds(val notBefore: LocalDateTime?, val notAfter: LocalDateTime?)

/** Resolve dependency bounds through the canonical resolver, including moves and actual completion times. */
internal class MilestoneDependencies(private val db: RoutineDatabase, private val timeline: TimelineRepository) {
    suspend fun bounds(goal: Long, stage: Int): StageBounds {
        val rows = db.routines().goalStages(goal)
        val dates = rows.mapNotNull { it.validFrom }
        if (dates.isEmpty()) return StageBounds(null,null)
        val first = dates.minOrNull()!!
        val last = dates.maxOrNull()!!.plusDays(15)
        require(ChronoUnit.DAYS.between(first,last) <= 731)
        val schedule = TimelineResolver().prepare(timeline.snapshot(first,last))
        val occurrences = linkedMapOf<String, ResolvedTimelineItem.Block>()
        var date = first
        while (date <= last) {
            schedule.forDate(date).filterIsInstance<ResolvedTimelineItem.Block>().filter {
                it.milestoneId == goal && it.topicId == null && it.stageOrder != null && !it.isSuppressed
            }.forEach { occurrences.putIfAbsent(it.occurrenceKey,it) }
            date = date.plusDays(1)
        }
        val blocks = occurrences.values
        val before = blocks.filter { it.stageOrder!! < stage }.maxOfOrNull { it.endsAt }
        val after = blocks.filter { it.stageOrder!! > stage }.minOfOrNull { it.startsAt }
        return StageBounds(before,after)
    }
}
