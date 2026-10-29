package com.example.core.actions

import com.example.core.model.ExecutionStatus

enum class ActionCategory(val displayName: String) {
    SYSTEM("System & Settings"),
    HARDWARE("Hardware & Sensors"),
    COMMUNICATION("Messaging & Calls"),
    AUTOMATION("Accessibility & Gestures"),
    PRODUCTIVITY("Alarms & Apps"),
    UTILITY("Cardinal Utilities")
}

enum class RiskLevel(val displayName: String) {
    READ_ONLY("Read Only"),
    REVERSIBLE("Reversible Action"),
    EXTERNAL_EFFECT("External Device Effect"),
    SENSITIVE("Sensitive / User Confirmation Required")
}

data class ActionParameter(
    val name: String,
    val type: String = "string", // "string", "number", "boolean"
    val description: String,
    val isRequired: Boolean = true,
    val defaultValue: String? = null,
    val maxLength: Int = 200,
    val allowedValues: List<String>? = null
)

data class ActionResult(
    val isSuccess: Boolean,
    val executionStatus: ExecutionStatus = if (isSuccess) ExecutionStatus.EXECUTION_SUCCEEDED else ExecutionStatus.EXECUTION_FAILED,
    val message: String,
    val evidence: String? = null,
    val outputData: Map<String, Any> = emptyMap(),
    val errorDetails: String? = null
)

data class ToolInvocation(
    val missionId: String,
    val stepId: String,
    val toolId: String,
    val validatedParams: Map<String, String>,
    val attempt: Int = 1,
    val timeoutMs: Long = 15_000L,
    val timestamp: Long = System.currentTimeMillis()
)

data class CardinalAction(
    val id: String,
    val name: String,
    val category: ActionCategory,
    val description: String,
    val riskLevel: RiskLevel = RiskLevel.REVERSIBLE,
    val parameters: List<ActionParameter> = emptyList(),
    val requiredPermission: String? = null,
    val isIdempotent: Boolean = true,
    val timeoutMs: Long = 15_000L,
    val expectedPostcondition: String = ""
)
