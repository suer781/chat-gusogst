/**
 * Agent Core - 驱动 Agent Loop
 * 基于 Hermes agent.py 重写
 */
import type { Message, AgentConfig, AgentEvent, ToolCall, SearchResult } from '../../shared/types'
import type { ProviderAdapter } from '../../shared/types'
import { MemoryManager } from '../memory/manager'
import { ToolRegistry } from '../tools/registry'

export class Agent {
  private history: Message[] = []
  private memory: MemoryManager
  private tools: ToolRegistry
  private config: AgentConfig
  private abortController: AbortController | null = null

  constructor(config: AgentConfig) {
    this.config = config
    this.memory = new MemoryManager()
    this.tools = new ToolRegistry()
  }

  updateConfig(config: Partial<AgentConfig>) {
    this.config = { ...this.config, ...config }
  }

  getHistory(): Message[] {
    return [...this.history]
  }

  clearHistory() {
    this.history = []
  }

  /**
   * 核心 Agent Loop
   * 流式生成器，逐步输出事件
   */
  async *sendMessage(userContent: string): AsyncGenerator<AgentEvent> {
    this.abortController = new AbortController()

    try {
      // 添加用户消息
      const userMsg: Message = {
        id: crypto.randomUUID(),
        role: 'user',
        content: userContent,
        timestamp: Date.now(),
      }
      this.history.push(userMsg)

      // 构建上下文
      const context = await this.buildContext(userContent)

      // 获取 provider 和工具
      const provider = this.getProvider()
      const toolDefs = this.tools.getToolDefinitions()
      const maxRounds = this.config.maxRounds || 10

      // Agent Loop
      for (let round = 0; round < maxRounds; round++) {
        if (this.abortController.signal.aborted) {
          yield { type: 'error', data: '请求已中止' }
          return
        }

        // 流式调用模型
        const response = await this.callModel(provider, context, toolDefs)

        // 处理工具调用
        if (response.tool_calls && response.tool_calls.length > 0) {
          // 添加助手消息（含工具调用）
          const assistantMsg: Message = {
            id: crypto.randomUUID(),
            role: 'assistant',
            content: response.content || '',
            timestamp: Date.now(),
            tool_calls: response.tool_calls,
          }
          this.history.push(assistantMsg)
          context.push(assistantMsg)

          // 依次执行工具调用
          for (const toolCall of response.tool_calls) {
            yield { type: 'tool_call', data: toolCall, name: toolCall.function.name }

            try {
              const result = await this.tools.execute(toolCall.function.name, JSON.parse(toolCall.function.arguments))
              const toolMsg: Message = {
                id: crypto.randomUUID(),
                role: 'tool',
                content: JSON.stringify(result),
                timestamp: Date.now(),
                tool_call_id: toolCall.id,
              }
              this.history.push(toolMsg)
              context.push(toolMsg)
              yield { type: 'tool_result', data: result, message: JSON.stringify(result) }
            } catch (error: any) {
              const errorMsg: Message = {
                id: crypto.randomUUID(),
                role: 'tool',
                content: `工具执行失败: ${error.message}`,
                timestamp: Date.now(),
                tool_call_id: toolCall.id,
              }
              this.history.push(errorMsg)
              context.push(errorMsg)
              yield { type: 'tool_result', data: error, message: `工具执行失败: ${error.message}` }
            }
          }

          // 继续循环，让模型处理工具结果
          continue
        }

        // 无工具调用，纯文本回复
        const botMsg: Message = {
          id: crypto.randomUUID(),
          role: 'assistant',
          content: response.content || '',
          timestamp: Date.now(),
        }
        this.history.push(botMsg)

        yield { type: 'token', data: response.content, content: response.content || '' }
        yield { type: 'done', data: botMsg }

        // 异步保存记忆
        this.saveMemory(userContent, response.content || '')
        return
      }

      // 达到最大轮次
      yield { type: 'error', data: `达到最大轮次 ${maxRounds}，请简化请求` }
    } catch (error: any) {
      yield { type: 'error', data: error.message }
    } finally {
      this.abortController = null
    }
  }

  /**
   * 中止当前请求
   */
  abort() {
    this.abortController?.abort()
  }

  // ── 内部方法 ──────────────────────

  private getProvider(): ProviderAdapter {
    // TODO: 根据 config.model.provider 获取对应的 provider
    // 暂时返回一个模拟的 provider
    return {
      name: 'openai',
      displayName: 'OpenAI',
      apiMode: 'chat_completions',
      baseUrl: this.config.model.baseUrl,
      authType: 'api_key',
    }
  }

  private async callModel(
    provider: ProviderAdapter,
    context: Message[],
    tools: any[]
  ): Promise<{ content?: string; tool_calls?: ToolCall[] }> {
    const { baseUrl, apiKey } = this.config.model
    const model = this.config.model.model
    const temperature = this.config.model.temperature
    const maxTokens = this.config.model.maxTokens

    // 构建请求体
    const body: any = {
      model,
      messages: context.map(m => ({
        role: m.role,
        content: m.content,
        ...(m.tool_calls ? { tool_calls: m.tool_calls } : {}),
        ...(m.tool_call_id ? { tool_call_id: m.tool_call_id } : {}),
      })),
      temperature,
      max_tokens: maxTokens,
      stream: false,
    }

    // 如果有工具，添加 tools
    if (tools.length > 0) {
      body.tools = tools.map(t => ({
        type: 'function',
        function: {
          name: t.name,
          description: t.description,
          parameters: t.parameters,
        },
      }))
    }

    const response = await fetch(`${baseUrl}/chat/completions`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${apiKey}`,
      },
      body: JSON.stringify(body),
      signal: this.abortController?.signal,
    })

    if (!response.ok) {
      const error = await response.text()
      throw new Error(`模型调用失败: ${response.status} ${error}`)
    }

    const data = await response.json()
    const message = data.choices?.[0]?.message

    return {
      content: message?.content,
      tool_calls: message?.tool_calls,
    }
  }

  private async buildContext(userContent: string): Promise<Message[]> {
    const context: Message[] = []

    // 1. 系统提示
    const systemPrompt = this.buildSystemPrompt()
    if (systemPrompt) {
      context.push({
        id: 'system',
        role: 'system',
        content: systemPrompt,
        timestamp: 0,
      })
    }

    // 2. 相关记忆
    if (this.config.memoryEnabled) {
      const memories = await this.memory.search(userContent, 5)
      if (memories.length > 0) {
        const memoryContent = memories.map(m => `- ${m.content}`).join('\n')
        context.push({
          id: 'memories',
          role: 'system',
          content: `相关记忆：\n${memoryContent}`,
          timestamp: 0,
        })
      }
    }

    // 3. 历史消息（截断）
    const maxHistoryTokens = this.config.maxHistoryTokens || 4000
    const truncatedHistory = this.truncateHistory(maxHistoryTokens)
    context.push(...truncatedHistory)

    // 4. 当前用户消息
    context.push({
      id: crypto.randomUUID(),
      role: 'user',
      content: userContent,
      timestamp: Date.now(),
    })

    return context
  }

  private buildSystemPrompt(): string {
    const persona = this.config.persona
    if (!persona) return ''

    const parts: string[] = [persona.systemPrompt]

    // 当前时间
    const now = new Date()
    const timeStr = now.toLocaleString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
    })
    parts.push(`当前时间：${timeStr}`)

    // 搜索能力
    if (this.config.searchEnabled) {
      parts.push('你可以使用搜索工具获取最新信息。')
    }

    return parts.join('\n\n')
  }

  private truncateHistory(maxTokens: number): Message[] {
    // 粗略估计：1 token ≈ 2 字符
    const maxChars = maxTokens * 2
    let totalChars = 0
    const result: Message[] = []

    // 从最新消息向前截断
    for (let i = this.history.length - 1; i >= 0; i--) {
      const msg = this.history[i]
      const msgChars = msg.content.length + (msg.tool_calls ? JSON.stringify(msg.tool_calls).length : 0)
      if (totalChars + msgChars > maxChars) break
      totalChars += msgChars
      result.unshift(msg)
    }

    return result
  }

  private async saveMemory(userMsg: string, botMsg: string) {
    try {
      // 取最近 10 条历史
      const recentHistory = this.history.slice(-10)
      await this.memory.extractAndSave(recentHistory)
    } catch (error) {
      console.error('保存记忆失败:', error)
    }
  }
}
