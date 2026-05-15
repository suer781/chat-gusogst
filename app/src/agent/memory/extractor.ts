// ============================================
// 记忆提取器 — 从对话中提取值得记住的事实
// 零 token 设计：后台异步运行，不阻塞对话
// 两层提取：正则快速匹配 + LLM 深度分析
// ============================================

import type { MemoryEntry, MemoryCategory } from '../../shared/types/memory'
import type { ProviderAdapter, ModelConfig, Message } from '../../shared/types'

// ── 正则快速提取（零 token）──────────────────────

interface QuickExtract {
  content: string
  category: MemoryCategory
  confidence: number
}

const QUICK_PATTERNS: { re: RegExp; category: MemoryCategory; extract: (m: RegExpMatchArray) => string }[] = [
  // 偏好
  { re: /我(?:喜欢|爱|偏好|习惯|经常)吃(.+?)[。，！]/, category: 'preference', extract: (m) => `喜欢吃${m[1].trim()}` },
  { re: /我(?:不喜欢|讨厌|不爱|不吃|不能吃)(.+?)[。，！]/, category: 'preference', extract: (m) => `不喜欢${m[1].trim()}` },
  { re: /我(?:喜欢|爱|偏好)(.+?)[。，！]/, category: 'preference', extract: (m) => `喜欢${m[1].trim()}` },
  { re: /我(?:讨厌|不喜欢|不爱)(.+?)[。，！]/, category: 'preference', extract: (m) => `不喜欢${m[1].trim()}` },
  
  // 事实
  { re: /我(?:住在|在|去了|来自)(.+?)[。，！]/, category: 'fact', extract: (m) => `地点：${m[1].trim()}` },
  { re: /我(?:是|做|从事|在)(.+?)(?:工作|职业|行业)/, category: 'fact', extract: (m) => `职业：${m[1].trim()}` },
  { re: /我(?:今年|现在)(.+?)(?:岁|年级)/, category: 'fact', extract: (m) => `年龄：${m[1].trim()}` },
  { re: /我(?:叫|是|名字叫)(.+?)[。，！]/, category: 'fact', extract: (m) => `名字：${m[1].trim()}` },
  
  // 情感
  { re: /我(?:今天|最近|现在)(?:心情|感觉)(.+?)[。，！]/, category: 'emotion', extract: (m) => `情绪：${m[1].trim()}` },
  { re: /我(?:开心|高兴|难过|伤心|焦虑|压力大|累|烦)/, category: 'emotion', extract: (m) => m[0] },
  
  // 习惯
  { re: /我(?:每天|通常|一般)(.+?)(?:点|时|分)(?:起床|睡|出门)/, category: 'habit', extract: (m) => `作息：${m[0].trim()}` },
  { re: /我(?:每天都|经常|习惯)(.+?)[。，！]/, category: 'habit', extract: (m) => `习惯：${m[0].trim()}` },
  
  // 人际关系
  { re: /我(?:的|有个)(.+?)(?:叫|是|名字)(.+?)[。，！]/, category: 'relationship', extract: (m) => `${m[1].trim()}：${m[2].trim()}` },
  { re: /我(?:老婆|老公|女朋友|男朋友|爸妈|爸|妈|哥|姐|弟|妹)(.+?)[。，！]/, category: 'relationship', extract: (m) => `${m[0].trim()}` },
]

export function quickExtract(userMessage: string): QuickExtract[] {
  const results: QuickExtract[] = []
  for (const { re, category, extract } of QUICK_PATTERNS) {
    const match = userMessage.match(re)
    if (match) {
      results.push({
        content: extract(match),
        category,
        confidence: 0.7,
      })
    }
  }
  return results
}

// ── LLM 深度提取 ──────────────────────

const EXTRACTION_PROMPT = `你是一个记忆提取系统。分析以下对话，提取值得长期记住的事实。

只提取以下类型的信息：
- 用户的个人信息（名字、职业、所在地、年龄）
- 用户的偏好（喜欢/不喜欢什么）
- 用户的习惯（作息、通勤、饮食）
- 用户的情感状态（最近的心情、压力）
- 用户的人际关系（家人、朋友、同事）
- 用户正在进行的项目或计划
- 用户明确表达的观点或态度

不要提取：
- 临时性的请求（帮我查一下XX）
- 闲聊中的废话（哈哈、嗯嗯、好的）
- 已经是常识的信息
- 模糊不清的表述

用 JSON 数组格式输出：[{"content":"事实描述","category":"分类","confidence":0.0-1.0}]
如果没有值得记住的内容，输出空数组 []

分类选项：fact, preference, emotion, habit, relationship, project, opinion, context`

export async function deepExtract(
  userMessage: string,
  assistantResponse: string,
  provider: ProviderAdapter,
  modelConfig: ModelConfig
): Promise<QuickExtract[]> {
  const messages: Message[] = [
    { role: 'system', content: EXTRACTION_PROMPT },
    { role: 'user', content: `用户：${userMessage}\n助手：${assistantResponse}` },
  ]
  try {
    const response = await provider.chat(messages, {
      ...modelConfig,
      temperature: 0.1,
      maxTokens: 500,
    })
    const text = response.content || '[]'
    const jsonMatch = text.match(/\[\s\{[\s\S]*\}\s*\]/)
    if (!jsonMatch) return []
    const items = JSON.parse(jsonMatch[0])
    return items.map((item: any) => ({
      content: item.content,
      category: item.category || 'fact',
      confidence: Math.min(1, Math.max(0, item.confidence || 0.5)),
    }))
  } catch {
    return []
  }
}

// ── 上下文压缩（省 token 的核心）──────────────────────

const COMPRESS_PROMPT = `将以下对话历史压缩成简洁的摘要，保留关键信息和上下文。

规则：
1. 保留所有事实性信息（名字、数字、结论）
2. 保留用户的情感状态变化
3. 保留待办事项和未完成的话题
4. 压缩闲聊和重复内容
5. 用第三人称描述（用户说...，助手建议...）
6. 控制在原文的 30% 以内

输出纯文本摘要，不要加标题或格式。`

export async function compressConversation(
  messages: { role: string; content: string }[],
  provider: ProviderAdapter,
  modelConfig: ModelConfig
): Promise<string> {
  const conversationText = messages
    .map(m => `${m.role === 'user' ? '用户' : '助手'}：${m.content}`)
    .join('\n')
  const chatMessages: Message[] = [
    { role: 'system', content: COMPRESS_PROMPT },
    { role: 'user', content: conversationText },
  ]
  try {
    const response = await provider.chat(chatMessages, {
      ...modelConfig,
      temperature: 0.2,
      maxTokens: Math.max(200, Math.floor(conversationText.length / 4)),
    })
    return response.content || conversationText.slice(0, 500)
  } catch {
    return conversationText.slice(0, 500)
  }
}

// ── 信任评分调整 ──────────────────────

export function adjustTrust(
  entry: MemoryEntry,
  type: 'retrieved' | 'helpful' | 'unhelpful',
  config: { helpfulDelta: number; unhelpfulDelta: number }
): MemoryEntry {
  const updated = { ...entry, lastAccessed: Date.now() }
  updated.retrievalCount++
  if (type === 'helpful') {
    updated.helpfulCount++
    updated.trustScore = Math.min(1, updated.trustScore + config.helpfulDelta)
  } else if (type === 'unhelpful') {
    updated.trustScore = Math.max(0, updated.trustScore + config.unhelpfulDelta)
  }
  return updated
}
