package com.example.core.agent

import android.content.Context
import com.example.core.actions.ActionExecutor
import com.example.core.actions.ActionRegistry
import com.example.core.model.ExecutionLimits
import com.example.core.model.ExecutionStatus
import com.example.core.model.LLMConfig
import com.example.core.model.LogEntry
import com.example.core.model.LogLevel
import com.example.core.model.Mission
import com.example.core.model.MissionStatus
import com.example.core.model.PlanStep
import com.example.core.model.StepStatus
import com.example.core.perception.VisionEngine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID

class AgentLoop(
    private val context: Context,
    private val actionExecutor: ActionExecutor = ActionExecutor(context),
    private val visionEngine: VisionEngine = VisionEngine(context),
    private val planManager: PlanManager = PlanManager(),
    private val limits: ExecutionLimits = ExecutionLimits()
) {
    // Structured coroutine scope with SupervisorJob tied to application lifecycle
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var executionJob: Job? = null
    private val executionMutex = Mutex()

    private val _activeMission = MutableStateFlow<Mission?>(null)
    val activeMission: StateFlow<Mission?> = _activeMission.asStateFlow()

    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    // Suspendable pause coordination gate (zero CPU spin)
    private val _isPausedFlow = MutableStateFlow(false)
    val isPausedFlow: StateFlow<Boolean> = _isPausedFlow.asStateFlow()
    private var pauseGate: CompletableDeferred<Unit>? = null
    private val pauseMutex = Mutex()

    fun startMission(
        goalPrompt: String,
        config: LLMConfig,
        onCompleted: ((Mission) -> Unit)? = null
    ): Boolean {
        val current = _activeMission.value
        if (current != null && !current.isTerminal && current.status != MissionStatus.IDLE) {
            addStructuredLog(LogLevel.WARN, "Mission", "Cannot start new mission. Runtime is BUSY with mission id=${current.id} (status=${current.status}). Abort first.")
            return false
        }

        val missionId = UUID.randomUUID().toString()
        val initialMission = Mission(
            id = missionId,
            goalPrompt = goalPrompt,
            status = MissionStatus.PLANNING,
            createdAt = System.currentTimeMillis()
        )
        _activeMission.value = initialMission
        _isPausedFlow.value = false
        pauseGate = null

        addStructuredLog(LogLevel.INFO, "Mission", "id=$missionId status=PLANNING goal=\"$goalPrompt\"")

        executionJob = scope.launch {
            val startTime = System.currentTimeMillis()

            try {
                // 1. OBSERVE: Perception snapshot
                val prePerception = visionEngine.capturePerception()
                addStructuredLog(
                    LogLevel.PERCEIVE,
                    "Perception",
                    "source=${prePerception.source.name} package=${prePerception.activePackage} nodes=${prePerception.interactiveElementsCount} truncated=${prePerception.isTruncated}"
                )

                // 2. PLAN & VALIDATE: Generate plan and validate deterministically
                val planResult = planManager.createPlan(goalPrompt, prePerception, config)

                when (planResult) {
                    is PlanResult.Rejected -> {
                        val reason = "Plan validation rejected: " + planResult.errors.joinToString("; ")
                        addStructuredLog(LogLevel.ERROR, "Validation", reason)
                        transitionToTerminal(MissionStatus.FAILED, reason, startTime)
                        return@launch
                    }
                    is PlanResult.Success -> {
                        addStructuredLog(LogLevel.PLAN, "Planner", "source=${planResult.source} planSteps=${planResult.steps.size}")
                        _activeMission.update {
                            it?.copy(
                                status = MissionStatus.RUNNING,
                                steps = planResult.steps
                            )
                        }
                    }
                }

                // 3. EXECUTE LOOP with bounded limits and verification
                val planSteps = _activeMission.value?.steps?.toMutableList() ?: mutableListOf()
                var stepIndex = 0
                var currentReplanCount = 0

                while (stepIndex < planSteps.size) {
                    // Check mission timeout
                    val elapsed = System.currentTimeMillis() - startTime
                    if (elapsed > limits.missionTimeoutMs) {
                        val timeoutReason = "BUDGET_EXCEEDED: Mission exceeded maximum duration (${limits.missionTimeoutMs / 1000}s)"
                        addStructuredLog(LogLevel.ERROR, "Mission", timeoutReason)
                        transitionToTerminal(MissionStatus.FAILED, timeoutReason, startTime)
                        return@launch
                    }

                    // Await cooperative unpause if paused
                    checkPauseGate()

                    val currentStep = planSteps[stepIndex]

                    // Mark step RUNNING
                    planSteps[stepIndex] = currentStep.copy(status = StepStatus.RUNNING)
                    _activeMission.update {
                        it?.copy(
                            steps = planSteps.toList(),
                            currentStepIndex = stepIndex,
                            durationMs = System.currentTimeMillis() - startTime
                        )
                    }

                    addStructuredLog(LogLevel.EXEC, "Execution", "stepId=${currentStep.id} tool=${currentStep.toolName} attempt=${currentStep.attemptCount}")

                    // Pre-action perception
                    val stepPrePerception = visionEngine.capturePerception()

                    // Execute tool with timeout
                    val stepStart = System.currentTimeMillis()
                    val executionResult = withTimeoutOrNull(currentStep.params["timeout_ms"]?.toLongOrNull() ?: limits.defaultToolTimeoutMs) {
                        actionExecutor.execute(currentStep.toolName, currentStep.params)
                    } ?: com.example.core.actions.ActionResult(
                        isSuccess = false,
                        executionStatus = ExecutionStatus.EXECUTION_FAILED,
                        message = "Tool execution timed out after ${limits.defaultToolTimeoutMs / 1000}s",
                        evidence = "TIMEOUT_UNCERTAIN_EFFECT"
                    )

                    val stepDuration = System.currentTimeMillis() - stepStart

                    // Post-action perception
                    val stepPostPerception = visionEngine.capturePerception()

                    // 4. VERIFY & EVALUATE
                    val evaluation = planManager.evaluateStep(
                        step = currentStep.copy(durationMs = stepDuration),
                        result = executionResult,
                        prePerception = stepPrePerception,
                        postPerception = stepPostPerception,
                        goal = goalPrompt,
                        config = config,
                        currentReplanCount = currentReplanCount,
                        limits = limits
                    )

                    when (evaluation) {
                        is StepEvaluationResult.Success -> {
                            val ver = evaluation.verification
                            addStructuredLog(
                                LogLevel.INFO,
                                "Verification",
                                "stepId=${currentStep.id} result=${ver.status.name} evidence=\"${ver.evidence}\""
                            )
                            planSteps[stepIndex] = evaluation.updatedStep
                            stepIndex++
                        }

                        is StepEvaluationResult.Replanned -> {
                            currentReplanCount++
                            addStructuredLog(
                                LogLevel.WARN,
                                "Recovery",
                                "stepId=${currentStep.id} strategy=REPLAN replacementTool=${evaluation.replacementStep.toolName} replansTotal=$currentReplanCount"
                            )
                            planSteps[stepIndex] = evaluation.failedStep
                            // Inject replanned step right after failed step
                            planSteps.add(stepIndex + 1, evaluation.replacementStep)
                            _activeMission.update { it?.copy(replanCount = currentReplanCount) }
                            stepIndex++
                        }

                        is StepEvaluationResult.BudgetExceeded -> {
                            addStructuredLog(LogLevel.ERROR, "Mission", evaluation.reason)
                            planSteps[stepIndex] = evaluation.failedStep
                            transitionToTerminal(MissionStatus.FAILED, evaluation.reason, startTime)
                            return@launch
                        }

                        is StepEvaluationResult.Failed -> {
                            addStructuredLog(LogLevel.ERROR, "Execution", "stepId=${currentStep.id} result=EXECUTION_FAILED reason=\"${evaluation.reason}\"")
                            planSteps[stepIndex] = evaluation.failedStep
                            transitionToTerminal(MissionStatus.FAILED, evaluation.reason, startTime)
                            return@launch
                        }
                    }

                    _activeMission.update {
                        it?.copy(
                            steps = planSteps.toList(),
                            durationMs = System.currentTimeMillis() - startTime
                        )
                    }

                    // Brief settling delay
                    delay(400)
                }

                // Invariant E: Validate mission terminal outcome
                val failedStepsCount = planSteps.count { it.status == StepStatus.FAILED }
                if (failedStepsCount > 0) {
                    val reason = "STEPS_FAILED: $failedStepsCount step(s) failed execution"
                    transitionToTerminal(MissionStatus.FAILED, reason, startTime)
                    return@launch
                }

                // Check for mandatory unverified steps (sensitive actions, external side-effects, or explicit expectedOutcome)
                val unverifiedMandatorySteps = planSteps.filter { step ->
                    if (step.executionStatus == ExecutionStatus.NOT_VERIFIED) {
                        val action = ActionRegistry.getAction(step.toolName)
                        action?.riskLevel == com.example.core.actions.RiskLevel.SENSITIVE ||
                                action?.riskLevel == com.example.core.actions.RiskLevel.EXTERNAL_EFFECT ||
                                step.expectedOutcome.isNotBlank()
                    } else false
                }

                val finalMission: Mission
                if (unverifiedMandatorySteps.isNotEmpty()) {
                    val reason = "UNVERIFIED_POSTCONDITION"
                    val summary = "Mission finished execution, but ${unverifiedMandatorySteps.size} mandatory step(s) have unverified postconditions: " +
                            unverifiedMandatorySteps.joinToString { it.toolName }
                    finalMission = _activeMission.value?.copy(
                        status = MissionStatus.FAILED,
                        durationMs = System.currentTimeMillis() - startTime,
                        summary = summary,
                        terminalReason = reason
                    ) ?: return@launch
                    _activeMission.value = finalMission
                    addStructuredLog(
                        LogLevel.WARN,
                        "Mission",
                        "id=$missionId status=FAILED reason=UNVERIFIED_POSTCONDITION durationMs=${System.currentTimeMillis() - startTime}"
                    )
                } else {
                    val unverifiedCount = planSteps.count { it.executionStatus == ExecutionStatus.NOT_VERIFIED }
                    val summary = if (unverifiedCount > 0) {
                        "Mission completed with ${planSteps.size} step(s). Notice: $unverifiedCount step(s) completed without verifiable postconditions (system panel/intents)."
                    } else {
                        "Mission successfully verified all ${planSteps.size} steps."
                    }
                    finalMission = _activeMission.value?.copy(
                        status = MissionStatus.COMPLETED,
                        durationMs = System.currentTimeMillis() - startTime,
                        summary = summary,
                        terminalReason = "GOAL_COMPLETED"
                    ) ?: return@launch
                    _activeMission.value = finalMission
                    addStructuredLog(
                        LogLevel.INFO,
                        "Mission",
                        "id=$missionId status=COMPLETED reason=GOAL_COMPLETED durationMs=${System.currentTimeMillis() - startTime}"
                    )
                }

                onCompleted?.invoke(finalMission)

            } catch (e: CancellationException) {
                val duration = System.currentTimeMillis() - startTime
                addStructuredLog(LogLevel.WARN, "Mission", "id=$missionId status=ABORTED reason=\"Mission cancelled by user\" durationMs=$duration")
                _activeMission.update {
                    it?.copy(
                        status = MissionStatus.ABORTED,
                        durationMs = duration,
                        terminalReason = "USER_CANCELLED"
                    )
                }
                throw e // Propagate cancellation cleanly
            } catch (e: Exception) {
                val duration = System.currentTimeMillis() - startTime
                val errorMsg = "Internal runtime failure: ${e.message}"
                addStructuredLog(LogLevel.ERROR, "Mission", "id=$missionId status=FAILED reason=\"$errorMsg\" durationMs=$duration")
                _activeMission.update {
                    it?.copy(
                        status = MissionStatus.FAILED,
                        durationMs = duration,
                        terminalReason = errorMsg
                    )
                }
            }
        }
        return true
    }

    suspend fun pauseMission(): Boolean {
        pauseMutex.withLock {
            val mission = _activeMission.value ?: return false
            if (mission.isTerminal || mission.status != MissionStatus.RUNNING) return false

            _isPausedFlow.value = true
            if (pauseGate == null || pauseGate?.isCompleted == true) {
                pauseGate = CompletableDeferred()
            }
            _activeMission.update { it?.copy(status = MissionStatus.PAUSED) }
            addStructuredLog(LogLevel.WARN, "Mission", "id=${mission.id} status=PAUSED reason=USER_PAUSED")
            return true
        }
    }

    suspend fun resumeMission(): Boolean {
        pauseMutex.withLock {
            val mission = _activeMission.value ?: return false
            if (mission.isTerminal || mission.status != MissionStatus.PAUSED) return false

            _isPausedFlow.value = false
            pauseGate?.complete(Unit)
            pauseGate = null
            _activeMission.update { it?.copy(status = MissionStatus.RUNNING) }
            addStructuredLog(LogLevel.INFO, "Mission", "id=${mission.id} status=RUNNING reason=USER_RESUMED")
            return true
        }
    }

    fun cancelCurrentMission() {
        val mission = _activeMission.value
        if (mission != null && !mission.isTerminal) {
            // Unpause gate so cancelled coroutine can exit immediately
            pauseGate?.complete(Unit)
            executionJob?.cancel(CancellationException("Mission cancelled via user action."))
        }
    }

    private suspend fun checkPauseGate() {
        if (_isPausedFlow.value) {
            pauseGate?.await()
        }
    }

    private fun transitionToTerminal(terminalStatus: MissionStatus, reason: String, startTime: Long) {
        val duration = System.currentTimeMillis() - startTime
        _activeMission.update {
            it?.copy(
                status = terminalStatus,
                durationMs = duration,
                terminalReason = reason
            )
        }
    }

    private fun addStructuredLog(level: LogLevel, tag: String, message: String) {
        // Redact any potential accidental API key sequences
        val sanitized = message.replace(Regex("(?i)(key|token|auth)=([a-zA-Z0-9_\\-]{10,})"), "$1=[REDACTED]")
        val entry = LogEntry(tag = tag, message = sanitized, level = level)

        _logs.update { current ->
            val updated = current.toMutableList()
            updated.add(entry)
            if (updated.size > 250) updated.removeAt(0)
            updated
        }

        _activeMission.update { m ->
            if (m != null) {
                val missionLogs = m.thoughtLog.toMutableList()
                missionLogs.add(entry)
                m.copy(thoughtLog = missionLogs.takeLast(60))
            } else null
        }
    }
}
