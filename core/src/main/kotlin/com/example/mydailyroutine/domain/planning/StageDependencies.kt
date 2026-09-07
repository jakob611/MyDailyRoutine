package com.example.mydailyroutine.domain.planning

/** Shared closure used after time-deficit and daily-capacity deferral. */
object StageDependencies {
    fun dependentsToDefer(active: Collection<TimeBlock>, deferred: Collection<TimeBlock>, allowedIds: Set<String>? = null): List<TimeBlock> {
        val firstBlocked = deferred.filter { it.precedenceGroup != null && it.stageOrder != null }
            .groupBy { it.precedenceGroup }.mapValues { (_, tasks) -> tasks.minOf { it.stageOrder!! } }
        return active.filter { task ->
            !task.isFixed && !task.category.isBuffer && (allowedIds == null || task.id in allowedIds) &&
                task.precedenceGroup?.let { group -> firstBlocked[group]?.let { task.stageOrder != null && task.stageOrder > it } } == true
        }.sortedBy { it.id }
    }
}
