package com.gusogst.chat.model

enum class ProviderType(val displayName: String) {
    OPENAI("OpenAI"),
    ANTHROPIC("Anthropic"),
    GEMINI("Gemini"),
    DEEPSEEK("DeepSeek"),
    OLLAMA("Ollama"),
    CUSTOM("Custom");

    companion object {
        fun fromString(value: String): ProviderType {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: CUSTOM
        }
    }
}
