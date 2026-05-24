package com.gusogst.chat.data

/**
 * Agent bridge —— 转换 UI 层数据为 API 层数据。
 *
 * 命名说明：
 *   Message / UISettings / UIProvider    → model 包（UI 层）
 *   ApiMessage / AgentConfig / ApiToolCall → data 包（API 层，见 AgentTypes.kt）
 *
 * 注意：ChatViewModel 目前直接构建 ApiMessage（callAiApi 内），
 * 不经过此 Bridge。如需统一入口，将 ChatViewModel 的构建逻辑迁入此处。
 */
object AgentBridge {
    // 原 settingsToConfig() / buildMessages() 已移除（未被调用）
    // 历史版本见 git log
}
