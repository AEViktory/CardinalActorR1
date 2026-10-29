package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.accessibility.CardinalAccessibilityService
import com.example.core.actions.ActionExecutor
import com.example.core.actions.ActionRegistry
import com.example.core.actions.ActionResult
import com.example.core.actions.CardinalAction
import com.example.core.agent.AgentLoop
import com.example.core.memory.MemoryStore
import com.example.core.model.LLMConfig
import com.example.core.model.LogEntry
import com.example.core.model.Mission
import com.example.core.model.PerceptionContext
import com.example.core.perception.VisionEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class CardinalTab(val title: String) {
    MISSION("Mission"),
    TOOLS("Tools"),
    PERCEPTION("Perception"),
    HISTORY("History"),
    SETTINGS("Settings")
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val memoryStore = MemoryStore(application)
    private val actionExecutor = ActionExecutor(application)
    private val visionEngine = VisionEngine(application)
    private val agentLoop = AgentLoop(application, actionExecutor, visionEngine)

    private val _selectedTab = MutableStateFlow(CardinalTab.MISSION)
    val selectedTab: StateFlow<CardinalTab> = _selectedTab.asStateFlow()

    val activeMission: StateFlow<Mission?> = agentLoop.activeMission
    val telemetryLogs: StateFlow<List<LogEntry>> = agentLoop.logs
    val recentMissions: StateFlow<List<Mission>> = memoryStore.recentMissions
    val llmConfig: StateFlow<LLMConfig> = memoryStore.config

    val isAccessibilityActive: StateFlow<Boolean> = CardinalAccessibilityService.isServiceBound

    private val _livePerception = MutableStateFlow(PerceptionContext())
    val livePerception: StateFlow<PerceptionContext> = _livePerception.asStateFlow()

    private val _singleToolExecutionResult = MutableStateFlow<Pair<String, ActionResult>?>(null)
    val singleToolExecutionResult: StateFlow<Pair<String, ActionResult>?> = _singleToolExecutionResult.asStateFlow()

    val allTools: List<CardinalAction> = ActionRegistry.allActions

    init {
        refreshPerception()
    }

    fun selectTab(tab: CardinalTab) {
        _selectedTab.value = tab
    }

    fun dispatchMission(goal: String) {
        if (goal.isBlank()) return
        agentLoop.startMission(goal, llmConfig.value) { completedMission ->
            memoryStore.saveMission(completedMission)
        }
    }

    fun pauseMission() {
        viewModelScope.launch {
            agentLoop.pauseMission()
        }
    }

    fun resumeMission() {
        viewModelScope.launch {
            agentLoop.resumeMission()
        }
    }

    fun abortMission() = agentLoop.cancelCurrentMission()

    fun updateLLMConfig(newConfig: LLMConfig) {
        memoryStore.updateConfig(newConfig)
    }

    fun refreshPerception() {
        viewModelScope.launch {
            val perception = visionEngine.capturePerception()
            _livePerception.value = perception
        }
    }

    fun executeSingleTool(actionId: String, params: Map<String, String>) {
        viewModelScope.launch {
            val result = actionExecutor.execute(actionId, params)
            _singleToolExecutionResult.value = actionId to result
        }
    }

    fun clearToolResult() {
        _singleToolExecutionResult.value = null
    }
}
