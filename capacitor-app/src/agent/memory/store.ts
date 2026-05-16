// 长期记忆系统 — 基于 localStorage（轻量，无需 IndexedDB）
import type { MemoryEntry, MemorySearchResult } from '../types'

const STORAGE_KEY = 'gusogst_memory'
const MAX_ENTRIES = 500

class MemoryStore {
  private entries: MemoryEntry[] = []
  private loaded = false

  // ========== 加载 ==========
  private load() {
    if (this.loaded) return
    try {
      const raw = localStorage.getItem(STORAGE_KEY)
      if (raw) this.entries = JSON.parse(raw)
    } catch {
      this.entries = []
    }
    this.loaded = true
  }

  private save() {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(this.entries))
    } catch {
      // storage full — evict oldest low-importance entries
      this.evict()
      try {
        localStorage.setItem(STORAGE_KEY, JSON.stringify(this.entries))
      } catch { /* give up */ }
    }
  }

  private evict() {
    // 保留高重要性的，删掉最老的低重要性条目
    const sorted = [...this.entries].sort((a, b) => {
      if (a.importance !== b.importance) return b.importance - a.importance
      return a.timestamp - b.timestamp
    })
    this.entries = sorted.slice(0, Math.floor(MAX_ENTRIES * 0.7))
  }

  // ========== 写入 ==========
  add(type: MemoryEntry['type'], content: string, importance = 0.5, tags: string[] = []): MemoryEntry {
    this.load()
    // 去重：如果已有相同内容，更新时间戳而非新增
    const existing = this.entries.find(e => e.content === content && e.type === type)
    if (existing) {
      existing.timestamp = Date.now()
      existing.importance = Math.max(existing.importance, importance)
      this.save()
      return existing
    }

    const entry: MemoryEntry = {
      id: `mem_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`,
      type,
      content,
      timestamp: Date.now(),
      importance,
      tags,
    }
    this.entries.push(entry)

    // 超限清理
    if (this.entries.length > MAX_ENTRIES) this.evict()
    this.save()
    return entry
  }

  // ========== 搜索（简单关键词匹配 + 类型过滤）==========
  search(query: string, limit = 5): MemorySearchResult[] {
    this.load()
    const queryLower = query.toLowerCase()
    const queryWords = queryLower.split(/\s+/).filter(w => w.length > 1)

    const scored = this.entries.map(entry => {
      const contentLower = entry.content.toLowerCase()
      let score = 0

      // 完全包含查询 → 高分
      if (contentLower.includes(queryLower)) score += 10

      // 词级匹配
      for (const word of queryWords) {
        if (contentLower.includes(word)) score += 2
      }

      // 标签匹配
      for (const tag of entry.tags) {
        if (queryWords.some(w => tag.toLowerCase().includes(w))) score += 3
      }

      // 时间衰减（7天半衰期）
      const ageDays = (Date.now() - entry.timestamp) / (1000 * 60 * 60 * 24)
      const timeDecay = Math.pow(0.5, ageDays / 7)

      // 最终分 = 匹配度 × 重要性 × 时间衰减
      const finalScore = score * entry.importance * (0.3 + 0.7 * timeDecay)
      return { entry, score: finalScore }
    })

    return scored
      .filter(s => s.score > 0)
      .sort((a, b) => b.score - a.score)
      .slice(0, limit)
  }

  // ========== 按类型获取 ==========
  getByType(type: MemoryEntry['type'], limit = 20): MemoryEntry[] {
    this.load()
    return this.entries
      .filter(e => e.type === type)
      .sort((a, b) => b.timestamp - a.timestamp)
      .slice(0, limit)
  }

  // ========== 获取全部（用于注入 prompt）==========
  getImportant(limit = 10): MemoryEntry[] {
    this.load()
    return [...this.entries]
      .sort((a, b) => b.importance - a.importance || b.timestamp - a.timestamp)
      .slice(0, limit)
  }

  // ========== 删除 ==========
  delete(id: string) {
    this.load()
    this.entries = this.entries.filter(e => e.id !== id)
    this.save()
  }

  clear() {
    this.entries = []
    this.save()
  }

  count(): number {
    this.load()
    return this.entries.length
  }
}

export const memoryStore = new MemoryStore()