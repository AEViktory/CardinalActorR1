package com.example.core.agent

import com.example.core.actions.ActionResult
import com.example.core.model.ExecutionStatus
import com.example.core.model.PerceptionContext
import com.example.core.model.PlanStep

data class VerificationResult(
    val status: ExecutionStatus,
    val isVerified: Boolean,
    val evidence: String,
    val diagnosticReason: String? = null
)

class ResultVerifier {

    fun verifyPostcondition(
        step: PlanStep,
        executionResult: ActionResult,
        prePerception: PerceptionContext,
        postPerception: PerceptionContext
    ): VerificationResult {
        if (!executionResult.isSuccess) {
            return VerificationResult(
                status = ExecutionStatus.EXECUTION_FAILED,
                isVerified = false,
                evidence = "Tool returned technical failure: ${executionResult.message}",
                diagnosticReason = executionResult.errorDetails
            )
        }

        return when (step.toolName) {
            "get_battery_status" -> {
                val hasBatteryData = executionResult.outputData.containsKey("level")
                if (hasBatteryData) {
                    VerificationResult(
                        status = ExecutionStatus.VERIFIED,
                        isVerified = true,
                        evidence = "Battery telemetry verified: ${executionResult.message}"
                    )
                } else {
                    VerificationResult(
                        status = ExecutionStatus.VERIFICATION_FAILED,
                        isVerified = false,
                        evidence = "Telemetry retrieved without structured capacity reading"
                    )
                }
            }

            "toggle_flashlight" -> {
                VerificationResult(
                    status = ExecutionStatus.VERIFIED,
                    isVerified = true,
                    evidence = executionResult.message
                )
            }

            "vibrate_device", "wait_delay" -> {
                VerificationResult(
                    status = ExecutionStatus.VERIFIED,
                    isVerified = true,
                    evidence = "Action execution completed locally: ${executionResult.message}"
                )
            }

            "click_node_text" -> {
                val target = step.params["target_text"].orEmpty()
                val wasPresentBefore = prePerception.extractedHierarchy.any {
                    it.text?.contains(target, ignoreCase = true) == true
                }
                val stillPresentAfter = postPerception.extractedHierarchy.any {
                    it.text?.contains(target, ignoreCase = true) == true
                }

                // If explicit expectedOutcome is provided, verify against post-perception
                if (step.expectedOutcome.isNotBlank()) {
                    val outcomeFound = postPerception.extractedHierarchy.any {
                        it.text?.contains(step.expectedOutcome, ignoreCase = true) == true
                    } || postPerception.screenTitle.contains(step.expectedOutcome, ignoreCase = true)

                    if (outcomeFound) {
                        return VerificationResult(
                            status = ExecutionStatus.VERIFIED,
                            isVerified = true,
                            evidence = "Expected outcome '${step.expectedOutcome}' verified in post-perception"
                        )
                    } else {
                        return VerificationResult(
                            status = ExecutionStatus.VERIFICATION_FAILED,
                            isVerified = false,
                            evidence = "Expected outcome '${step.expectedOutcome}' not found in active screen hierarchy"
                        )
                    }
                }

                // Strictly evaluate state transition: title change alone is not sufficient if target was never present
                if (!wasPresentBefore) {
                    VerificationResult(
                        status = ExecutionStatus.VERIFICATION_FAILED,
                        isVerified = false,
                        evidence = "Target element '$target' was not present in pre-action screen tree"
                    )
                } else if (!stillPresentAfter || prePerception.interactiveElementsCount != postPerception.interactiveElementsCount) {
                    VerificationResult(
                        status = ExecutionStatus.VERIFIED,
                        isVerified = true,
                        evidence = "UI transition verified after click on '$target' (target resolved or window updated)"
                    )
                } else {
                    VerificationResult(
                        status = ExecutionStatus.VERIFICATION_FAILED,
                        isVerified = false,
                        evidence = "Element clicked, but screen state remained unchanged"
                    )
                }
            }

            "launch_app" -> {
                val app = step.params["app_name"].orEmpty()
                val isForeground = postPerception.activeAppName.contains(app, ignoreCase = true) ||
                        postPerception.activePackage.contains(app, ignoreCase = true)
                if (isForeground) {
                    VerificationResult(
                        status = ExecutionStatus.VERIFIED,
                        isVerified = true,
                        evidence = "Active foreground app confirmed as '${postPerception.activeAppName}' (${postPerception.activePackage})"
                    )
                } else {
                    VerificationResult(
                        status = ExecutionStatus.VERIFICATION_FAILED,
                        isVerified = false,
                        evidence = "Launch intent dispatched, but target app '$app' not detected in foreground (current: '${postPerception.activeAppName}')"
                    )
                }
            }

            // System panels / Intents where Android limits conventional apps from verifying private system state
            "toggle_wifi_settings", "toggle_bluetooth_settings", "open_sound_settings",
            "open_display_settings", "open_dnd_settings", "open_battery_settings" -> {
                VerificationResult(
                    status = ExecutionStatus.NOT_VERIFIED,
                    isVerified = false,
                    evidence = "System settings panel launched. Third-party Android sandboxing prevents direct query of internal setting toggle state."
                )
            }

            "set_alarm", "set_timer", "create_calendar_event", "send_sms", "dial_phone_number", "open_whatsapp", "open_url" -> {
                VerificationResult(
                    status = ExecutionStatus.NOT_VERIFIED,
                    isVerified = false,
                    evidence = "System intent dispatched to external provider (${step.toolName}). Awaiting external app processing."
                )
            }

            "scroll_forward", "press_home", "pull_quick_settings", "open_recent_apps" -> {
                VerificationResult(
                    status = ExecutionStatus.VERIFIED,
                    isVerified = true,
                    evidence = "System gesture executed successfully: ${executionResult.message}"
                )
            }

            "inspect_screen" -> {
                VerificationResult(
                    status = ExecutionStatus.VERIFIED,
                    isVerified = true,
                    evidence = "Screen state captured successfully (${postPerception.interactiveElementsCount} elements)"
                )
            }

            else -> {
                VerificationResult(
                    status = ExecutionStatus.NOT_VERIFIED,
                    isVerified = false,
                    evidence = "Executed without explicit postcondition verification rule"
                )
            }
        }
    }
}
