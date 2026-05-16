/**
 * Agent Core - 驱动 Agent Loop
 * 修复：provider registry + 流式输出 + 外部 tools
 */
import type { Message, AgentConfig, AgentEvent, ToolCall } from '../../shared/types'
import type { ProviderAdapter } from '../../shared/types'
import { MemoryManager } from '../memory/manager'
import { ToolRegistry } from '../tools/registry'
import { getProvider as getProviderFromRegistry } from '../providers'

export class Agent {
  private history: Message[] = []
  private memory: MemoryManager
  private tools: ToolRegistry
  private config: AgentConfig
  private abortController: AbortController | null = null
  private resolvedProvider: ProviderAdapter | null = null

  constructor(config: AgentConfig, externalTools?: ToolRegistry) {
    this.config = config
    this.memory = new MemoryManager()
    this.tools = externalTools || new ToolRegistry()
    this.resolvedProvider = this.resolveProvider()
  }

  updateConfig(config: Partial<AgentConfig>) {
    const needNewProvider = config.model?.provider && config.model.provider !== this.config.model?.provider
    this.config = { ...this.config, ...config }
    if (needNewProvider) this.resolvedProvider = this.resolveProvider()
  }

  getHistory(): Message[] { return [...this.history] }
  clearHistory() { this.history = [] }

  async *sendMessage(userContent: string): AsyncGenerator<AgentEvent> {
    this.abortController = new AbortController()
    try {
      const userMsg: Message = { id: crypto.randomUUID(), role: 'user', content: userContent, timestamp: Date.now() }
      this.history.push(userMsg)
      const context = await this.buildContext(userContent)
      const toolDefs = this.tools.getToolDefinitions()
      const maxRounds = this.config.maxRounds || 10

      for (let round = 0; round < maxRounds; round++) {
        if (this.abortController.signal.aborted) { yield { type: 'error', data: '请求已中止' }; return }
        const provider = this.getProvider()
        const { content, toolCalls } = await this.callModelStreaming(provider, context, toolDefs)

        if (toolCalls && toolCalls.length > 0) {
          const assistantMsg: Message = { id: crypto.randomUUID(), role: 'assistant', content: content || '', timestamp: Date.now(), tool_calls: toolCalls }
          this.history.push(assistantMsg)
          for (const tc of toolCalls) {
            yield { type: 'tool_call', data: tc }
            try {
              const result = await this.tools.execute(tc.function.name, JSON.parse(tc.function.arguments || '{}'))
              const toolMsg: Message = { id: crypto.randomUUID(), role: 'tool', content: result, timestamp: Date.now(), tool_call_id: tc.id }
              this.history.push(toolMsg)
              yield { type: 'tool_result', data: { id: tc.id, result } }
            } catch (error: any) {
              const errText = `工具执行失败: ${error.message}`
              this.history.push({ id: crypto.randomUUID(), role: 'tool', content: errText, timestamp: Date.now(), tool_call_id: tc.id })
              yield { type: 'tool_result', data: { id: tc.id, result: errText } }
            }
          }
          continue
        }
        const assistantMsg: Message = { id: crypto.randomUUID(), role: 'assistant', content: content || '', timestamp: Date.now() }
        this.history.push(assistantMsg)
        yield { type: 'token', data: content || '' }
        yield { type: 'done', data: '' }
        this.saveMemory(userContent, content || '').catch(() => {})
        return
      }
      yield { type: 'error', data: `达到最大轮次 ${maxRounds}，请简化请求` }
    } catch (error: any) {
      yield { type: 'error', data: error.message || '未知错误' }
    } finally {
      this.abortController = null
    }
  }

  abort() { this.abortController?.abort() }

  private getProvider(): ProviderAdapter { return this.resolvedProvider || this.resolveProvider() }

  private resolveProvider(): ProviderAdapter {
    const providerName = this.config.model?.provider || 'openai'
    try {
      const p = getProviderFromRegistry(providerName)
      this.resolvedProvider = p
      return p
    } catch {
      const fallback = { name: providerName, displayName: providerName, apiMode: 'chat_completions' as const, baseUrl: this.config.model.baseUrl, authType: 'api_key' as const } as ProviderAdapter
      this.resolvedProvider = fallback
      return fallback
    }
  }

  private async callModelStreaming(provider: ProviderAdapter, context: Message[], tools: any[]): Promise<{ content: string; toolCalls?: ToolCall[] }> {
    const p = provider as any
    const toolDefs = tools.length > 0 ? tools : undefined
    if (typeof p.chatStream === 'function') {
      let fullContent = ''
      const stream = p.chatStream(context, this.config.model, toolDefs)
      for await (const chunk of stream) {
        if (this.abortController?.signal.aborted) break
        fullContent += chunk
      }
      const toolCalls = p._lastStreamToolCalls as ToolCall[] | undefined
      return { content: fullContent, toolCalls: toolCalls?.length ? toolCalls : undefined }
    }
    if (typeof p.chat === 'function') {
      const msg = await p.chat(context, this.config.model, toolDefs)
      return { content: msg.content || '', toolCalls: msg.tool_calls }
    }
    return this.rawFetch(context, tools)
  }

  private async rawFetch(context: Message[], tools: any[]): Promise<{ content: string; toolCalls?: ToolCall[] }> {
    const { baseUrl, apiKey } = this.config.model
    const model = this.config.model.model
    const body: any = { model, messages: context.map(m => ({ role: m.role, content: m.content, ...(m.tool_calls ? { tool_calls: m.tool_calls } : {}), ...(m.tool_call_id ? { tool_call_id: m.tool_call_id } : {}) })), temperature: this.config.model.temperature, max_tokens: this.config.model.maxTokens, stream: false }
    if (tools.length > 0) body.tools = tools.map(t => ({ type: 'function', function: { name: t.name, description: t.description, parameters: t.parameters } }))
    const response = await fetch(`${baseUrl}/chat/completions`, { method: 'POST', headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${apiKey}` }, body: JSON.stringify(body), signal: this.abortController?.signal })
    if (!response.ok) { const error = await response.text(); throw new Error(`模型调用失败: ${response.status} ${error}`) }
    const data = await response.json()
    const message = data.choices?.[0]?.message
    return { content: message?.content, toolCalls: message?.tool_calls }
  }

  private async buildContext(userContent: string): Promise<Message[]> {
    const context: Message[] = []
    const systemPrompt = this.buildSystemPrompt()
    if (systemPrompt) context.push({ id: 'system', role: 'system', content: systemPrompt, timestamp: 0 })
    if (this.config.memoryEnabled) {
      const memories = await this.memory.search(userContent, 5)
      if (memories.length > 0) {
        const memoryContent = memories.map(m => `- ${m.content}`).join('\n')
        context.push({ id: 'memories', role: 'system', content: `相关记忆：\n${memoryContent}`, timestamp: 0 })
      }
    }
    const maxHistoryTokens = this.config.maxHistoryTokens || 4000
    context.push(...this.truncateHistory(maxHistoryTokens))
    context.push({ id: crypto.randomUUID(), role: 'user', content: userContent, timestamp: Date.now() })
    return context
  }

  private buildSystemPrompt(): string {
    const persona = this.config.persona
    if (!persona) return ''
    const parts: string[] = [persona.systemPrompt]
    const now = new Date()
    const timeStr = now.toLocaleString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
    parts.push(`当前时间：${timeStr}`)
    if (this.config.searchEnabled) parts.push('你可以使用搜索工具获取最新信息。')
    return parts.join('\n\n')
  }

  private truncateHistory(maxTokens: number): Message[] {
    const maxChars = maxTokens * 2
    let totalChars = 0
    const result: Message[] = []
    for (let i = this.history.length - 1; i >= 0; i--) {
      const msg = this.history[i]
      const msgChars = msg.content.length + (msg.tool_calls ? JSON.stringify(msg.tool_calls).length : 0)
      if (totalChars + msgChars > maxChars) break
      totalChars += msgChars
      result.unshift(msg)
    }
    return result
  }

  private async saveMemory(_userMsg: string, _botMsg: string) {
    try { await this.memory.extractAndSave(this.history.slice(-10)) } catch (error) { console.error('保存记忆失败:', error) }
  }
}
