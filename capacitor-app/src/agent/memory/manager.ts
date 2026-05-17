/**
 * Memory Manager — 基于 Hermes memory_manager.py 重写
 * 简单的文件存储记忆系统，带重要性衰减和搜索
 */
import type { MemoryEntry, Message } from '../../shared/types'

export class MemoryManager {
  private memories: MemoryEntry[] = []
  private storageKey = 'chat-gusogst-memory'

  constructor() { this.load() }

  private load() {
    try {
      const raw = localStorage.getItem(this.storageKey)
      if (raw) this.memories = JSON.parse(raw)
    } catch { this.memories = [] }
  }

  private save() {
    localStorage.setItem(this.storageKey, JSON.stringify(this.memories))
  }

  /** 搜索相关记忆 */
  async search(query: string, limit = 5): Promise<MemoryEntry[]> {
    const queryLower = query.toLowerCase()
    const scored = this.memories
      .map(m => {
        let score = 0
        // 关键词匹配
        const words = queryLower.split(/\s+/)
        for (const w of words) {
          if (m.content.toLowerCase().includes(w)) score += 0.3
        }
        // 标签匹配
        for (const tag of m.tags) {
          if (queryLower.includes(tag.toLowerCase())) score += 0.2
        }
        // 重要性加权
        score *= m.importance
        // 时间衰减（24小时半衰期）
        const age = Date.now() - new Date(m.created_at).getTime()
        score *= Math.pow(0.5, age / (24 * 3600 * 1000))
        // 访问频次加权
        score *= 1 + Math.log(1 + m.access_count)
        return { entry: m, score }
      })
      .filter(x => x.score > 0.01)
      .sort((a, b) => b.score - a.score)
      .slice(0, limit)

    // 更新访问计数
    for (const { entry } of scored) {
      entry.access_count++
      entry.last_accessed = new Date().toISOString()
    }
    this.save()

    return scored.map(x => x.entry)
  }

  /** 添加记忆 */
  async add(content: string, type: MemoryEntry['type'], importance = 0.5, tags: string[] = []): Promise<MemoryEntry> {
    const entry: MemoryEntry = {
      id: `mem_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`,
      content, type, importance, tags,
      created_at: new Date().toISOString(),
      last_accessed: new Date().toISOString(),
      access_count: 0,
    }
    this.memories.push(entry)
    this.save()
    return entry
  }

  /** 从对话历史中提取记忆（简化版，实际应调模型提取） */
  async extractAndSave(messages: Message[]): Promise<number> {
    // 简单策略：记录最近的用户偏好和事实
    let saved = 0
    for (const msg of messages) {
      if (msg.role !== 'user' || !msg.content) continue
      // 检测偏好表达
      const prefPatterns = [
        /我喜欢(.+)/, /我讨厌(.+)/, /我不喜欢(.+)/,
        /我想要(.+)/, /我需要(.+)/, /帮我记住(.+)/,
      ]
      for (const pat of prefPatterns) {
        const match = msg.content.match(pat)
        if (match) {
          await this.add(match[0], 'preference', 0.8, ['偏好'])
          saved++
        }
      }
    }
    return saved
  }

  /** 清除所有记忆 */
  clear() {
    this.memories = []
    this.save()
  }

  /** 获取所有记忆 */
  getAll(): MemoryEntry[] { return [...this.memories] }
}
