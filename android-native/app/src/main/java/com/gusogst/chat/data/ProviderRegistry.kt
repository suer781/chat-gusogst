package com.gusogst.chat.data

/**
 * Provider definitions - mirrors Web providers/definitions/agent-core.ts
 */
data class ProviderDef(
    val id: String,
    val name: String,
    val baseUrl: String,
    val models: List<String>,
    val authType: String = "api_key", // api_key / oauth / none
    val transport: String = "http",   // http / websocket / local
    val isAggregator: Boolean = false,
    val isLocal: Boolean = false
)

object ProviderRegistry {
    val PROVIDERS = listOf(
        ProviderDef("openai", "OpenAI", "https://api.openai.com/v1", listOf("gpt-4o", "gpt-4o-mini", "gpt-4-turbo", "gpt-3.5-turbo")),
        ProviderDef("anthropic", "Anthropic", "https://api.anthropic.com/v1", listOf("claude-3-opus", "claude-3-sonnet", "claude-3-haiku")),
        ProviderDef("ollama", "Ollama", "http://localhost:11434/v1", listOf("llama3", "mistral", "codellama"), authType = "none", isLocal = true),
        ProviderDef("google", "Google", "https://generativelanguage.googleapis.com/v1", listOf("gemini-pro", "gemini-ultra")),
        ProviderDef("deepseek", "DeepSeek", "https://api.deepseek.com/v1", listOf("deepseek-chat", "deepseek-coder")),
        ProviderDef("qwen", "Qwen", "https://dashscope.aliyuncs.com/api/v1", listOf("qwen-max", "qwen-plus", "qwen-turbo")),
        ProviderDef("zhipu", "ZhiPu", "https://open.bigmodel.cn/api/paas/v4", listOf("glm-4", "glm-3-turbo")),
        ProviderDef("moonshot", "Moonshot", "https://api.moonshot.cn/v1", listOf("moonshot-v1-128k", "moonshot-v1-32k")),
        ProviderDef("openrouter", "OpenRouter", "https://openrouter.ai/api/v1", listOf(), isAggregator = true),
        ProviderDef("together", "Together", "https://api.together.xyz/v1", listOf(), isAggregator = true),
        ProviderDef("groq", "Groq", "https://api.groq.com/openai/v1", listOf("llama3-70b", "mixtral-8x7b")),
        ProviderDef("lmstudio", "LM Studio", "http://localhost:1234/v1", listOf(), authType = "none", isLocal = true),
        ProviderDef("custom", "\u81EA\u5B9A\u4E49", "", listOf())
    )

    val ALIASES = mapOf("openai-compatible" to "openai")

    fun getById(id: String): ProviderDef? = PROVIDERS.find { it.id == (ALIASES[id] ?: id) }
    fun getAggregators(): List<ProviderDef> = PROVIDERS.filter { it.isAggregator }
    fun getLocal(): List<ProviderDef> = PROVIDERS.filter { it.isLocal }
}
