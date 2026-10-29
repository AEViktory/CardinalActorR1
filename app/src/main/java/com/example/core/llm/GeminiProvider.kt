package com.example.core.llm

import com.example.BuildConfig
import com.example.core.actions.ActionRegistry
import com.example.core.model.LLMConfig
import com.example.core.model.PerceptionContext
import com.example.core.model.PlanStep
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.UUID
import java.util.concurrent.TimeUnit

enum class ProviderErrorCode {
    NETWORK_ERROR,
    TIMEOUT,
    AUTHENTICATION_ERROR,
    RATE_LIMITED,
    SERVER_ERROR,
    INVALID_RESPONSE,
    SCHEMA_VALIDATION_ERROR,
    CANCELLED,
    PROVIDER_UNAVAILABLE
}

data class ProviderException(
    val code: ProviderErrorCode,
    val safeMessage: String,
    val technicalDetails: String? = null
) : Exception(safeMessage)

class GeminiProvider : LLMClient {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val fallbackPlanner = DeterministicPlanner()

    override suspend fun generatePlan(
        goal: String,
        perception: PerceptionContext,
        config: LLMConfig
    ): List<PlanStep> = withContext(Dispatchers.IO) {
        val apiKey = resolveApiKey(config)

        if (apiKey.isBlank()) {
            // Explicit transparent deterministic fallback
            return@withContext fallbackPlanner.generatePlan(goal, perception, config)
        }

        try {
            val model = config.modelName.ifBlank { "gemini-2.0-flash" }
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

            val prompt = buildPlanPrompt(goal, perception)

            val jsonBody = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("parts", JSONArray().apply {
                            put(JSONObject().put("text", prompt))
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("temperature", config.temperature.toDouble())
                    put("responseMimeType", "application/json")
                })
            }

            val request = Request.Builder()
                .url(url)
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = try {
                httpClient.newCall(request).execute()
            } catch (e: SocketTimeoutException) {
                return@withContext fallbackPlanner.generatePlan(goal, perception, config)
            } catch (e: IOException) {
                return@withContext fallbackPlanner.generatePlan(goal, perception, config)
            }

            if (!response.isSuccessful) {
                // Classified HTTP error
                val code = when (response.code) {
                    401, 403 -> ProviderErrorCode.AUTHENTICATION_ERROR
                    429 -> ProviderErrorCode.RATE_LIMITED
                    in 500..599 -> ProviderErrorCode.SERVER_ERROR
                    else -> ProviderErrorCode.PROVIDER_UNAVAILABLE
                }
                // Fallback on transient/auth failures
                return@withContext fallbackPlanner.generatePlan(goal, perception, config)
            }

            val responseStr = response.body?.string() ?: return@withContext fallbackPlanner.generatePlan(goal, perception, config)
            val jsonRoot = JSONObject(responseStr)
            val candidates = jsonRoot.optJSONArray("candidates") ?: return@withContext fallbackPlanner.generatePlan(goal, perception, config)
            val firstCandidate = candidates.optJSONObject(0) ?: return@withContext fallbackPlanner.generatePlan(goal, perception, config)
            val content = firstCandidate.optJSONObject("content") ?: return@withContext fallbackPlanner.generatePlan(goal, perception, config)
            val parts = content.optJSONArray("parts") ?: return@withContext fallbackPlanner.generatePlan(goal, perception, config)
            val rawText = parts.optJSONObject(0)?.optString("text") ?: return@withContext fallbackPlanner.generatePlan(goal, perception, config)

            parseStepsFromJson(rawText, goal)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            fallbackPlanner.generatePlan(goal, perception, config)
        }
    }

    override suspend fun replanStep(
        goal: String,
        failedStep: PlanStep,
        failureReason: String,
        perception: PerceptionContext,
        config: LLMConfig
    ): PlanStep? = withContext(Dispatchers.IO) {
        val apiKey = resolveApiKey(config)
        if (apiKey.isBlank()) {
            return@withContext fallbackPlanner.replanStep(goal, failedStep, failureReason, perception, config)
        }

        try {
            val prompt = """
                You are Aethereal Cardinal autonomous task recovery engine.
                The agent was executing:
                Goal: "$goal"
                Failed Step: [${failedStep.toolName}] "${failedStep.title}"
                Reason for Failure: "$failureReason"
                Active Screen: "${perception.screenTitle}" (${perception.activeAppName})
                
                Propose an alternative single PlanStep in JSON with keys:
                "toolName", "title", "description", "params" (key-value strings).
            """.trimIndent()

            val model = config.modelName.ifBlank { "gemini-2.0-flash" }
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

            val jsonBody = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().put("text", prompt))
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.1)
                    put("responseMimeType", "application/json")
                })
            }

            val request = Request.Builder()
                .url(url)
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) return@withContext fallbackPlanner.replanStep(goal, failedStep, failureReason, perception, config)

            val responseStr = response.body?.string() ?: return@withContext null
            val rawText = JSONObject(responseStr)
                .getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")

            val obj = JSONObject(rawText)
            val toolName = obj.optString("toolName", "search_web")
            val title = obj.optString("title", "Alternative Action")
            val desc = obj.optString("description", "Replanned step to achieve goal")
            val paramsObj = obj.optJSONObject("params")
            val params = mutableMapOf<String, String>()
            paramsObj?.keys()?.forEach { k ->
                params[k] = paramsObj.getString(k)
            }

            PlanStep(
                id = UUID.randomUUID().toString(),
                stepIndex = failedStep.stepIndex,
                toolName = toolName,
                title = title,
                description = desc,
                params = params,
                parentStepId = failedStep.id
            )
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            fallbackPlanner.replanStep(goal, failedStep, failureReason, perception, config)
        }
    }

    fun sanitizeUrl(url: String): String {
        return url.replace(Regex("key=[^&]+"), "key=[REDACTED]")
    }

    private fun resolveApiKey(config: LLMConfig): String {
        if (config.apiKey.isNotBlank()) return config.apiKey
        return try {
            val field = BuildConfig::class.java.getDeclaredField("GEMINI_API_KEY")
            field.get(null)?.toString().orEmpty()
        } catch (_: Exception) {
            ""
        }
    }

    private fun buildPlanPrompt(goal: String, perception: PerceptionContext): String {
        val toolsSummary = ActionRegistry.allActions.joinToString("\n") { action ->
            val paramsStr = action.parameters.joinToString(", ") { "${it.name}: ${it.type} (${it.description})" }
            "- ${action.id}: ${action.description} [Risk: ${action.riskLevel.name}, Params: $paramsStr]"
        }

        return """
            You are AETHEREAL CARDINAL, an autonomous on-device Android AI agent.
            Decompose the following user goal into an ordered JSON array of executable task steps.
            
            USER GOAL: "$goal"
            
            ACTIVE DEVICE STATE:
            - Active App: ${perception.activeAppName} (${perception.activePackage})
            - Screen: ${perception.screenTitle}
            - Accessibility Active: ${perception.isAccessibilityActive}
            - Visible text: "${perception.rawTextSummary}"
            
            AVAILABLE SYSTEM TOOLS:
            $toolsSummary
            
            Respond strictly with a JSON array where each object has:
            - "toolName": must match one of the available tool ids above
            - "title": short human-readable action title (3-6 words)
            - "description": rationale for why this step is necessary
            - "params": map of required string parameters
        """.trimIndent()
    }

    private suspend fun parseStepsFromJson(rawJson: String, fallbackGoal: String): List<PlanStep> {
        val steps = mutableListOf<PlanStep>()
        try {
            val array = JSONArray(rawJson.trim())
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val toolName = obj.optString("toolName", "inspect_screen")
                val title = obj.optString("title", "Step ${i + 1}")
                val description = obj.optString("description", "")
                val paramsObj = obj.optJSONObject("params")
                val params = mutableMapOf<String, String>()
                paramsObj?.keys()?.forEach { k ->
                    params[k] = paramsObj.getString(k)
                }

                steps.add(
                    PlanStep(
                        id = UUID.randomUUID().toString(),
                        stepIndex = i + 1,
                        toolName = toolName,
                        title = title,
                        description = description,
                        params = params
                    )
                )
            }
        } catch (_: Exception) {
            return fallbackPlanner.generatePlan(fallbackGoal, PerceptionContext(), LLMConfig())
        }
        return if (steps.isNotEmpty()) steps else fallbackPlanner.generatePlan(fallbackGoal, PerceptionContext(), LLMConfig())
    }
}
