package com.gusogst.chat.data

import com.gusogst.chat.model.UISettings
import com.gusogst.chat.model.UIProvider

/**
 * Agent bridge - mirrors Web bridge.ts
 * Converts app settings to AgentConfig, handles stream events
 */
object AgentBridge {

    fun settingsToConfig(
        settings: UISettings,
        providers: List<UIProvider>,
        systemPrompt: String = ""
    ): AgentConfig {
        val activeProvider = providers.firstOrNull { it.enabled }
        return AgentConfig(
            apiKey = activeProvider?.apiKey ?: "",
            baseUrl = activeProvider?.baseUrl ?: "https://api.openai.com/v1",
            model = settings.modelName.ifEmpty { "gpt-4o-mini" },
            temperature = settings.temperature,
            topP = settings.topP,
            maxTokens = settings.maxTokens,
            systemPrompt = systemPrompt
        )
    }

    fun buildMessages(
        systemPrompt: String,
        history: List<com.gusogst.chat.model.UIMessage>
    ): List<Message> {
        val messages = mutableListOf<Message>()
        if (systemPrompt.isNotEmpty()) {
            messages.add(Message(role = "system", content = systemPrompt))
        }
        for (msg in history) {
            messages.add(Message(
                id = msg.id,
                role = msg.role,
                content = msg.content
            ))
        }
        return messages
    }
}
