/**
 * Agent Core — Hermes agent loop 的 TypeScript 重写
 * 流程: 用户消息 → build_context → 调模型 → tool_calls? → 执行 → 再调 → 返回
 */
import type { Message, AgentConfig, AgentEvent, ToolDefinition } from '../../shared/types'
import { buildChannelInstruction } from './channel'
import { getProvider } from '../providers'
import { MemoryManager } from '../memory/manager'
import { ToolRegistry } from '../tools/registry'

type EventHandler = (event: AgentEvent) => void

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
    Object.assign(this.config, config)
  }

  getHistory(): Message[] { return [...this.history] }
  clearHistory() { this.history = [] }

  /**
   * 核心：发送消息并处理 agent loop
   */
  async *sendMessage(userContent: string): AsyncGenerator<AgentEvent> {
    this.abortController = new AbortController()

    // 1. 构建上下文
    const context = await this.buildContext(userContent)

    // 2. 获取 provider 和工具定义
    const provider = getProvider(this.config.model.provider)
    const toolDefs = this.tools.getToolDefinitions()

    // 3. Agent loop（最多 maxRounds 轮 tool call）
    let rounds = 0
    while (rounds < this.config.maxRounds) {
      rounds++
      let response: Message

      try {
        // 流式调用，同时累积 tool_calls
        let fullContent = ""
        for await (const token of provider.chatStream(context, this.config.model, toolDefs)) {
          if (this.abortController?.signal.aborted) break
          fullContent += token
          yield { type: "token", content: token }
        }

        // 流式已拿到 tool_calls 则直接用，否则回退非流式
        const streamedToolCalls = (provider as any)._lastStreamToolCalls
        if (streamedToolCalls) {
          response = { role: "assistant", content: fullContent, tool_calls: streamedToolCalls }
        } else {
          response = await provider.chat(context, this.config.model, toolDefs)
        }
      } catch (err: any) {
        yield { type: "error", error: err.message }
        return
      }

      // 4. 检查是否有 tool calls
      if (response.tool_calls && response.tool_calls.length > 0) {
        // 把 assistant 响应（含 tool_calls）加入上下文
        context.push(response)
        this.history.push(response)

        // 执行所有 tool calls
        for (const tc of response.tool_calls) {
          yield { type: 'tool_call', name: tc.function.name, args: JSON.parse(tc.function.arguments) }

          const result = await this.tools.execute(
            tc.function.name,
            JSON.parse(tc.function.arguments),
          )

          yield { type: 'tool_result', name: tc.function.name, content: result }

          // 把 tool result 加入上下文
          const toolMsg: Message = {
            role: 'tool',
            tool_call_id: tc.id,
            name: tc.function.name,
            content: result,
            timestamp: Date.now(),
          }
          context.push(toolMsg)
          this.history.push(toolMsg)
        }
        // 继续循环，让模型处理 tool results
        continue
      }

      // 5. 纯文本回复，结束
      this.history.push(response)
      yield { type: 'done', message: response }

      // 6. 异步保存记忆
      this.saveMemory(userContent, response.content ?? '').then(count => {
        if (count > 0) {
          // 记忆保存完成
        }
      })

      return
    }

    yield { type: 'error', error: `达到最大轮次 ${this.config.maxRounds}` }
  }

  abort() { this.abortController?.abort() }

  /**
   * 构建上下文：系统提示 + 相关记忆 + 历史
   */
  private async buildContext(userContent: string): Promise<Message[]> {
    const context: Message[] = []

    // 系统提示
    const systemPrompt = this.buildSystemPrompt()
    context.push({ role: 'system', content: systemPrompt, timestamp: Date.now() })

    // 相关记忆
    if (this.config.memoryEnabled) {
      const memories = await this.memory.search(userContent, 5)
      if (memories.length > 0) {
        const memText = memories.map(m => `- ${m.content}`).join('\n')
        context.push({
          role: 'system',
          content: `[记忆] 以下是与当前对话相关的记忆：\n${memText}`,
          timestamp: Date.now(),
        })
      }
    }

    // 历史消息（截断到 token 上限）
    const history = this.truncateHistory(this.config.maxHistoryTokens)
    context.push(...history)

    // 当前用户消息
    context.push({ role: 'user', content: userContent, timestamp: Date.now() })

    return context
  }

  private buildSystemPrompt(): string {
    const parts: string[] = []

    // 人设
    parts.push(this.config.persona.systemPrompt)

    // 时间
    parts.push(`当前时间: ${new Date().toLocaleString('zh-CN')}`)

    // 能力说明
    if (this.config.searchEnabled) {
      parts.push('你可以使用搜索工具查找实时信息。')
    }

    return parts.join('\n\n')
  }

  private truncateHistory(maxTokens: number): Message[] {
    // 粗略估算：1 token ≈ 2 字符
    const maxChars = maxTokens * 2
    let total = 0
    const result: Message[] = []
    for (let i = this.history.length - 1; i >= 0; i--) {
      const msg = this.history[i]
      const chars = (msg.content?.length ?? 0) + (JSON.stringify(msg.tool_calls)?.length ?? 0)
      if (total + chars > maxChars) break
      total += chars
      result.unshift(msg)
    }
    return result
  }

  private async saveMemory(userMsg: string, botMsg: string): Promise<number> {
    // 简单策略：每 5 轮对话提取一次记忆
    // Every turn saves memory
    try {
      return await this.memory.extractAndSave(this.history.slice(-10))
    } catch { return 0 }
  }
}
