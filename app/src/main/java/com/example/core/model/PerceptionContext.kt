package com.example.core.model

enum class PerceptionSource {
    ACCESSIBILITY_TREE,
    SCREENSHOT,
    HYBRID,
    UNAVAILABLE
}

data class ScreenNode(
    val id: String,
    val className: String,
    val text: String? = null,
    val contentDescription: String? = null,
    val viewId: String? = null,
    val isClickable: Boolean = false,
    val isEditable: Boolean = false,
    val isScrollable: Boolean = false,
    val isEnabled: Boolean = true,
    val isVisible: Boolean = true,
    val bounds: String = "",
    val depth: Int = 0
)

data class PerceptionContext(
    val source: PerceptionSource = PerceptionSource.UNAVAILABLE,
    val activePackage: String = "com.aethereal.cardinal",
    val activeAppName: String = "Aethereal Cardinal",
    val screenTitle: String = "Dashboard",
    val isAccessibilityActive: Boolean = false,
    val interactiveElementsCount: Int = 0,
    val extractedHierarchy: List<ScreenNode> = emptyList(),
    val rawTextSummary: String = "",
    val isTruncated: Boolean = false,
    val diagnosticReason: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
