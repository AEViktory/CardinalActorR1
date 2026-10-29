package com.example.core.perception

import android.content.Context
import com.example.core.accessibility.CardinalAccessibilityService
import com.example.core.model.PerceptionContext
import com.example.core.model.PerceptionSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class VisionEngine(private val context: Context) {

    suspend fun capturePerception(): PerceptionContext = withContext(Dispatchers.Default) {
        val service = CardinalAccessibilityService.instance
        if (service != null) {
            // Live accessibility perception
            return@withContext service.inspectCurrentWindow()
        }

        // Accessibility is disconnected or disabled: Report real degraded state, never synthesize mock screen nodes!
        PerceptionContext(
            source = PerceptionSource.UNAVAILABLE,
            activePackage = context.packageName,
            activeAppName = "Aethereal Cardinal",
            screenTitle = "Aethereal Cardinal",
            isAccessibilityActive = false,
            interactiveElementsCount = 0,
            extractedHierarchy = emptyList(),
            rawTextSummary = "",
            isTruncated = false,
            diagnosticReason = "Accessibility Service is not bound. Live UI node inspection and screen actions are unavailable until granted in Android Settings."
        )
    }

    fun buildPerceptionPromptSnippet(perception: PerceptionContext): String {
        val sb = StringBuilder()
        sb.append("CURRENT DEVICE CONTEXT:\n")
        sb.append("- Perception Source: ${perception.source.name}\n")
        sb.append("- Active App: ${perception.activeAppName} (${perception.activePackage})\n")
        sb.append("- Screen/Activity: ${perception.screenTitle}\n")
        sb.append("- Accessibility Service Active: ${perception.isAccessibilityActive}\n")

        if (perception.diagnosticReason != null) {
            sb.append("- Perception Notice: ${perception.diagnosticReason}\n")
        }

        if (perception.extractedHierarchy.isNotEmpty()) {
            sb.append("- Detected Interactive UI Nodes (${perception.extractedHierarchy.size} elements):\n")
            perception.extractedHierarchy.filter { it.isClickable || it.isEditable }.take(15).forEach { node ->
                val label = node.text ?: node.contentDescription ?: node.id
                sb.append("  • [${node.className.substringAfterLast('.')}] \"$label\" (id: ${node.id}, enabled=${node.isEnabled})\n")
            }
        }

        if (perception.rawTextSummary.isNotBlank()) {
            sb.append("- Screen Text Summary: \"${perception.rawTextSummary}\"\n")
        }

        if (perception.isTruncated) {
            sb.append("- Notice: UI node hierarchy was truncated to stay within bounded memory/latency limits.\n")
        }

        return sb.toString()
    }
}
