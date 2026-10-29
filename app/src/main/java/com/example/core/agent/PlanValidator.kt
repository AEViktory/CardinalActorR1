package com.example.core.agent

import com.example.core.actions.ActionRegistry
import com.example.core.model.ExecutionLimits
import com.example.core.model.PlanStep
import com.example.core.model.StepStatus

data class PlanValidationResult(
    val isValid: Boolean,
    val errors: List<String> = emptyList(),
    val validatedSteps: List<PlanStep> = emptyList()
)

class PlanValidator(private val limits: ExecutionLimits = ExecutionLimits()) {

    fun validatePlan(steps: List<PlanStep>): PlanValidationResult {
        val errors = mutableListOf<String>()

        if (steps.isEmpty()) {
            return PlanValidationResult(isValid = false, errors = listOf("Plan contains zero steps."))
        }

        if (steps.size > limits.maxSteps) {
            errors.add("Plan exceeds maximum allowed steps (${steps.size} > ${limits.maxSteps}).")
        }

        val validatedList = mutableListOf<PlanStep>()

        for ((index, step) in steps.withIndex()) {
            val stepErrors = validateStep(step, index + 1)
            if (stepErrors.isNotEmpty()) {
                errors.addAll(stepErrors)
            } else {
                validatedList.add(step.copy(stepIndex = index + 1))
            }
        }

        return PlanValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            validatedSteps = if (errors.isEmpty()) validatedList else emptyList()
        )
    }

    fun validateStep(step: PlanStep, expectedIndex: Int = step.stepIndex): List<String> {
        val errors = mutableListOf<String>()
        val action = ActionRegistry.getAction(step.toolName)

        if (action == null) {
            errors.add("Step $expectedIndex references unregistered tool: '${step.toolName}'.")
            return errors
        }

        // Validate required parameters
        for (paramSpec in action.parameters) {
            val value = step.params[paramSpec.name]
            if (paramSpec.isRequired && (value == null || value.isBlank())) {
                errors.add("Step $expectedIndex [${step.toolName}]: Missing required parameter '${paramSpec.name}'.")
                continue
            }

            if (value != null && value.isNotBlank()) {
                if (value.length > paramSpec.maxLength) {
                    errors.add("Step $expectedIndex [${step.toolName}]: Parameter '${paramSpec.name}' exceeds max length (${value.length} > ${paramSpec.maxLength}).")
                }

                when (paramSpec.type) {
                    "number" -> {
                        if (value.toLongOrNull() == null && value.toDoubleOrNull() == null) {
                            errors.add("Step $expectedIndex [${step.toolName}]: Parameter '${paramSpec.name}' expects a number, got '$value'.")
                        }
                    }
                    "boolean" -> {
                        val lower = value.lowercase()
                        if (lower != "true" && lower != "false") {
                            errors.add("Step $expectedIndex [${step.toolName}]: Parameter '${paramSpec.name}' expects a boolean, got '$value'.")
                        }
                    }
                }

                if (paramSpec.allowedValues != null && !paramSpec.allowedValues.contains(value)) {
                    errors.add("Step $expectedIndex [${step.toolName}]: Parameter '${paramSpec.name}' has invalid value '$value'. Allowed: ${paramSpec.allowedValues}.")
                }
            }
        }

        return errors
    }
}
