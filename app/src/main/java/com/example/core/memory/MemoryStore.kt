package com.example.core.memory

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import com.example.core.database.CardinalDatabase
import com.example.core.database.entity.ExecutionEventEntity
import com.example.core.database.entity.MissionEntity
import com.example.core.database.entity.StepEntity
import com.example.core.model.ExecutionStatus
import com.example.core.model.LLMConfig
import com.example.core.model.LLMProviderType
import com.example.core.model.LogEntry
import com.example.core.model.Mission
import com.example.core.model.MissionStatus
import com.example.core.model.PlanStep
import com.example.core.model.StepStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class MemoryStore(context: Context) {

    private val appContext = context.applicationContext
    private val db = CardinalDatabase.getInstance(appContext)
    private val missionDao = db.missionDao()
    private val stepDao = db.stepDao()
    private val eventDao = db.eventDao()
    private val prefs = appContext.getSharedPreferences("cardinal_agent_memory", Context.MODE_PRIVATE)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _recentMissions = MutableStateFlow<List<Mission>>(emptyList())
    val recentMissions: StateFlow<List<Mission>> = _recentMissions.asStateFlow()

    private val _config = MutableStateFlow(loadConfig())
    val config: StateFlow<LLMConfig> = _config.asStateFlow()

    init {
        scope.launch {
            // Idempotent one-time migration for legacy JSON
            if (!prefs.getBoolean("cardinal_legacy_migrated_v1", false)) {
                migrateLegacyJsonIfPresent()
            }

            // Continuous subscription to Room missions
            missionDao.getAllMissions().collectLatest { entities ->
                _recentMissions.value = entities.map { it.toDomain() }
            }
        }
    }

    suspend fun persistMission(mission: Mission) {
        db.withTransaction {
            // Insert or update mission entity
            val entity = MissionEntity(
                id = mission.id,
                goalPrompt = mission.goalPrompt,
                createdAt = mission.createdAt,
                status = mission.status.name,
                completedSteps = mission.completedSteps,
                totalSteps = mission.totalSteps,
                replanCount = mission.replanCount,
                durationMs = mission.durationMs,
                summary = mission.summary,
                terminalReason = mission.terminalReason
            )
            missionDao.insertMission(entity)

            // Insert steps
            if (mission.steps.isNotEmpty()) {
                val stepEntities = mission.steps.map { step ->
                    val paramsJson = JSONObject().apply {
                        step.params.forEach { (k, v) -> put(k, v) }
                    }.toString()

                    StepEntity(
                        missionId = mission.id,
                        stepId = step.id,
                        stepIndex = step.stepIndex,
                        toolName = step.toolName,
                        title = step.title,
                        description = step.description,
                        paramsJson = paramsJson,
                        status = step.status.name,
                        executionStatus = step.executionStatus?.name,
                        attemptCount = step.attemptCount,
                        parentStepId = step.parentStepId,
                        result = step.result,
                        durationMs = step.durationMs
                    )
                }
                stepDao.insertSteps(stepEntities)
            }

            // Insert execution events
            mission.thoughtLog.takeLast(30).forEach { log ->
                eventDao.insertEvent(
                    ExecutionEventEntity(
                        missionId = mission.id,
                        timestamp = log.timestamp,
                        tag = log.tag,
                        level = log.level.name,
                        message = log.message
                    )
                )
            }
        }
    }

    fun saveMission(mission: Mission) {
        scope.launch {
            try {
                persistMission(mission)
            } catch (e: Exception) {
                Log.e("MemoryStore", "Failed to persist mission ${mission.id} to CardinalDatabase", e)
            }
        }
    }

    suspend fun getStepsForMission(missionId: String): List<PlanStep> {
        return try {
            stepDao.getStepsForMission(missionId).map { it.toDomain() }
        } catch (e: Exception) {
            Log.w("MemoryStore", "Error loading steps for mission $missionId", e)
            emptyList()
        }
    }

    fun updateConfig(newConfig: LLMConfig) {
        _config.value = newConfig
        prefs.edit().apply {
            putString("provider", newConfig.provider.name)
            putString("api_key", newConfig.apiKey)
            putString("model_name", newConfig.modelName)
            putFloat("temperature", newConfig.temperature)
            putInt("max_retries", newConfig.maxRetries)
            apply()
        }
    }

    private fun loadConfig(): LLMConfig {
        val providerStr = prefs.getString("provider", LLMProviderType.GEMINI.name) ?: LLMProviderType.GEMINI.name
        val provider = try { LLMProviderType.valueOf(providerStr) } catch (_: Exception) { LLMProviderType.GEMINI }
        val apiKey = prefs.getString("api_key", "") ?: ""
        val model = prefs.getString("model_name", "gemini-2.0-flash") ?: "gemini-2.0-flash"
        val temp = prefs.getFloat("temperature", 0.2f)
        val retries = prefs.getInt("max_retries", 3)

        return LLMConfig(
            provider = provider,
            apiKey = apiKey,
            modelName = model,
            temperature = temp,
            maxRetries = retries
        )
    }

    suspend fun migrateLegacyJsonIfPresent(): Int {
        if (prefs.getBoolean("cardinal_legacy_migrated_v1", false)) {
            return 0
        }
        val jsonStr = prefs.getString("missions_json", null) ?: run {
            prefs.edit().putBoolean("cardinal_legacy_migrated_v1", true).apply()
            return 0
        }
        return try {
            val array = JSONArray(jsonStr)
            var migrated = 0
            db.withTransaction {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val statusStr = obj.optString("status", "COMPLETED")
                    val entity = MissionEntity(
                        id = obj.getString("id"),
                        goalPrompt = obj.getString("goal"),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        status = statusStr,
                        completedSteps = obj.optInt("stepsCount", 1),
                        totalSteps = obj.optInt("stepsCount", 1),
                        replanCount = 0,
                        durationMs = obj.optLong("durationMs", 0L),
                        summary = obj.optString("summary", null)
                    )
                    missionDao.insertMission(entity)
                    migrated++
                }
            }
            prefs.edit().putBoolean("cardinal_legacy_migrated_v1", true).apply()
            migrated
        } catch (e: Exception) {
            Log.e("MemoryStore", "Failed to migrate legacy missions JSON", e)
            0
        }
    }

    private fun MissionEntity.toDomain(): Mission {
        val domainStatus = try {
            MissionStatus.valueOf(status)
        } catch (_: Exception) {
            MissionStatus.COMPLETED
        }

        return Mission(
            id = id,
            goalPrompt = goalPrompt,
            createdAt = createdAt,
            status = domainStatus,
            replanCount = replanCount,
            durationMs = durationMs,
            summary = summary,
            terminalReason = terminalReason
        )
    }

    private fun StepEntity.toDomain(): PlanStep {
        val domainStatus = try {
            StepStatus.valueOf(status)
        } catch (_: Exception) {
            StepStatus.PENDING
        }

        val execStatus = executionStatus?.let {
            try { ExecutionStatus.valueOf(it) } catch (_: Exception) { null }
        }

        val params = mutableMapOf<String, String>()
        try {
            val json = JSONObject(paramsJson)
            json.keys().forEach { k ->
                params[k] = json.getString(k)
            }
        } catch (_: Exception) {
            // Keep empty on error
        }

        return PlanStep(
            id = stepId,
            stepIndex = stepIndex,
            toolName = toolName,
            title = title,
            description = description,
            params = params,
            status = domainStatus,
            executionStatus = execStatus,
            attemptCount = attemptCount,
            parentStepId = parentStepId,
            result = result,
            durationMs = durationMs
        )
    }
}
