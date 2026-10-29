package com.example.core.model

data class ExecutionLimits(
    val maxSteps: Int = 30,
    val maxRetriesPerStep: Int = 3,
    val maxReplansPerMission: Int = 10,
    val missionTimeoutMs: Long = 600_000L, // 10 minutes
    val defaultToolTimeoutMs: Long = 15_000L, // 15 seconds
    val llmTimeoutMs: Long = 45_000L, // 45 seconds
    val maxTraversalNodes: Int = 1000,
    val maxTraversalDepth: Int = 30,
    val maxNodeTextLength: Int = 512
)
