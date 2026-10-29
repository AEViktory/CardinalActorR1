package com.example.core.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.core.model.ExecutionLimits
import com.example.core.model.PerceptionContext
import com.example.core.model.PerceptionSource
import com.example.core.model.ScreenNode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface NodeInteractionResult {
    object Success : NodeInteractionResult
    data class AmbiguousTarget(val matchesCount: Int, val description: String) : NodeInteractionResult
    object NodeNotFound : NodeInteractionResult
    object DisabledOrInvisible : NodeInteractionResult
    data class Error(val reason: String) : NodeInteractionResult
}

class CardinalAccessibilityService : AccessibilityService() {

    private val limits = ExecutionLimits()

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        _isServiceBound.value = true
        inspectCurrentWindow()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val packageName = event.packageName?.toString() ?: return
        val className = event.className?.toString() ?: ""
        inspectCurrentWindow(packageName, className)
    }

    override fun onInterrupt() {
        _isServiceBound.value = false
        instance = null
    }

    override fun onDestroy() {
        super.onDestroy()
        _isServiceBound.value = false
        instance = null
    }

    fun inspectCurrentWindow(activePkg: String? = null, className: String? = null): PerceptionContext {
        val rootNode = rootInActiveWindow
        if (rootNode == null) {
            val emptyContext = PerceptionContext(
                source = PerceptionSource.UNAVAILABLE,
                activePackage = activePkg ?: "com.aethereal.cardinal",
                activeAppName = "Unknown",
                screenTitle = className ?: "Screen",
                isAccessibilityActive = true,
                interactiveElementsCount = 0,
                extractedHierarchy = emptyList(),
                rawTextSummary = "",
                diagnosticReason = "Active window root node returned null (system window transition or protected content)."
            )
            _currentPerception.value = emptyContext
            return emptyContext
        }

        val pkg = activePkg ?: rootNode.packageName?.toString() ?: "com.aethereal.cardinal"
        val nodes = mutableListOf<ScreenNode>()
        val textSnippets = mutableListOf<String>()
        var isTruncated = false

        try {
            traverseNodeBounded(
                node = rootNode,
                outNodes = nodes,
                outTexts = textSnippets,
                currentDepth = 0,
                maxDepth = limits.maxTraversalDepth,
                maxNodes = limits.maxTraversalNodes,
                maxTextLen = limits.maxNodeTextLength
            )
        } catch (_: Exception) {
            isTruncated = true
        }

        val context = PerceptionContext(
            source = PerceptionSource.ACCESSIBILITY_TREE,
            activePackage = pkg,
            activeAppName = getAppNameFromPackage(pkg),
            screenTitle = className ?: "Screen",
            isAccessibilityActive = true,
            interactiveElementsCount = nodes.count { it.isClickable },
            extractedHierarchy = nodes.take(limits.maxTraversalNodes),
            rawTextSummary = textSnippets.filter { it.isNotBlank() }.take(25).joinToString(" | "),
            isTruncated = isTruncated || nodes.size >= limits.maxTraversalNodes,
            timestamp = System.currentTimeMillis()
        )

        _currentPerception.value = context
        return context
    }

    private fun traverseNodeBounded(
        node: AccessibilityNodeInfo,
        outNodes: MutableList<ScreenNode>,
        outTexts: MutableList<String>,
        currentDepth: Int,
        maxDepth: Int,
        maxNodes: Int,
        maxTextLen: Int
    ) {
        if (outNodes.size >= maxNodes || currentDepth > maxDepth) {
            return
        }

        val rect = Rect()
        node.getBoundsInScreen(rect)

        val rawText = node.text?.toString()?.take(maxTextLen)
        val rawDesc = node.contentDescription?.toString()?.take(maxTextLen)
        val viewId = node.viewIdResourceName

        if (!rawText.isNullOrBlank()) outTexts.add(rawText)
        if (!rawDesc.isNullOrBlank()) outTexts.add(rawDesc)

        outNodes.add(
            ScreenNode(
                id = if (!viewId.isNullOrBlank()) viewId else "node_${outNodes.size}",
                className = node.className?.toString() ?: "View",
                text = rawText,
                contentDescription = rawDesc,
                viewId = viewId,
                isClickable = node.isClickable,
                isEditable = node.isEditable,
                isScrollable = node.isScrollable,
                isEnabled = node.isEnabled,
                isVisible = node.isVisibleToUser,
                bounds = "${rect.left},${rect.top}-${rect.right},${rect.bottom}",
                depth = currentDepth
            )
        )

        val childCount = node.childCount
        for (i in 0 until childCount) {
            if (outNodes.size >= maxNodes) break
            val child = node.getChild(i)
            if (child != null) {
                traverseNodeBounded(child, outNodes, outTexts, currentDepth + 1, maxDepth, maxNodes, maxTextLen)
            }
        }
    }

    fun performClickOnNode(targetTextOrId: String): NodeInteractionResult {
        val rootNode = rootInActiveWindow ?: return NodeInteractionResult.Error("No active window root")

        // First attempt: resolve by exact view ID
        val idMatches = rootNode.findAccessibilityNodeInfosByViewId(targetTextOrId)
        if (idMatches.isNotEmpty()) {
            if (idMatches.size > 1) {
                return NodeInteractionResult.AmbiguousTarget(idMatches.size, "Multiple elements match View ID '$targetTextOrId'")
            }
            val candidate = idMatches.first()
            return executeClick(candidate)
        }

        // Second attempt: resolve by text search
        val textMatches = rootNode.findAccessibilityNodeInfosByText(targetTextOrId)
        if (textMatches.isEmpty()) {
            return NodeInteractionResult.NodeNotFound
        }

        // Filter valid interactive matches
        val candidates = textMatches.filter { it.isVisibleToUser && it.isEnabled }
        if (candidates.isEmpty()) {
            return NodeInteractionResult.DisabledOrInvisible
        }

        if (candidates.size > 1) {
            // Check if there is exactly one exact text match
            val exactMatches = candidates.filter {
                it.text?.toString().equals(targetTextOrId, ignoreCase = true) ||
                it.contentDescription?.toString().equals(targetTextOrId, ignoreCase = true)
            }
            if (exactMatches.size == 1) {
                return executeClick(exactMatches.first())
            }
            return NodeInteractionResult.AmbiguousTarget(
                candidates.size,
                "Ambiguous target: ${candidates.size} elements match label '$targetTextOrId'. Provide exact ID or unique qualifier."
            )
        }

        return executeClick(candidates.first())
    }

    private fun executeClick(targetNode: AccessibilityNodeInfo): NodeInteractionResult {
        if (!targetNode.isVisibleToUser || !targetNode.isEnabled) {
            return NodeInteractionResult.DisabledOrInvisible
        }

        if (targetNode.isClickable) {
            val success = targetNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            return if (success) NodeInteractionResult.Success else NodeInteractionResult.Error("Node rejected ACTION_CLICK")
        }

        // Ascend to clickable parent
        var parent = targetNode.parent
        while (parent != null) {
            if (parent.isClickable && parent.isEnabled && parent.isVisibleToUser) {
                val success = parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                return if (success) NodeInteractionResult.Success else NodeInteractionResult.Error("Parent rejected ACTION_CLICK")
            }
            parent = parent.parent
        }

        return NodeInteractionResult.Error("Neither element nor its parents are clickable")
    }

    fun performScroll(forward: Boolean = true): NodeInteractionResult {
        val rootNode = rootInActiveWindow ?: return NodeInteractionResult.Error("No active window root")

        // Locate designated scrollable container
        val scrollableNode = findScrollableContainer(rootNode)
        if (scrollableNode == null) {
            return NodeInteractionResult.Error("No scrollable container found on active screen")
        }

        val action = if (forward) AccessibilityNodeInfo.ACTION_SCROLL_FORWARD else AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
        val success = scrollableNode.performAction(action)
        return if (success) NodeInteractionResult.Success else NodeInteractionResult.Error("Scroll container rejected scroll action")
    }

    private fun findScrollableContainer(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isScrollable && node.isVisibleToUser) {
            return node
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                val found = findScrollableContainer(child)
                if (found != null) return found
            }
        }
        return null
    }

    private fun getAppNameFromPackage(pkg: String): String {
        return try {
            val pm = packageManager
            val appInfo = pm.getApplicationInfo(pkg, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (_: Exception) {
            pkg.substringAfterLast('.')
        }
    }

    companion object {
        var instance: CardinalAccessibilityService? = null
            private set

        private val _isServiceBound = MutableStateFlow(false)
        val isServiceBound: StateFlow<Boolean> = _isServiceBound.asStateFlow()

        private val _currentPerception = MutableStateFlow(
            PerceptionContext(
                source = PerceptionSource.UNAVAILABLE,
                diagnosticReason = "Accessibility service has not connected."
            )
        )
        val currentPerception: StateFlow<PerceptionContext> = _currentPerception.asStateFlow()
    }
}
