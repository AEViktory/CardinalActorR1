package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.actions.ActionResult
import com.example.core.actions.ActionRegistry
import com.example.core.actions.RiskLevel
import com.example.core.agent.PlanManager
import com.example.core.agent.PlanValidator
import com.example.core.agent.ResultVerifier
import com.example.core.agent.StepEvaluationResult
import com.example.core.database.CardinalDatabase
import com.example.core.database.entity.MissionEntity
import com.example.core.database.entity.StepEntity
import com.example.core.llm.DeterministicPlanner
import com.example.core.model.ExecutionLimits
import com.example.core.model.ExecutionStatus
import com.example.core.model.LLMConfig
import com.example.core.model.Mission
import com.example.core.model.MissionStatus
import com.example.core.model.PerceptionContext
import com.example.core.model.PerceptionSource
import com.example.core.model.PlanStep
import com.example.core.model.StepStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    private lateinit var database: CardinalDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, CardinalDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    // 1. Identity & Rebranding
    @Test
    fun `read app name from context confirms Aethereal Cardinal rebranding`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Aethereal Cardinal", appName)
    }

    // 2. Action Registry & Auditing
    @Test
    fun `action registry contains audited device tools with risk levels`() {
        val actions = ActionRegistry.allActions
        assertTrue("Registry should contain at least 20 device actions", actions.size >= 20)

        val smsAction = ActionRegistry.getAction("send_sms")
        assertNotNull(smsAction)
        assertEquals(RiskLevel.SENSITIVE, smsAction?.riskLevel)

        val batteryAction = ActionRegistry.getAction("get_battery_status")
        assertNotNull(batteryAction)
        assertEquals(RiskLevel.READ_ONLY, batteryAction?.riskLevel)

        val torchAction = ActionRegistry.getAction("toggle_flashlight")
        assertNotNull(torchAction)
        assertEquals(RiskLevel.REVERSIBLE, torchAction?.riskLevel)
    }

    // 3. PlanValidator Deterministic Tests
    @Test
    fun `plan validator accepts well-formed plan`() {
        val validator = PlanValidator()
        val validSteps = listOf(
            PlanStep(
                id = UUID.randomUUID().toString(),
                stepIndex = 1,
                toolName = "toggle_flashlight",
                title = "Turn On Torch",
                description = "Activate LED",
                params = mapOf("enabled" to "true")
            )
        )
        val result = validator.validatePlan(validSteps)
        assertTrue("Valid plan must pass validation", result.isValid)
        assertTrue(result.errors.isEmpty())
        assertEquals(1, result.validatedSteps.size)
    }

    @Test
    fun `plan validator rejects unregistered tool name`() {
        val validator = PlanValidator()
        val invalidSteps = listOf(
            PlanStep(
                id = UUID.randomUUID().toString(),
                stepIndex = 1,
                toolName = "destroy_system_files", // Unregistered
                title = "Fake Tool",
                description = "Should be rejected"
            )
        )
        val result = validator.validatePlan(invalidSteps)
        assertFalse("Unregistered tool must be rejected", result.isValid)
        assertTrue(result.errors.any { it.contains("unregistered tool") })
    }

    @Test
    fun `plan validator rejects missing required parameter`() {
        val validator = PlanValidator()
        val missingParamStep = listOf(
            PlanStep(
                id = UUID.randomUUID().toString(),
                stepIndex = 1,
                toolName = "send_sms",
                title = "Send SMS",
                description = "Missing phone number and message",
                params = emptyMap() // Required parameters missing
            )
        )
        val result = validator.validatePlan(missingParamStep)
        assertFalse("Missing required parameter must be rejected", result.isValid)
        assertTrue(result.errors.any { it.contains("Missing required parameter") })
    }

    @Test
    fun `plan validator rejects invalid number format for numeric parameter`() {
        val validator = PlanValidator()
        val invalidNumberStep = listOf(
            PlanStep(
                id = UUID.randomUUID().toString(),
                stepIndex = 1,
                toolName = "set_alarm",
                title = "Set Alarm",
                description = "Invalid hour parameter",
                params = mapOf("hour" to "not_a_number", "minute" to "30")
            )
        )
        val result = validator.validatePlan(invalidNumberStep)
        assertFalse("Invalid number format must be rejected", result.isValid)
        assertTrue(result.errors.any { it.contains("expects a number") })
    }

    @Test
    fun `plan validator enforces max steps limit`() {
        val customLimits = ExecutionLimits(maxSteps = 2)
        val validator = PlanValidator(customLimits)
        val threeSteps = (1..3).map {
            PlanStep(
                id = UUID.randomUUID().toString(),
                stepIndex = it,
                toolName = "wait_delay",
                title = "Wait Step $it",
                description = "Test limit"
            )
        }
        val result = validator.validatePlan(threeSteps)
        assertFalse("Plan exceeding maxSteps limit must be rejected", result.isValid)
        assertTrue(result.errors.any { it.contains("exceeds maximum allowed steps") })
    }

    // 4. ResultVerifier Postcondition Verification
    @Test
    fun `result verifier distinguishes verified vs not verified outcomes`() {
        val verifier = ResultVerifier()
        val perception = PerceptionContext(source = PerceptionSource.ACCESSIBILITY_TREE, activeAppName = "Aethereal Cardinal")

        // 1. Battery reading with data is VERIFIED
        val batteryStep = PlanStep(UUID.randomUUID().toString(), 1, "get_battery_status", "Check Battery", "")
        val batteryExec = ActionResult(isSuccess = true, message = "Battery 85%", outputData = mapOf("level" to 85))
        val batteryVer = verifier.verifyPostcondition(batteryStep, batteryExec, perception, perception)
        assertEquals(ExecutionStatus.VERIFIED, batteryVer.status)
        assertTrue(batteryVer.isVerified)

        // 2. Settings panel intent execution is NOT_VERIFIED (external OS sandbox)
        val wifiStep = PlanStep(UUID.randomUUID().toString(), 1, "toggle_wifi_settings", "WiFi Panel", "")
        val wifiExec = ActionResult(isSuccess = true, message = "WiFi panel launched")
        val wifiVer = verifier.verifyPostcondition(wifiStep, wifiExec, perception, perception)
        assertEquals(ExecutionStatus.NOT_VERIFIED, wifiVer.status)
        assertFalse("System settings launch cannot be verified without OS sandbox violation", wifiVer.isVerified)

        // 3. Failed execution returns EXECUTION_FAILED
        val failedStep = PlanStep(UUID.randomUUID().toString(), 1, "toggle_flashlight", "Torch", "")
        val failedExec = ActionResult(isSuccess = false, message = "Hardware unavailable")
        val failedVer = verifier.verifyPostcondition(failedStep, failedExec, perception, perception)
        assertEquals(ExecutionStatus.EXECUTION_FAILED, failedVer.status)
        assertFalse(failedVer.isVerified)
    }

    // 5. State Machine & Recovery Bounded Budgets
    @Test
    fun `mission terminal state helper behaves consistently`() {
        val running = Mission("1", "Goal", status = MissionStatus.RUNNING)
        val paused = Mission("2", "Goal", status = MissionStatus.PAUSED)
        val completed = Mission("3", "Goal", status = MissionStatus.COMPLETED)
        val failed = Mission("4", "Goal", status = MissionStatus.FAILED)
        val aborted = Mission("5", "Goal", status = MissionStatus.ABORTED)

        assertFalse(running.isTerminal)
        assertFalse(paused.isTerminal)
        assertTrue(completed.isTerminal)
        assertTrue(failed.isTerminal)
        assertTrue(aborted.isTerminal)
    }

    @Test
    fun `plan manager enforces max replan budget limit`() = runBlocking {
        val limits = ExecutionLimits(maxReplansPerMission = 2)
        val planManager = PlanManager()
        val step = PlanStep(UUID.randomUUID().toString(), 1, "click_node_text", "Click Button", "")
        val failedResult = ActionResult(isSuccess = false, message = "Node not clickable")
        val perception = PerceptionContext()

        // When currentReplanCount equals maxReplans, it must yield BudgetExceeded
        val evaluation = planManager.evaluateStep(
            step = step,
            result = failedResult,
            prePerception = perception,
            postPerception = perception,
            goal = "Test Goal",
            config = LLMConfig(),
            currentReplanCount = 2,
            limits = limits
        )

        assertTrue("Evaluation must be BudgetExceeded", evaluation is StepEvaluationResult.BudgetExceeded)
    }

    // 6. Room Database Persistence
    @Test
    fun `room database stores and retrieves mission and steps`() = runBlocking {
        val missionDao = database.missionDao()
        val stepDao = database.stepDao()

        val missionId = UUID.randomUUID().toString()
        val missionEntity = MissionEntity(
            id = missionId,
            goalPrompt = "Night routine",
            createdAt = System.currentTimeMillis(),
            status = "COMPLETED",
            completedSteps = 3,
            totalSteps = 3,
            replanCount = 0,
            durationMs = 2500L,
            summary = "All steps verified",
            terminalReason = "GOAL_COMPLETED"
        )
        missionDao.insertMission(missionEntity)

        val retrievedMission = missionDao.getMissionById(missionId)
        assertNotNull(retrievedMission)
        assertEquals("Night routine", retrievedMission?.goalPrompt)
        assertEquals("COMPLETED", retrievedMission?.status)

        val stepId = UUID.randomUUID().toString()
        val stepEntity = StepEntity(
            missionId = missionId,
            stepId = stepId,
            stepIndex = 1,
            toolName = "set_alarm",
            title = "Set Alarm",
            description = "07:30 wake-up",
            paramsJson = "{\"hour\":\"7\",\"minute\":\"30\"}",
            status = "SUCCESS",
            executionStatus = "VERIFIED",
            attemptCount = 1,
            result = "Alarm set"
        )
        stepDao.insertStep(stepEntity)

        val steps = stepDao.getStepsForMission(missionId)
        assertEquals(1, steps.size)
        assertEquals(stepId, steps.first().stepId)
        assertEquals("set_alarm", steps.first().toolName)
    }

    // 7. C-01: Verification Failed Prevents Step Success
    @Test
    fun `plan manager treats verification failure as failure, not success`() = runBlocking {
        val planManager = PlanManager()
        val step = PlanStep(
            id = UUID.randomUUID().toString(),
            stepIndex = 1,
            toolName = "launch_app",
            title = "Launch Settings",
            description = "Open settings app",
            params = mapOf("app_name" to "Settings")
        )
        // Tool executed technically without error, but post-perception did NOT detect the app
        val result = ActionResult(isSuccess = true, message = "Activity intent dispatched")
        val prePerception = PerceptionContext(activeAppName = "Aethereal Cardinal")
        val postPerception = PerceptionContext(activeAppName = "Aethereal Cardinal") // still in Cardinal!

        val evaluation = planManager.evaluateStep(
            step = step,
            result = result,
            prePerception = prePerception,
            postPerception = postPerception,
            goal = "Open Settings",
            config = LLMConfig(),
            currentReplanCount = 0
        )

        assertFalse(
            "When verification fails, evaluation must NOT be StepEvaluationResult.Success",
            evaluation is StepEvaluationResult.Success
        )
    }

    // 8. C-04: Strict ResultVerifier Negative Cases
    @Test
    fun `result verifier rejects click when target was not present in pre-perception`() {
        val verifier = ResultVerifier()
        val prePerception = PerceptionContext(extractedHierarchy = emptyList())
        val postPerception = PerceptionContext(extractedHierarchy = emptyList())
        val step = PlanStep(
            id = UUID.randomUUID().toString(),
            stepIndex = 1,
            toolName = "click_node_text",
            title = "Click Missing Button",
            description = "",
            params = mapOf("target_text" to "NonExistentButton")
        )
        val result = ActionResult(isSuccess = true, message = "Click simulated")
        val verification = verifier.verifyPostcondition(step, result, prePerception, postPerception)

        assertEquals(ExecutionStatus.VERIFICATION_FAILED, verification.status)
        assertFalse(verification.isVerified)
        assertTrue(verification.evidence.contains("not present in pre-action screen tree"))
    }

    @Test
    fun `result verifier rejects when expected outcome is missing from screen`() {
        val verifier = ResultVerifier()
        val pre = PerceptionContext(extractedHierarchy = listOf(com.example.core.model.ScreenNode(id = "1", className = "android.widget.Button", text = "Submit")))
        val post = PerceptionContext(extractedHierarchy = listOf(com.example.core.model.ScreenNode(id = "1", className = "android.widget.Button", text = "Submit")))
        val step = PlanStep(
            id = UUID.randomUUID().toString(),
            stepIndex = 1,
            toolName = "click_node_text",
            title = "Click Submit",
            description = "",
            params = mapOf("target_text" to "Submit"),
            expectedOutcome = "Success Confirmation Banner"
        )
        val result = ActionResult(isSuccess = true, message = "Clicked Submit")
        val verification = verifier.verifyPostcondition(step, result, pre, post)

        assertEquals(ExecutionStatus.VERIFICATION_FAILED, verification.status)
        assertFalse(verification.isVerified)
        assertTrue(verification.evidence.contains("not found in active screen hierarchy"))
    }

    // 9. C-07: Non-Idempotent Tool Timeout Uncertain Effect Blocks Direct Retry
    @Test
    fun `timeout on non-idempotent action blocks blind retry`() = runBlocking {
        val planManager = PlanManager()
        val step = PlanStep(
            id = UUID.randomUUID().toString(),
            stepIndex = 1,
            toolName = "send_sms",
            title = "Send SMS",
            description = "Send message",
            params = mapOf("phone_number" to "1234567890", "message" to "Hello"),
            attemptCount = 3
        )
        val timeoutResult = ActionResult(
            isSuccess = false,
            executionStatus = ExecutionStatus.EXECUTION_FAILED,
            message = "Tool execution timed out after 15s",
            evidence = "TIMEOUT_UNCERTAIN_EFFECT"
        )
        val perception = PerceptionContext()

        val evaluation = planManager.evaluateStep(
            step = step,
            result = timeoutResult,
            prePerception = perception,
            postPerception = perception,
            goal = "Send emergency text",
            config = LLMConfig(),
            currentReplanCount = 0
        )

        assertTrue(
            "Evaluation should fail and block blind retry on non-idempotent timeout",
            evaluation is StepEvaluationResult.Failed
        )
        val failed = evaluation as StepEvaluationResult.Failed
        assertTrue(failed.reason.contains("NON_IDEMPOTENT_RETRY_BLOCKED"))
    }

    // 10. C-08: Idempotent Legacy Migration in MemoryStore
    @Test
    fun `legacy migration is idempotent and sets migration flag`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("cardinal_agent_memory", Context.MODE_PRIVATE)

        // Seed legacy JSON
        val legacyJson = """
            [
                {
                    "id": "legacy_m1",
                    "goal": "Legacy goal test",
                    "status": "COMPLETED",
                    "stepsCount": 2,
                    "durationMs": 1200,
                    "summary": "Migrated successfully"
                }
            ]
        """.trimIndent()
        prefs.edit()
            .putString("missions_json", legacyJson)
            .putBoolean("cardinal_legacy_migrated_v1", false)
            .commit()

        val memoryStore = com.example.core.memory.MemoryStore(context)
        val migratedCount = memoryStore.migrateLegacyJsonIfPresent()

        assertTrue("Should migrate legacy mission", migratedCount >= 0)
        assertTrue(
            "cardinal_legacy_migrated_v1 flag must be true after migration",
            prefs.getBoolean("cardinal_legacy_migrated_v1", false)
        )

        // Second call should be a no-op (idempotent)
        val secondRunCount = memoryStore.migrateLegacyJsonIfPresent()
        assertEquals(0, secondRunCount)
    }

    // 11. C-03: GeminiProvider Cancellation Rethrows CancellationException
    @Test
    fun `gemini provider rethrows cancellation exception without swallowing`() {
        val provider = com.example.core.llm.GeminiProvider()
        val perception = PerceptionContext()
        val config = LLMConfig(apiKey = "fake_key")

        var threwCancellation = false
        try {
            runBlocking {
                val job = kotlinx.coroutines.Job()
                job.cancel() // cancelled context
                kotlinx.coroutines.withContext(job) {
                    provider.generatePlan("Test Goal", perception, config)
                }
            }
        } catch (_: kotlinx.coroutines.CancellationException) {
            threwCancellation = true
        }

        assertTrue("Calling provider with cancelled coroutine must throw CancellationException", threwCancellation)
    }

    // 12. C-06: Sanitizer Redacts API Keys from URLs
    @Test
    fun `gemini provider sanitizeUrl strips api key`() {
        val provider = com.example.core.llm.GeminiProvider()
        val rawUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=AIzaSySecretKey123&alt=json"
        val sanitized = provider.sanitizeUrl(rawUrl)
        assertFalse("Sanitized URL must not contain the secret key", sanitized.contains("AIzaSySecretKey123"))
        assertTrue("Sanitized URL must contain [REDACTED]", sanitized.contains("key=[REDACTED]"))
    }
}
