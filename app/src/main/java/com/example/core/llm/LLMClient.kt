package com.example.core.llm

import com.example.core.actions.ActionRegistry
import com.example.core.model.LLMConfig
import com.example.core.model.PerceptionContext
import com.example.core.model.PlanStep
import com.example.core.model.StepStatus
import java.util.UUID

interface LLMClient {
    suspend fun generatePlan(goal: String, perception: PerceptionContext, config: LLMConfig): List<PlanStep>
    suspend fun replanStep(goal: String, failedStep: PlanStep, failureReason: String, perception: PerceptionContext, config: LLMConfig): PlanStep?
}

class DeterministicPlanner : LLMClient {

    override suspend fun generatePlan(
        goal: String,
        perception: PerceptionContext,
        config: LLMConfig
    ): List<PlanStep> {
        val lower = goal.lowercase().trim()
        val steps = mutableListOf<PlanStep>()

        // Compound Routine: Night / Sleep / Bedtime
        if (lower.contains("night") || lower.contains("sleep") || lower.contains("bedtime")) {
            steps.add(
                PlanStep(
                    id = UUID.randomUUID().toString(),
                    stepIndex = 1,
                    toolName = "open_dnd_settings",
                    title = "Enable Do Not Disturb",
                    description = "Silence non-critical alerts for restful sleep"
                )
            )
            steps.add(
                PlanStep(
                    id = UUID.randomUUID().toString(),
                    stepIndex = 2,
                    toolName = "set_alarm",
                    title = "Set Morning Alarm",
                    description = "Schedule wake-up alarm for 07:30 AM",
                    params = mapOf("hour" to "7", "minute" to "30", "message" to "Good Morning")
                )
            )
            steps.add(
                PlanStep(
                    id = UUID.randomUUID().toString(),
                    stepIndex = 3,
                    toolName = "toggle_flashlight",
                    title = "Ensure Flashlight Off",
                    description = "Power down camera torch to conserve battery",
                    params = mapOf("enabled" to "false")
                )
            )
            return steps
        }

        // Focus / Study / Deep Work
        if (lower.contains("focus") || lower.contains("study") || lower.contains("work")) {
            steps.add(
                PlanStep(
                    id = UUID.randomUUID().toString(),
                    stepIndex = 1,
                    toolName = "open_dnd_settings",
                    title = "Activate Focus Mode",
                    description = "Engage Do Not Disturb to eliminate notifications"
                )
            )
            steps.add(
                PlanStep(
                    id = UUID.randomUUID().toString(),
                    stepIndex = 2,
                    toolName = "set_timer",
                    title = "Start 45-Minute Focus Sprint",
                    description = "Countdown timer for Pomodoro focus cycle",
                    params = mapOf("seconds" to "2700", "message" to "Deep Work Cycle")
                )
            )
            return steps
        }

        // Alarm intent
        if (lower.contains("alarm")) {
            var hour = 7
            var minute = 0
            val regex = Regex("(\\d{1,2})[:.]?(\\d{2})?\\s*(am|pm)?")
            val match = regex.find(lower)
            if (match != null) {
                val h = match.groupValues[1].toIntOrNull() ?: 7
                val m = match.groupValues[2].toIntOrNull() ?: 0
                val ampm = match.groupValues[3]
                hour = when {
                    ampm == "pm" && h < 12 -> h + 12
                    ampm == "am" && h == 12 -> 0
                    else -> h
                }
                minute = m
            }
            steps.add(
                PlanStep(
                    id = UUID.randomUUID().toString(),
                    stepIndex = 1,
                    toolName = "set_alarm",
                    title = "Schedule System Alarm",
                    description = "Set wake alarm for %02d:%02d".format(hour, minute),
                    params = mapOf("hour" to hour.toString(), "minute" to minute.toString(), "message" to goal)
                )
            )
        }

        // Timer intent
        if (lower.contains("timer")) {
            val minMatch = Regex("(\\d+)\\s*(min|minute|sec|second)").find(lower)
            val count = minMatch?.groupValues?.get(1)?.toIntOrNull() ?: 5
            val unit = minMatch?.groupValues?.get(2) ?: "minute"
            val totalSeconds = if (unit.startsWith("sec")) count else count * 60
            steps.add(
                PlanStep(
                    id = UUID.randomUUID().toString(),
                    stepIndex = steps.size + 1,
                    toolName = "set_timer",
                    title = "Start Countdown Timer",
                    description = "Countdown for $count $unit",
                    params = mapOf("seconds" to totalSeconds.toString(), "message" to goal)
                )
            )
        }

        // Torch / Flashlight
        if (lower.contains("torch") || lower.contains("flashlight")) {
            val turnOff = lower.contains("off") || lower.contains("disable")
            steps.add(
                PlanStep(
                    id = UUID.randomUUID().toString(),
                    stepIndex = steps.size + 1,
                    toolName = "toggle_flashlight",
                    title = if (turnOff) "Turn Off Flashlight" else "Turn On Flashlight",
                    description = "Control device LED torch",
                    params = mapOf("enabled" to (!turnOff).toString())
                )
            )
        }

        // Battery Check
        if (lower.contains("battery") || lower.contains("power")) {
            steps.add(
                PlanStep(
                    id = UUID.randomUUID().toString(),
                    stepIndex = steps.size + 1,
                    toolName = "get_battery_status",
                    title = "Inspect Battery Telemetry",
                    description = "Query state of charge and battery health"
                )
            )
            if (lower.contains("save") || lower.contains("low")) {
                steps.add(
                    PlanStep(
                        id = UUID.randomUUID().toString(),
                        stepIndex = steps.size + 1,
                        toolName = "open_battery_settings",
                        title = "Enable Battery Saver Mode",
                        description = "Launch battery optimization settings"
                    )
                )
            }
        }

        // WhatsApp / SMS Communication
        if (lower.contains("whatsapp")) {
            steps.add(
                PlanStep(
                    id = UUID.randomUUID().toString(),
                    stepIndex = steps.size + 1,
                    toolName = "open_whatsapp",
                    title = "Draft WhatsApp Message",
                    description = "Open WhatsApp conversation draft",
                    params = mapOf("phone_number" to "", "text" to goal)
                )
            )
        } else if (lower.contains("sms") || lower.contains("text")) {
            steps.add(
                PlanStep(
                    id = UUID.randomUUID().toString(),
                    stepIndex = steps.size + 1,
                    toolName = "send_sms",
                    title = "Compose SMS Text",
                    description = "Draft system SMS message",
                    params = mapOf("phone_number" to "", "message_text" to goal)
                )
            )
        }

        // Web Search
        if (lower.contains("search") || lower.contains("google") || lower.contains("lookup")) {
            val query = goal.replace(Regex("(?i)^(search for|search|lookup|google)\\s*"), "").trim()
            steps.add(
                PlanStep(
                    id = UUID.randomUUID().toString(),
                    stepIndex = steps.size + 1,
                    toolName = "search_web",
                    title = "Search Web",
                    description = "Search internet for '$query'",
                    params = mapOf("query" to query)
                )
            )
        }

        // App Launching
        val appMatches = listOf("chrome", "youtube", "maps", "camera", "calculator", "calendar", "gmail")
        for (app in appMatches) {
            if (lower.contains(app) && steps.none { it.toolName == "launch_app" }) {
                steps.add(
                    PlanStep(
                        id = UUID.randomUUID().toString(),
                        stepIndex = steps.size + 1,
                        toolName = "launch_app",
                        title = "Launch ${app.replaceFirstChar { it.uppercase() }}",
                        description = "Open $app on Android device",
                        params = mapOf("app_name" to app)
                    )
                )
            }
        }

        // Default: If no specific pattern matched, create a targeted perception & search step
        if (steps.isEmpty()) {
            steps.add(
                PlanStep(
                    id = UUID.randomUUID().toString(),
                    stepIndex = 1,
                    toolName = "inspect_screen",
                    title = "Perceive Screen Context",
                    description = "Inspect current UI state for contextual action"
                )
            )
            steps.add(
                PlanStep(
                    id = UUID.randomUUID().toString(),
                    stepIndex = 2,
                    toolName = "search_web",
                    title = "Search Knowledge for Mission",
                    description = "Query information relevant to '$goal'",
                    params = mapOf("query" to goal)
                )
            )
        }

        return steps
    }

    override suspend fun replanStep(
        goal: String,
        failedStep: PlanStep,
        failureReason: String,
        perception: PerceptionContext,
        config: LLMConfig
    ): PlanStep? {
        // Alternative recovery strategies
        return when (failedStep.toolName) {
            "click_node_text" -> {
                // Fallback: try scrolling screen to reveal element
                PlanStep(
                    id = UUID.randomUUID().toString(),
                    stepIndex = failedStep.stepIndex,
                    toolName = "scroll_screen",
                    title = "Scroll to Locate UI Node",
                    description = "Target text was not visible; scrolling down to reveal element",
                    params = mapOf("direction" to "forward")
                )
            }
            "toggle_flashlight" -> {
                // Fallback: trigger haptic confirmation instead
                PlanStep(
                    id = UUID.randomUUID().toString(),
                    stepIndex = failedStep.stepIndex,
                    toolName = "vibrate_device",
                    title = "Fallback Haptic Confirmation",
                    description = "Torch unavailable; signalling via vibration",
                    params = mapOf("duration_ms" to "400")
                )
            }
            "launch_app" -> {
                // Fallback: search web for app content
                PlanStep(
                    id = UUID.randomUUID().toString(),
                    stepIndex = failedStep.stepIndex,
                    toolName = "search_web",
                    title = "Fallback Web Access",
                    description = "App launch failed; redirecting to web browser",
                    params = mapOf("query" to failedStep.params["app_name"].orEmpty())
                )
            }
            else -> null
        }
    }
}
