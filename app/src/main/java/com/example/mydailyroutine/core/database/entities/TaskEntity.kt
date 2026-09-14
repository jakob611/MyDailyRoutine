package com.example.mydailyroutine.core.database.entities

import androidx.room.*
import java.time.LocalDate

@Entity(tableName = "tasks", foreignKeys = [ForeignKey(entity = SubjectEntity::class,
    parentColumns = ["id"], childColumns = ["subjectId"], onDelete = ForeignKey.SET_NULL)],
    indices = [Index("subjectId"), Index("dueDate")])
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subjectId: Long?, val title: String,
    val dueDate: LocalDate?, val note: String?,
    val createdAtEpochMillis: Long, val completedAtEpochMillis: Long?,
)
