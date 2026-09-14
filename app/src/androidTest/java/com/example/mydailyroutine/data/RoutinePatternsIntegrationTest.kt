package com.example.mydailyroutine.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.mydailyroutine.R
import com.example.mydailyroutine.core.database.*
import com.example.mydailyroutine.features.routines.data.RoomRoutinePatternsRepository
import com.example.mydailyroutine.features.timeline.data.RoomTimelineRepository
import com.example.mydailyroutine.domain.model.*
import com.example.mydailyroutine.domain.routines.*
import com.example.mydailyroutine.domain.scheduling.AlarmPlanner
import java.time.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoutinePatternsIntegrationTest {
    private val context=ApplicationProvider.getApplicationContext<Context>()
    private lateinit var db: RoutineDatabase
    private lateinit var timeline: RoomTimelineRepository
    private lateinit var patterns: RoomRoutinePatternsRepository
    private val monday=LocalDate.of(2027,1,11)
    private val clock=MutableClock(monday.minusDays(1).atTime(12,0).toInstant(ZoneOffset.UTC))
    @Before fun setup() {
        db=Room.inMemoryDatabaseBuilder(context,RoutineDatabase::class.java).addCallback(SeedAndIntegrityCallback(context.resources)).build()
        timeline=RoomTimelineRepository(db,{})
        patterns=RoomRoutinePatternsRepository(db,timeline,{},clock) { ZoneOffset.UTC }
    }
    @After fun cleanup() { db.close() }
    private fun lesson()=RoutineBlueprint(subjectId=null,title="Pouk",category=RoutineCategory.SCHOOL,dayOfWeek=monday.dayOfWeek,
        startTime=LocalTime.of(8,0),endTime=LocalTime.of(8,45),isNotificationEnabled=false,validFrom=monday)
    private fun sleep(enabled: Boolean=true,wake: LocalTime=LocalTime.of(7,0))=SleepSchedule(enabled,LocalTime.of(23,0),wake,Weekdays.mask(setOf(DayOfWeek.SUNDAY)),30)
    private suspend fun saveSleep(value: SleepSchedule) = patterns.saveSleep(value,context.getString(R.string.sleep_title),context.getString(R.string.sleep_morning_title))

    @Test fun selectedWeekdaysAndCompanionsCommitAsOnePattern(): Unit = runBlocking {
        val result=patterns.create(RoutinePatternRequest(lesson(),setOf(DayOfWeek.MONDAY,DayOfWeek.WEDNESDAY,DayOfWeek.FRIDAY),true,5,context.getString(R.string.lesson_break_title)))
        assertEquals(3,result.routineIds.size);assertEquals(3,result.companionIds.size)
        val mondayItems=timeline.getTimelineForDate(monday).first().filterIsInstance<ResolvedTimelineItem.Block>()
        assertEquals(2,mondayItems.size)
        assertEquals(525,mondayItems.single { it.parentRoutineId!=null }.startMinute)
        assertEquals(3,mondayItems.first().seriesDays.size)
        assertTrue(timeline.getTimelineForDate(monday.plusDays(1)).first().isEmpty())
        val group=mondayItems.first().seriesKey!!
        patterns.editSeries(group,"Matematika",LocalTime.of(9,0),LocalTime.of(9,45))
        for(offset in listOf(0L,2L,4L)) {
            val items=timeline.getTimelineForDate(monday.plusDays(offset)).first().filterIsInstance<ResolvedTimelineItem.Block>()
            assertEquals(540,items.single { it.parentRoutineId==null }.startMinute)
            assertEquals(585,items.single { it.parentRoutineId!=null }.startMinute)
        }
        patterns.deleteSeries(group)
        assertTrue(timeline.snapshot(monday,monday.plusWeeks(2)).routines.isEmpty())
    }
    @Test fun cancelledSchoolDayDoesNotLeaveAnOrphanBreak(): Unit = runBlocking {
        val result=patterns.create(RoutinePatternRequest(lesson(),setOf(DayOfWeek.MONDAY),true,5,context.getString(R.string.lesson_break_title)))
        timeline.cancelOccurrence(result.routineIds.single(),monday)
        assertTrue(timeline.getTimelineForDate(monday).first().isEmpty())
        assertEquals(2,timeline.getTimelineForDate(monday.plusWeeks(1)).first().size)
        timeline.restoreOccurrence(result.routineIds.single(),monday)
        assertEquals(2,timeline.getTimelineForDate(monday).first().size)
        timeline.deleteRoutine(result.routineIds.single())
        assertNull(db.routines().get(result.companionIds.single()))
    }
    @Test fun collidingBreakIsNotCountedOrNotified(): Unit = runBlocking {
        patterns.create(RoutinePatternRequest(lesson(),setOf(DayOfWeek.MONDAY),true,5,context.getString(R.string.lesson_break_title),true))
        timeline.saveRoutine(lesson().copy(title="Naslednja ura",startTime=LocalTime.of(8,45),endTime=LocalTime.of(9,30)))
        val items=timeline.getTimelineForDate(monday).first().filterIsInstance<ResolvedTimelineItem.Block>()
        val child=items.single { it.parentRoutineId!=null }
        assertTrue(child.companionConflict)
        assertNull(AlarmPlanner().forOccurrence(child,ZoneOffset.UTC))
        assertEquals(525,items.single { it.title=="Naslednja ura" }.startMinute)
    }
    @Test fun sleepIsExplicitSilentOvernightAndIdempotent(): Unit = runBlocking {
        assertFalse(patterns.sleep.first().enabled)
        assertTrue(timeline.snapshot(monday,monday).routines.isEmpty())
        saveSleep(sleep())
        saveSleep(sleep())
        assertEquals(sleep(),patterns.sleep.first())
        val rows=timeline.getTimelineForDate(monday).first().filterIsInstance<ResolvedTimelineItem.Block>()
        val night=rows.single { it.origin==RoutineOrigin.SLEEP }
        val morning=rows.single { it.origin==RoutineOrigin.MORNING_BUFFER }
        assertEquals(0,night.startMinute);assertEquals(420,night.endMinute)
        assertEquals(420,morning.startMinute);assertEquals(450,morning.endMinute)
        assertTrue(rows.all { it.isFixedCommitment && !it.isNotificationEnabled })
        assertEquals(2,timeline.snapshot(monday.minusDays(1),monday).routines.size)
    }
    @Test fun changingSleepPreservesTheAlreadyStartedNight(): Unit = runBlocking {
        saveSleep(sleep())
        clock.current=monday.atTime(1,0).toInstant(ZoneOffset.UTC)
        saveSleep(sleep(wake=LocalTime.of(8,0)))
        assertEquals(420,timeline.getTimelineForDate(monday).first().filterIsInstance<ResolvedTimelineItem.Block>().single { it.origin==RoutineOrigin.SLEEP }.endMinute)
        assertEquals(480,timeline.getTimelineForDate(monday.plusWeeks(1)).first().filterIsInstance<ResolvedTimelineItem.Block>().single { it.origin==RoutineOrigin.SLEEP }.endMinute)
    }
    @Test fun futureSleepExceptionSurvivesSettingsChange(): Unit = runBlocking {
        saveSleep(sleep())
        val root=db.routines().currentSleep().single()
        val nextSunday=monday.plusDays(6)
        timeline.cancelOccurrence(root.id,nextSunday)
        saveSleep(sleep(wake=LocalTime.of(8,0)))
        assertTrue(timeline.getTimelineForDate(nextSunday).first().isEmpty())
        assertTrue(timeline.getTimelineForDate(nextSunday.plusDays(1)).first().isEmpty())
    }
    @Test fun disabledSleepKeepsSettingsButProtectsNoFutureSlots(): Unit = runBlocking {
        saveSleep(sleep())
        saveSleep(sleep(enabled=false))
        assertFalse(patterns.sleep.first().enabled)
        assertEquals(LocalTime.of(23,0),patterns.sleep.first().bedtime)
        assertTrue(timeline.getTimelineForDate(monday).first().isEmpty())
    }
    @Test fun v6MigrationKeepsExistingScheduleAndAddsSafeDefaults(): Unit = runBlocking(Dispatchers.IO) {
        val name="patterns-${java.util.UUID.randomUUID()}.db"
        try {
            context.openOrCreateDatabase(name,Context.MODE_PRIVATE,null).use { legacy ->
                val sql=androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().context.assets.open("schema-v6.sql").bufferedReader().use { it.readText() }
                Regex("(?ms)CREATE TABLE.*?;|CREATE (?:UNIQUE )?INDEX.*?;|PRAGMA.*?;").findAll(sql).forEach { legacy.execSQL(it.value.removeSuffix(";")) }
                legacy.execSQL("INSERT INTO routine_blocks(id,subjectId,title,category,dayOfWeek,startMinutes,durationMinutes,isNotificationEnabled,minDurationMinutes,elasticity,priorityWeight,isFixedCommitment,rawDurationMinutes) VALUES(1,NULL,'Pouk','SCHOOL',1,480,45,0,45,0.0,3.0,1,45)")
            }
            val migrated=Room.databaseBuilder(context,RoutineDatabase::class.java,name).addMigrations(DatabaseMigrations.MIGRATION_6_7, DatabaseMigrations.MIGRATION_7_8, DatabaseMigrations.MIGRATION_8_9)
                .addCallback(SeedAndIntegrityCallback(context.resources)).build()
            try {
                val row=migrated.routines().get(1)!!
                assertEquals(45,row.durationMinutes);assertEquals(RoutineOrigin.USER,row.origin);assertTrue(row.isEnabled)
                assertNull(row.parentRoutineId);assertNull(row.seriesKey)
                migrated.openHelper.readableDatabase.query("PRAGMA foreign_key_check").use { assertEquals(0,it.count) }
            } finally { migrated.close() }
        } finally { context.deleteDatabase(name) }
    }
    private class MutableClock(var current: Instant): Clock() {
        override fun instant(): Instant=current
        override fun getZone(): ZoneId=ZoneOffset.UTC
        override fun withZone(zone: ZoneId): Clock=this
    }
}
