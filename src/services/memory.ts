import AsyncStorage from '@react-native-async-storage/async-storage';
import { MemoryEntry } from '../types';

const MEMORY_KEY = 'cgs_memories';
const SUMMARY_KEY = 'cgs_summaries';

/** 记忆服务 — Hermes-style: 跨会话记忆、关键词检索、重要性评分 */
export class MemoryService {
  private memories: MemoryEntry[] = [];
  private summaries: Record<string, string> = {};

  async init() {
    try {
      const [m, s] = await Promise.all([
        AsyncStorage.getItem(MEMORY_KEY),
        AsyncStorage.getItem(SUMMARY_KEY),
      ]);
      if (m) this.memories = JSON.parse(m);
      if (s) this.summaries = JSON.parse(s);
    } catch {}
  }

  private async persist() {
    await Promise.all([
      AsyncStorage.setItem(MEMORY_KEY, JSON.stringify(this.memories)),
      AsyncStorage.setItem(SUMMARY_KEY, JSON.stringify(this.summaries)),
    ]);
  }

  async addMemory(entry: MemoryEntry) {
    this.memories.push(entry);
    // Hermes-style: 保留最重要的记忆
    if (this.memories.length > 500) {
      this.memories.sort((a, b) => b.importance - a.importance);
      this.memories = this.memories.slice(0, 400);
    }
    await this.persist();
  }

  searchMemories(query: string, limit = 5): MemoryEntry[] {
    const terms = query.toLowerCase().split(/\s+/).filter(Boolean);
    if (terms.length === 0) return this.memories.slice(0, limit);
    return this.memories
      .map(m => {
        const text = (m.summary + ' ' + m.keywords.join(' ')).toLowerCase();
        let score = terms.reduce((s, t) => s + (text.includes(t) ? 1 : 0), 0);
        score += m.importance * 0.5;
        return { entry: m, score };
      })
      .filter(s => s.score > 0)
      .sort((a, b) => b.score - a.score)
      .slice(0, limit)
      .map(s => s.entry);
  }

  async setSummary(convId: string, summary: string) {
    this.summaries[convId] = summary;
    await this.persist();
  }

  getSummary(convId: string): string | undefined {
    return this.summaries[convId];
  }

  async clearAll() {
    this.memories = [];
    this.summaries = {};
    await this.persist();
  }
}

export const memoryService = new MemoryService();
