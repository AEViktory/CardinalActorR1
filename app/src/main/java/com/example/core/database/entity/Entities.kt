package com.example.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "missions")
data class MissionEntity(
    @PrimaryKey val id: String,
    val goalPrompt: String,
    val createdAt: Long,
    val status: String,
    val completedSteps: Int,
    val totalSteps: Int,
    val replanCount: Int = 0,
    val durationMs: Long = 0L,
    val summary: String? = null,
    val terminalReason: String? = null
)

@Entity(tableName = "mission_steps", primaryKeys = ["missionId", "stepId"])
data class StepEntity(
    val missionId: String,
    val stepId: String,
    val stepIndex: Int,
    val toolName: String,
    val title: String,
    val description: String,
    val paramsJson: String = "{}",
    val status: String,
    val executionStatus: String? = null,
    val attemptCount: Int = 1,
    val parentStepId: String? = null,
    val result: String? = null,
    val durationMs: Long = 0L
)

@Entity(tableName = "execution_events")
data class ExecutionEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val missionId: String,
    val stepId: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val tag: String,
    val level: String,
    val message: String
)
