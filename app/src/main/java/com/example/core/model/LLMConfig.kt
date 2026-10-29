package com.example.core.model

enum class LLMProviderType(val displayName: String) {
    GEMINI("Google Gemini (AI Studio)"),
    OPENAI("OpenAI Compatible"),
    GROQ("Groq High-Speed"),
    OLLAMA("Ollama Local AI")
}

data class LLMConfig(
    val provider: LLMProviderType = LLMProviderType.GEMINI,
    val apiKey: String = "",
    val modelName: String = "gemini-2.0-flash",
    val baseUrl: String = "",
    val temperature: Float = 0.2f,
    val maxRetries: Int = 3,
    val isAutoExecutionEnabled: Boolean = true
)
