package com.example.core.model

enum class MissionStatus {
    IDLE,
    PLANNING,
    RUNNING,
    PAUSED,
    COMPLETED,
    FAILED,
    ABORTED
}

data class Mission(
    val id: String,
    val goalPrompt: String,
    val createdAt: Long = System.currentTimeMillis(),
    val status: MissionStatus = MissionStatus.IDLE,
    val steps: List<PlanStep> = emptyList(),
    val thoughtLog: List<LogEntry> = emptyList(),
    val currentStepIndex: Int = 0,
    val replanCount: Int = 0,
    val durationMs: Long = 0L,
    val summary: String? = null,
    val terminalReason: String? = null
) {
    val isTerminal: Boolean
        get() = status == MissionStatus.COMPLETED || status == MissionStatus.FAILED || status == MissionStatus.ABORTED

    val totalSteps: Int get() = steps.size
    val completedSteps: Int get() = steps.count { it.status == StepStatus.SUCCESS }
    val progress: Float get() = if (steps.isEmpty()) 0f else completedSteps.toFloat() / steps.size
}
