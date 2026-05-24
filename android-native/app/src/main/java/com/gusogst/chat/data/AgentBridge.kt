package com.gusogst.chat.data

import com.gusogst.chat.model.Message
import com.gusogst.chat.model.UISettings
import com.gusogst.chat.model.UIProvider

/**
 * Agent bridge —— 转换 UI 层数据为 API 层数据。
 *
 * 命名说明：
 *   Message / UISettings / UIProvider    → model 包（UI 层）
 *   ApiMessage / AgentConfig / ApiToolCall → data 包（API 层，见 AgentTypes.kt）
 *   两者命名不同源，import 无冲突，无需 typealias 绕路。
 */
object AgentBridge {

    /**
     * 将 UI 设置 + 供应商列表转为 AgentConfig。
     * 取第一个启用的供应商，若无则用 OpenAI 默认值。
     */
    fun settingsToConfig(
        settings: UISettings,
        providers: List<UIProvider>,
        systemPrompt: String = ""
    ): AgentConfig {
        val activeProvider = providers.firstOrNull { it.enabled }
        return AgentConfig(
            apiKey = activeProvider?.apiKey ?: "",
            baseUrl = activeProvider?.baseUrl ?: "https://api.openai.com/v1",
            model = activeProvider?.models?.firstOrNull()?.id ?: "gpt-4o-mini",
            systemPrompt = systemPrompt
        )
    }

    /**
     * 将 UI 层的 Message 列表转为 API 层的 ApiMessage 列表。
     * 自动插入 system prompt 作为首条。
     */
    fun buildMessages(
        systemPrompt: String,
        history: List<Message>
    ): List<ApiMessage> {
        val messages = mutableListOf<ApiMessage>()
        if (systemPrompt.isNotEmpty()) {
            messages.add(ApiMessage(role = "system", content = systemPrompt))
        }
        for (msg in history) {
            messages.add(ApiMessage(
                id = msg.id,
                role = msg.role.name,
                content = msg.content
            ))
        }
        return messages
    }
}
