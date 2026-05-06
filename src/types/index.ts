/** Chat Gusogst 类型定义 — 融合 Hermes Agent + Chatbox */

// ── 消息 ────────────────────────────── (from Hermes turnStore pattern)
export type MessageRole = 'user' | 'assistant' | 'system';
export type MessageStatus = 'sending' | 'sent' | 'error';

export interface Message {
  id: string;
  role: MessageRole;
  content: string;
  timestamp: number;
  status?: MessageStatus;
  tokens?: number;
  /** Hermes-style: 工具调用记录 */
  tools?: ToolCall[];
  /** Hermes-style: 推理过程 */
  reasoning?: string;
}

export interface ToolCall {
  name: string;
  input?: string;
  output?: string;
  durationMs?: number;
}

// ── 会话 ────────────────────────────── (from Hermes session concept)
export interface Conversation {
  id: string;
  title: string;
  agentId: string;
  createdAt: number;
  lastMessage?: string;
  lastMessageTime?: number;
  pinned?: boolean;
  /** Hermes-style: 会话摘要 */
  summary?: string;
  /** 消息计数 */
  messageCount?: number;
}

// ── 模型提供方 ────────────────────────── (from Chatbox provider_settings)
export type ProviderType = 'openai' | 'anthropic' | 'deepseek' | 'gemini' | 'custom';

export interface ModelProvider {
  id: string;
  name: string;
  type: ProviderType;
  apiKey: string;
  apiUrl: string;
  model: string;
  enabled: boolean;
  /** Chatbox-style: 模型列表 */
  availableModels?: string[];
}

export interface ProviderPreset {
  type: ProviderType;
  name: string;
  defaultUrl: string;
  defaultModel: string;
  models: string[];
  icon: string;
}

// ── Agent 人设 ──────────────────────────
export interface Agent {
  id: string;
  name: string;
  avatar: string;
  personality: string;
  /** 主动聊天开关 */
  proactiveEnabled: boolean;
  /** 主动聊天间隔(分钟) */
  proactiveInterval: number;
  /** Hermes-style: 使用的语言风格 */
  tone?: 'casual' | 'formal' | 'playful' | 'caring';
}

// ── 全局设置 ──────────────────────────
export interface Settings {
  providers: ModelProvider[];
  activeProviderId: string;
  agent: Agent;
  ttsEnabled: boolean;
  hapticEnabled: boolean;
  memoryEnabled: boolean;
  /** 主动聊天功能 */
  proactiveEnabled: boolean;
}

// ── 记忆 ────────────────────────────── (from Hermes memory concept)
export interface MemoryEntry {
  id: string;
  conversationId: string;
  summary: string;
  importance: number;
  timestamp: number;
  keywords: string[];
}

// ── 流式回调 ──────────────────────────
export interface StreamCallbacks {
  onToken: (token: string) => void;
  onComplete: (fullText: string, tokens?: number) => void;
  onError: (error: string) => void;
}

// ── 外部平台 ────────────────────────── (from Hermes gateway)
export type PlatformType = 'local' | 'wechat' | 'qq' | 'telegram' | 'whatsapp';

export interface PlatformConfig {
  type: PlatformType;
  enabled: boolean;
  /** 平台特定配置 */
  config: Record<string, string>;
}
