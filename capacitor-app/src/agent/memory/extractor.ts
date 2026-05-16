// 记忆提取器 — 从对话中自动提取值得记住的信息
import type { ModelConfig, Persona, Message } from '../lib/types'
import { memoryStore } from './store'

/**
 * 分析用户消息，提取值得记住的事实
 * 使用轻量 prompt 调用模型提取，不增加太多延迟
 */
export async function extractMemories(
  model: ModelConfig,
  persona: Persona,
  userMessage: string,
  assistantResponse: string,
): Promise<void> {
  // 跳过太短的消息
  if (userMessage.length < 10 && assistantResponse.length < 20) return

  try {
    const extractPrompt = `你是记忆提取器。分析这段对话，提取值得长期记住的用户信息。

规则：
- 只提取用户明确表达的事实、偏好、习惯、关系
- 不提取通用知识或对话流程信息
- 如果没有值得记住的内容，返回空数组 []
- 返回 JSON 数组，每项 {"type": "fact|preference|event|relationship", "content": "...", "importance": 0.0-1.0, "tags": ["..."]}

用户：${userMessage.slice(0, 500)}
AI：${assistantResponse.slice(0, 500)}

JSON：`

    const response = await fetch(`${model.apiHost}/v1/chat/completions`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${model.apiKey}`,
      },
      body: JSON.stringify({
        model: model.model,
        messages: [{ role: 'user', content: extractPrompt }],
        temperature: 0,
        max_tokens: 500,
      }),
    })

    if (!response.ok) return

    const data = await response.json()
    const text = data.choices?.[0]?.message?.content || ''

    // 提取 JSON 数组
    const jsonMatch = text.match(/\[[\s\S]*\]/)
    if (!jsonMatch) return

    const items = JSON.parse(jsonMatch[0])
    if (!Array.isArray(items)) return

    for (const item of items) {
      if (item.content && item.type) {
        memoryStore.add(
          item.type,
          item.content,
          Math.min(1, Math.max(0, item.importance || 0.5)),
          item.tags || [],
        )
      }
    }
  } catch {
    // 提取失败不影响主流程，静默处理
  }
}

/**
 * 构建记忆上下文，注入到系统提示词中
 */
export function buildMemoryContext(query: string): string {
  const relevant = memoryStore.search(query, 5)
  if (relevant.length === 0) return ''

  const lines = relevant.map(r => `- ${r.entry.content}`)
  return `\n\n## 关于用户的记忆\n${lines.join('\n')}`
}

/**
 * 获取用户档案摘要（用于 system prompt 注入）
 */
export function getMemorySummary(): string {
  const facts = memoryStore.getByType('fact', 10)
  const prefs = memoryStore.getByType('preference', 10)
  const relations = memoryStore.getByType('relationship', 5)

  const parts: string[] = []
  if (facts.length) parts.push('事实：' + facts.map(f => f.content).join('；'))
  if (prefs.length) parts.push('偏好：' + prefs.map(f => f.content).join('；'))
  if (relations.length) parts.push('关系：' + relations.map(f => f.content).join('；'))

  return parts.length ? `\n\n## 用户档案\n${parts.join('\n')}` : ''
}