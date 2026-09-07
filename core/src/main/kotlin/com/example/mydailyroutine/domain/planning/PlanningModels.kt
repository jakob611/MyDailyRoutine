package com.example.mydailyroutine.domain.planning

import com.example.mydailyroutine.domain.model.RoutineCategory
import java.time.LocalDate

/** Minute-exact immutable numerical projection of one resolved occurrence, not another schedule store. */
data class TimeBlock(
    val id: String,
    val name: String,
    val category: RoutineCategory,
    val startMinutes: Int,
    val durationMinutes: Int,
    val minDurationMinutes: Int,
    val isFixedCommitment: Boolean,
    val elasticity: Double,
    val priorityWeight: Double,
    val completedActualMinutes: Int? = null,
) {
    init {
        require(id.isNotBlank() && startMinutes in -10080..10080)
        require(durationMinutes in 0..1439 && (durationMinutes > 0 || isFixedCommitment || category.isBuffer))
        require(minDurationMinutes in 0..durationMinutes)
        require(elasticity.isFinite() && elasticity in 0.0..1_000_000.0)
        require(priorityWeight.isFinite() && priorityWeight > 0.0 && priorityWeight <= 1_000_000.0)
        require(completedActualMinutes == null || completedActualMinutes in 1..10080)
    }
    val endMinutes: Int get() = startMinutes + durationMinutes
    val isFixed: Boolean get() = isFixedCommitment || category == RoutineCategory.SCHOOL
}

data class HealingReport(
    val initialDeficitMinutes: Int,
    val slackUsedMinutes: Int,
    val bufferUsedMinutes: Int,
    val compressionMinutes: Int,
    val deferredCount: Int,
    val deferredMinutes: Int,
    val fixedBoundaryMinutes: Int,
    val boundaryAlreadyPassed: Boolean,
)
data class HealingResult(val blocks: List<TimeBlock>, val deferred: List<TimeBlock>, val report: HealingReport)

data class PlanningConfig(
    val dailyStudyCapacityMinutes: Int = 270,
    val studyStartMinutes: Int = 9 * 60,
    val studyEndMinutes: Int = 20 * 60 + 30,
    val dipCenterMinutes: Int = 14 * 60 + 15,
    val dipSigmaMinutes: Int = 45,
    val defaultSlipMinutes: Int = 45,
    val targetFocusMinutes: Int = 75,
    val postSchoolRecoveryMinutes: Int = 45,
) {
    init {
        require(dailyStudyCapacityMinutes in 30..600)
        require(studyStartMinutes in 0..1438 && studyEndMinutes in (studyStartMinutes + 1)..1440)
        require(dipCenterMinutes in 0..1439 && dipSigmaMinutes in 15..180)
        require(defaultSlipMinutes in 1..720 && targetFocusMinutes in 25..90 && postSchoolRecoveryMinutes in 30..120)
    }
    // Exact integer fifth, never a floating-point 20% approximation.
    val dailyReviewCap: Int get() = dailyStudyCapacityMinutes / 5
}

enum class BacklogReason { SLIPPAGE, CAPACITY, REVIEW_CAPACITY }
data class BacklogEntry(
    val id: Long = 0, val title: String, val category: RoutineCategory,
    val durationMinutes: Int, val minDurationMinutes: Int, val elasticity: Double, val priorityWeight: Double,
    val subjectId: Long? = null, val sourceRoutineId: Long? = null, val occurrenceDate: LocalDate? = null,
    val milestoneId: Long? = null, val topicId: Long? = null, val reviewId: Long? = null,
    val reason: BacklogReason = BacklogReason.SLIPPAGE,
    val rawDurationMinutes: Int = durationMinutes,
)
data class PlacementResult(val placed: Boolean, val date: LocalDate? = null)
data class PlanSummary(val studyMinutes: Int, val reserveMinutes: Int, val reviews: Int, val deferredMinutes: Int)
