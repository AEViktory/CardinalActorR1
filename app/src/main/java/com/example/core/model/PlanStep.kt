package com.example.core.model

enum class StepStatus {
    PENDING,
    VALIDATING,
    RUNNING,
    VERIFYING,
    SUCCESS,
    FAILED,
    REPLANNED,
    SKIPPED,
    REJECTED
}

enum class ExecutionStatus(val displayName: String) {
    EXECUTION_SUCCEEDED("Executed Technically"),
    VERIFIED("Goal Postcondition Verified"),
    NOT_VERIFIED("Action Performed, Unverified Postcondition"),
    VERIFICATION_FAILED("Postcondition Check Failed"),
    NEEDS_USER_CONFIRMATION("Awaiting User Confirmation"),
    EXECUTION_FAILED("Tool Invocation Failed")
}

data class PlanStep(
    val id: String,
    val stepIndex: Int,
    val toolName: String,
    val title: String,
    val description: String,
    val params: Map<String, String> = emptyMap(),
    val expectedOutcome: String = "",
    val status: StepStatus = StepStatus.PENDING,
    val executionStatus: ExecutionStatus? = null,
    val attemptCount: Int = 1,
    val parentStepId: String? = null,
    val result: String? = null,
    val durationMs: Long = 0L
)
