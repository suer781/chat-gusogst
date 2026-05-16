// app/src/adapter/interface.ts
// UI 隔离层接口定义
// UI 代码只依赖此接口，不关心后端是 HTTP / Bridge / 直连

export interface Attachment {
  type: 'image' | 'file'
  name: string
  mimeType: string
  /** base64 或 file URI */
  data: string
}

export interface AdapterChunk {
  type: 'text-delta' | 'tool-call' | 'tool-result' | 'error' | 'finish'
  textDelta?: string
  toolCallId?: string
  toolName?: string
  args?: Record<string, unknown>
  result?: unknown
  error?: string
  tokensUsed?: number
}

export interface SessionSummary {
  id: string
  title: string
  lastMessage?: string
  updatedAt: number
}

export interface SessionConfig {
  title?: string
  personaId?: string
  modelId?: string
  systemPrompt?: string
}

export interface UIAdapter {
  sendMessage(
    sessionId: string,
    content: string,
    attachments?: Attachment[]
  ): AsyncGenerator<AdapterChunk>

  abortGeneration(): void

  listSessions(): Promise<SessionSummary[]>
  createSession(config?: SessionConfig): Promise<string>
  deleteSession(id: string): Promise<void>
  renameSession(id: string, title: string): Promise<void>
}
