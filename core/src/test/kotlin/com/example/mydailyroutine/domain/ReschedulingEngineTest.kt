package com.example.mydailyroutine.domain

import com.example.mydailyroutine.domain.model.RoutineCategory
import com.example.mydailyroutine.domain.planning.*
import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class ReschedulingEngineTest {
    private val engine = DeterministicReschedulingEngine()
    private fun task(id: String, start: Int, duration: Int, min: Int = 25, e: Double = 1.0, priority: Double = 1.0,
        category: RoutineCategory = RoutineCategory.FOCUS_ANALYTICAL, fixed: Boolean = false) = TimeBlock(id,id,category,start,duration,min,fixed,e,priority)
    private fun fixed(at: Int) = task("fixed", at, 45, 45, 0.0, 10.0, RoutineCategory.SCHOOL, true)
    @Test fun `slack absorbs delay without shortening`() {
        val original = listOf(task("a",840,45), fixed(960))
        val result = engine.recover(original,840,45)
        assertEquals(45, result.blocks.first { it.id=="a" }.durationMinutes)
        assertEquals(885, result.blocks.first { it.id=="a" }.startMinutes)
        assertEquals(45, result.report.slackUsedMinutes)
        assertEquals(original.last(), result.blocks.first { it.isFixed })
    }
    @Test fun `reserve is consumed before focus compression`() {
        val original = listOf(task("a",840,90), task("reserve",930,30,0,3.0,1.0,RoutineCategory.EMERGENCY_RESERVE), fixed(960))
        val result = engine.recover(original,840,30)
        assertEquals(30, result.report.bufferUsedMinutes)
        assertEquals(0, result.report.compressionMinutes)
        assertFalse(result.blocks.any { it.id=="reserve" })
        assertTrue(result.deferred.isEmpty())
    }
    @Test fun `45 minute slip uses buffer then weighted elastic compression`() {
        val items = listOf(task("a",840,60,30,1.0),task("b",900,30,15,2.0),task("reserve",930,30,0,3.0,1.0,RoutineCategory.EMERGENCY_RESERVE),fixed(960))
        val result = engine.recover(items,840,45)
        assertEquals(30,result.report.bufferUsedMinutes)
        assertEquals(15,result.report.compressionMinutes)
        assertEquals(55,result.blocks.single { it.id=="a" }.durationMinutes)
        assertEquals(20,result.blocks.single { it.id=="b" }.durationMinutes)
        assertEquals(960,result.blocks.filterNot { it.isFixed }.maxOf { it.endMinutes })
    }
    @Test fun `zero elasticity never compresses`() {
        val items=listOf(task("rigid-duration",840,60,25,0.0,5.0),task("flex",900,60,25,1.0),fixed(960))
        val result=engine.recover(items,840,30)
        assertEquals(60,result.blocks.single { it.id=="rigid-duration" }.durationMinutes)
        assertEquals(30,result.blocks.single { it.id=="flex" }.durationMinutes)
    }
    @Test fun `low utility task is deferred and original work is preserved`() {
        val items=listOf(task("important",840,60,40,1.0,10.0),task("optional",900,60,40,1.0,1.0),fixed(960))
        val result=engine.recover(items,840,60)
        assertEquals(listOf("optional"),result.deferred.map { it.id })
        assertEquals(60,result.deferred.single().durationMinutes)
        assertEquals(items.last(),result.blocks.single { it.isFixed })
    }
    @Test fun `extreme integer slip never overflows or moves fixed commitments`() {
        val items=listOf(task("a",840,90),fixed(960))
        val result=engine.recover(items,840,Int.MAX_VALUE)
        assertEquals(listOf("a"),result.deferred.map { it.id })
        assertEquals(listOf(items.last()),result.blocks)
        assertTrue(result.report.boundaryAlreadyPassed)
    }
    @Test fun `midnight crossing uses extended minutes rather than wrapping to yesterday`() {
        val items=listOf(task("night",1410,90,30),fixed(1560))
        val result=engine.recover(items,1410,45,2880)
        assertEquals(1455,result.blocks.first { it.id=="night" }.startMinutes)
        assertEquals(1545,result.blocks.first { it.id=="night" }.endMinutes)
    }
    @Test fun `current fixed commitment is not invaded`() {
        val items=listOf(task("late",790,30),task("school",780,120,120,0.0,10.0,RoutineCategory.SCHOOL,true))
        val result=engine.recover(items,840,20)
        assertEquals(listOf("late"),result.deferred.map { it.id })
        assertEquals(items[1],result.blocks.single())
    }
    @Test fun `completed work is excluded and input is immutable`() {
        val done=task("done",800,40).copy(completedActualMinutes=50)
        val items=listOf(done,task("next",840,90),fixed(960));val copy=items.toList()
        val result=engine.recover(items,840,45)
        assertEquals(copy,items)
        assertEquals(done,result.blocks.single { it.id=="done" })
    }
    @Test fun `integer compression sums exactly after saturated tasks are removed`() {
        val result=ElasticCompression.reductions(listOf(task("a",0,60,59,10.0),task("b",60,60,25,1.0),task("c",120,60,25,1.0)),31)
        assertEquals(31,result.values.sum());assertEquals(1,result["a"])
        assertEquals(15,result["b"]);assertEquals(15,result["c"])
    }
    @Test fun `largest remainder ties are stable`() {
        val input=listOf(task("c",0,30,1),task("a",30,30,1),task("b",60,30,1))
        assertEquals(mapOf("a" to 1,"b" to 1,"c" to 0),ElasticCompression.reductions(input,2))
    }
    @Test fun `seeded randomized compression never drifts below bounds`() {
        val random=Random(42)
        repeat(200) {
            val items=List(1+random.nextInt(20)) { i -> val duration=random.nextInt(25,91); task("$i",i*90,duration,random.nextInt(1,duration),random.nextDouble(0.01,10.0)) }
            val capacity=items.sumOf { it.durationMinutes-it.minDurationMinutes };val requested=random.nextInt(0,capacity+50)
            val allocation=ElasticCompression.reductions(items,requested)
            assertEquals(minOf(capacity,requested),allocation.values.sum())
            items.forEach { assertTrue((allocation[it.id]?:0) in 0..(it.durationMinutes-it.minDurationMinutes)) }
        }
    }
    @Test fun `zero minimum task goes to backlog instead of becoming a zero length ghost`() {
        val input=listOf(task("optional",840,60,0),fixed(900))
        val result=engine.recover(input,840,60)
        assertEquals(listOf("optional"),result.deferred.map { it.id })
        assertTrue(result.blocks.none { !it.isFixed && it.durationMinutes==0 })
    }
    @Test fun `output independent of input order`() {
        val tasks=listOf(task("a",840,60,25,1.0,3.0),task("b",900,60,25,2.0,1.0),fixed(960))
        assertEquals(engine.recover(tasks,840,50),engine.recover(tasks.reversed(),840,50))
    }
}
