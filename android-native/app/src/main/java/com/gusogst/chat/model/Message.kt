package com.gusogst.chat.model

data class Message(
    val id: String = System.currentTimeMillis().toString(),
    val role: Role,
    val content: String,
    val thinking: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isStreaming: Boolean = false
)

enum class Role {
    USER, ASSISTANT, SYSTEM
}
