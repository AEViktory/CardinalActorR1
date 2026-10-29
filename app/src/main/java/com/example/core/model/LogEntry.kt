package com.example.core.model

enum class LogLevel {
    INFO,
    PLAN,
    EXEC,
    PERCEIVE,
    WARN,
    ERROR
}

data class LogEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val tag: String,
    val message: String,
    val level: LogLevel = LogLevel.INFO
)
