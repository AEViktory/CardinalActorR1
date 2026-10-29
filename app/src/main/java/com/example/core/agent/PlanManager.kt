package com.example.core.agent

import com.example.core.actions.ActionResult
import com.example.core.actions.ActionRegistry
import com.example.core.llm.GeminiProvider
import com.example.core.llm.LLMClient
import com.example.core.model.ExecutionLimits
import com.example.core.model.ExecutionStatus
import com.example.core.model.LLMConfig
import com.example.core.model.PerceptionContext
import com.example.core.model.PlanStep
import com.example.core.model.StepStatus

sealed interface PlanResult {
    data class Success(val steps: List<PlanStep>, val source: String) : PlanResult
    data class Rejected(val errors: List<String>) : PlanResult
}

sealed interface StepEvaluationResult {
    data class Success(val updatedStep: PlanStep, val verification: VerificationResult) : StepEvaluationResult
    data class Replanned(val failedStep: PlanStep, val replacementStep: PlanStep) : StepEvaluationResult
    data class Failed(val failedStep: PlanStep, val reason: String) : StepEvaluationResult
    data class BudgetExceeded(val failedStep: PlanStep, val reason: String) : StepEvaluationResult
}

class PlanManager(
    private val llmClient: LLMClient = GeminiProvider(),
    private val validator: PlanValidator = PlanValidator(),
    private val verifier: ResultVerifier = ResultVerifier()
) {

    suspend fun createPlan(
        goal: String,
        perception: PerceptionContext,
        config: LLMConfig
    ): PlanResult {
        val rawSteps = llmClient.generatePlan(goal, perception, config)
        val validation = validator.validatePlan(rawSteps)

        return if (validation.isValid) {
            val source = if (config.apiKey.isNotBlank()) "LLM (${config.provider.displayName})" else "DETERMINISTIC_FALLBACK"
            PlanResult.Success(validation.validatedSteps, source)
        } else {
            PlanResult.Rejected(validation.errors)
        }
    }

    suspend fun evaluateStep(
        step: PlanStep,
        result: ActionResult,
        prePerception: PerceptionContext,
        postPerception: PerceptionContext,
        goal: String,
        config: LLMConfig,
        currentReplanCount: Int,
        limits: ExecutionLimits = ExecutionLimits()
    ): StepEvaluationResult {
        // Run rigorous postcondition verification
        val verification = verifier.verifyPostcondition(
            step = step,
            executionResult = result,
            prePerception = prePerception,
            postPerception = postPerception
        )

        // If technical execution succeeded and verification did NOT actively fail:
        if (result.isSuccess && verification.status != ExecutionStatus.VERIFICATION_FAILED) {
            val finalStatus = if (verification.isVerified) StepStatus.SUCCESS else StepStatus.SUCCESS
            return StepEvaluationResult.Success(
                updatedStep = step.copy(
                    status = finalStatus,
                    executionStatus = verification.status,
                    result = "${result.message} [${verification.evidence}]"
                ),
                verification = verification
            )
        }

        // Failure handling (either technical execution failure or postcondition check failed)
        val failureReason = if (!result.isSuccess) {
            result.message
        } else {
            "Verification Failed: ${verification.evidence}"
        }

        val failedExecutionStatus = if (result.isSuccess) {
            ExecutionStatus.VERIFICATION_FAILED
        } else {
            result.executionStatus
        }

        // Budget thresholds before replanning
        if (currentReplanCount >= limits.maxReplansPerMission) {
            return StepEvaluationResult.BudgetExceeded(
                failedStep = step.copy(
                    status = StepStatus.FAILED,
                    executionStatus = failedExecutionStatus,
                    result = "Budget exceeded: Reached maximum replans (${limits.maxReplansPerMission}) [Reason: $failureReason]"
                ),
                reason = "BUDGET_EXCEEDED: Reached maximum replan limit (${limits.maxReplansPerMission}) for mission."
            )
        }

        // Non-idempotent or timeout uncertain effect check: prevent duplicate external side-effects
        val action = ActionRegistry.getAction(step.toolName)
        val isNonIdempotentOrUncertain = (action?.isIdempotent == false) ||
                (result.evidence == "TIMEOUT_UNCERTAIN_EFFECT")

        if (step.attemptCount >= limits.maxRetriesPerStep && isNonIdempotentOrUncertain) {
            return StepEvaluationResult.Failed(
                failedStep = step.copy(
                    status = StepStatus.FAILED,
                    executionStatus = failedExecutionStatus,
                    result = "Action '${step.toolName}' is non-idempotent or timed out with uncertain effect. Retry prohibited to prevent duplicate side effects. [Reason: $failureReason]"
                ),
                reason = "NON_IDEMPOTENT_RETRY_BLOCKED: Cannot safely retry '${step.toolName}' without risking duplicate external side effects."
            )
        }

        if (step.attemptCount >= limits.maxRetriesPerStep) {
            return StepEvaluationResult.Failed(
                failedStep = step.copy(
                    status = StepStatus.FAILED,
                    executionStatus = failedExecutionStatus,
                    result = "Exceeded max attempts (${limits.maxRetriesPerStep}) on step '${step.toolName}'. [Reason: $failureReason]"
                ),
                reason = "Step attempts exhausted (${step.attemptCount}/${limits.maxRetriesPerStep})."
            )
        }

        // Request replan from provider
        val replannedStep = llmClient.replanStep(
            goal = goal,
            failedStep = step,
            failureReason = failureReason,
            perception = postPerception,
            config = config
        )

        if (replannedStep != null) {
            val stepErrors = validator.validateStep(replannedStep, step.stepIndex)
            if (stepErrors.isEmpty()) {
                val validatedReplacement = replannedStep.copy(
                    stepIndex = step.stepIndex + 1,
                    status = StepStatus.PENDING,
                    attemptCount = 1,
                    parentStepId = step.id
                )
                return StepEvaluationResult.Replanned(
                    failedStep = step.copy(
                        status = StepStatus.REPLANNED,
                        executionStatus = failedExecutionStatus,
                        result = failureReason
                    ),
                    replacementStep = validatedReplacement
                )
            }
        }

        return StepEvaluationResult.Failed(
            failedStep = step.copy(
                status = StepStatus.FAILED,
                executionStatus = failedExecutionStatus,
                result = failureReason
            ),
            reason = failureReason
        )
    }
}
