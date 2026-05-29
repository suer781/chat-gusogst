package com.gusogst.chat.model

import com.google.gson.annotations.SerializedName

// ===== 消息 =====
// [NOTE] AgentBridge 内部也有 Message 类（API 数据层），这里用 typealias 避免命名冲突，删除会导致 AgentBridge 编译失败（2026-05-22）

data class Message(
    val id: String = java.util.UUID.randomUUID().toString(),
    val conversationId: String = "",
    val role: Role = Role.user,
    var content: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    var status: MessageStatus = MessageStatus.ready,
    var thinking: String? = null,
    val thinkingCollapsed: Boolean = true,
    var providerId: String? = null,
    var modelId: String? = null,
    val toolCalls: List<ToolCall>? = null,
    val attachments: List<Attachment>? = null
)

// [NOTE] enum 值曾因大写引用（Role.USER）导致 CI 编译失败，改 enum 定义时务必同步更新所有引用（2026-05-22）
enum class Role { system, user, assistant }
// [NOTE] 同 Role，大小写必须严格匹配，CI 不容错（2026-05-22）
enum class MessageStatus { streaming, ready, error }

data class ToolCall(
    val id: String = "",
    val name: String = "",
    val arguments: String = "",
    var result: String? = null
)

data class Attachment(
    val type: String = "",  // "image"
    val url: String = "",
    val name: String? = null,
    val mimeType: String? = null,
    val size: Long? = null
)

// ===== 对话 =====

data class ModelParamsConfig(
    val temperature: Float = 0.7f,
    val topP: Float = 0.9f,
    val maxTokens: Int = 2048,
    val overrideGlobal: Boolean = false,
    val autoMode: String = "off"
)

data class Conversation(
    val id: String = java.util.UUID.randomUUID().toString(),
    var title: String = "新对话",
    val messages: MutableList<Message> = mutableListOf(),
    val createdAt: Long = System.currentTimeMillis(),
    var updatedAt: Long = System.currentTimeMillis(),
    var personaId: String? = null,
    var providerId: String? = null,
    var modelId: String? = null,
    var lastProviderId: String? = null,
    var lastModelId: String? = null,
    var settings: ConversationSettings? = null
)

data class ConversationSettings(
    val fontSize: String? = null,
    val enableThinking: Boolean? = null,
    val thinkingAutoExpand: Boolean? = null,
    val toolCallAutoExpand: Boolean? = null
)

// ===== 角色 =====
data class PersonalityTraits(
    val calm: Float = 0.5f,
    val warm: Float = 0.5f,
    val analytical: Float = 0.5f,
    val creative: Float = 0.5f,
    val curious: Float = 0.5f,
    val precise: Float = 0.5f,
    val playful: Float = 0.5f,
    val energetic: Float = 0.5f
)

data class Persona(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String = "",
    val avatar: String = "",
    val avatarType: AvatarType = AvatarType.emoji,
    val prompt: String = "",
    var bgColor: String = "",
    var textColor: String = "",
    val tags: List<String> = emptyList(),
    val personality: PersonalityTraits = PersonalityTraits(),
    val modelParamsConfig: ModelParamsConfig? = null
)

// [NOTE] 同上
enum class AvatarType { url, blob, base64, emoji }

data class PresetPersona(
    val persona: Persona,
    val category: String = ""
)


// ===== Model =====
data class ModelInfo(
    val id: String,
    val name: String? = null,
    val contextLength: Long = 0
)

// ===== 服务商 =====
data class UIProvider(
    val id: String = java.util.UUID.randomUUID().toString(),
    var name: String = "",
    var isCustom: Boolean = false,
    var baseUrl: String = "",
    var apiKey: String = "",
    val models: MutableList<ModelInfo> = mutableListOf(),
    var enabled: Boolean = true,
    var lastUpdated: Long = 0L  // 隐藏更新时间戳，用于检测数据是否过时
)

// ===== 设置 =====
data class UISettings(
    val theme: String = "system",
    val glassEnabled: Boolean = false,
    val glassOpacity: Int = 80,
    val enableThinking: Boolean = true,
    val thinkingAutoExpand: Boolean = false,
    val toolCallAutoExpand: Boolean = false,
    val developerMode: Boolean = false,
    val selectedFont: String = "default",
    val fontSize: String = "md",
    val hapticEnabled: Boolean = true,
    val eyeCareMode: Boolean = false,
    val eyeCareWarmth: Int = 0,
    val eyeCareIntensity: Int = 40,
    val hdrEnabled: Boolean = false,
    val bgAnimationEnabled: Boolean = true,
    val searchEnabled: Boolean = false,
    val activeSearchEngine: String = "duckduckgo"
)

enum class ThemeMode { system, light, dark, pureWhite, pureBlack }
enum class DisplayMode { compact, default, expanded }

// ===== API 请求/响应 =====
data class ChatRequest(
    val model: String,
    val messages: List<ApiMessage>,
    val stream: Boolean = true,
    val temperature: Float = 0.7f,
    val max_tokens: Int? = null
)

data class ApiMessage(
    val role: String,
    val content: String
)

data class ChatResponse(
    val id: String = "",
    val choices: List<Choice> = emptyList()
)

data class Choice(
    val index: Int = 0,
    val delta: Delta? = null,
    val message: ApiMessage? = null
)

data class Delta(
    val role: String? = null,
    val content: String? = null,
    val reasoning_content: String? = null
)
