package com.example.mydailyroutine.domain

import com.example.mydailyroutine.domain.model.RoutineCategory
import com.example.mydailyroutine.domain.planning.AgendaProjection
import java.time.ZoneOffset
import org.junit.Assert.*
import org.junit.Test

class AgendaProjectionTest {
    @Test fun `widget projects actual progress reserve remainder and only two next blocks`() {
        val tasks=listOf(block(480,540),block(540,570,RoutineCategory.EMERGENCY_RESERVE),block(570,600),block(600,660))
        val projection=AgendaProjection.from(tasks,monday.atTime(8,30).toInstant(ZoneOffset.UTC),ZoneOffset.UTC)
        assertEquals(tasks[0],projection.active)
        assertEquals(0.5f,projection.progress,0.001f)
        assertEquals(30,projection.reserveRemainingMinutes)
        assertEquals(2,projection.upcoming.size)
        val later=AgendaProjection.from(tasks,monday.atTime(9,15).toInstant(ZoneOffset.UTC),ZoneOffset.UTC)
        assertEquals(15,later.reserveRemainingMinutes)
    }
    @Test fun `overlapping reserves do not invent extra available time`() {
        val tasks=listOf(block(540,570,RoutineCategory.EMERGENCY_RESERVE),block(550,580,RoutineCategory.EMERGENCY_RESERVE))
        val result=AgendaProjection.from(tasks,monday.atTime(9,0).toInstant(ZoneOffset.UTC),ZoneOffset.UTC)
        assertEquals(40,result.reserveRemainingMinutes)
    }
    @Test fun `completed and suppressed blocks never become active`() {
        val tasks=listOf(block(480,540).copy(isCompleted=true),block(480,540,RoutineCategory.SCHOOL).copy(holidayTitle="Praznik"))
        val result=AgendaProjection.from(tasks,monday.atTime(8,30).toInstant(ZoneOffset.UTC),ZoneOffset.UTC)
        assertNull(result.active);assertTrue(result.upcoming.isEmpty());assertEquals(0,result.reserveRemainingMinutes)
    }
}
