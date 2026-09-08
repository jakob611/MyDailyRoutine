package com.example.mydailyroutine.domain.learning

import java.time.LocalDate

data class HistoricalVelocity(val subjectId: String?, val plannedDurationMinutes: Int, val actualDurationMinutes: Int, val timestamp: Long, val id: Long = 0)
data class SubjectVelocity(val subjectId: String, val multiplier: Double, val sampleCount: Int, val conservativeMultiplier: Double)
data class StudyTopic(val id: Long = 0, val title: String, val subjectId: Long?, val initialDate: LocalDate,
    val finalDate: LocalDate, val reviewCount: Int = 4, val reviewDurationMinutes: Int = 15,
    val priorityWeight: Double = 3.0, val milestoneId: Long? = null)
data class SpacedReview(val id: Long = 0, val topicId: Long, val scheduledEpochDay: Long, val durationMinutes: Int,
    val priorityWeight: Double, val ordinal: Int, val timeBlockId: Long? = null, val isCompleted: Boolean = false)
data class ReviewDayCapacity(val epochDay: Long, val studyCapacityMinutes: Int, val studyMinutes: Int,
    val reviewMinutes: Int, val freeMinutes: Int, val longestFreeSlotMinutes: Int = freeMinutes)
data class ReviewRequest(val topicId: Long, val initialEpochDay: Long, val finalEpochDay: Long,
    val count: Int, val durationMinutes: Int, val priorityWeight: Double = 3.0,
    val minimumDurationMinutes: Int = minOf(10, durationMinutes)) {
    init { require(minimumDurationMinutes in 1..durationMinutes) }
}
data class ReviewAllocation(val ordinal: Int, val idealEpochDay: Long, val scheduledEpochDay: Long?, val durationMinutes: Int, val jitterRadiusDays: Int, val isCompact: Boolean = false)
