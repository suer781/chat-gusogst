// ============================================
// 记忆持久化存储 — IndexedDB 实现
// 替代 Hermes 的文件系统 + SQLite
// ============================================

import type { MemoryEntry, ChatRecord, SessionSummary } from '../../shared/types/memory'

const DB_NAME = 'chat-gusogst-memory'
const DB_VERSION = 1

const STORES = {
  memories: 'memories',     // 记忆条目
  records: 'chat_records',  // 对话历史（零 token 记录）
  summaries: 'summaries',   // 压缩摘要
} as const

function openDB(): Promise<IDBDatabase> {
  return new Promise((resolve, reject) => {
    const req = indexedDB.open(DB_NAME, DB_VERSION)
    req.onupgradeneeded = () => {
      const db = req.result
      if (!db.objectStoreNames.contains(STORES.memories)) {
        const mem = db.createObjectStore(STORES.memories, { keyPath: 'id' })
        mem.createIndex('bank', 'bank', { unique: false })
        mem.createIndex('category', 'category', { unique: false })
        mem.createIndex('personaId', 'personaId', { unique: false })
        mem.createIndex('trustScore', 'trustScore', { unique: false })
      }
      if (!db.objectStoreNames.contains(STORES.records)) {
        const rec = db.createObjectStore(STORES.records, { keyPath: 'id' })
        rec.createIndex('sessionId', 'sessionId', { unique: false })
        rec.createIndex('personaId', 'personaId', { unique: false })
        rec.createIndex('timestamp', 'timestamp', { unique: false })
      }
      if (!db.objectStoreNames.contains(STORES.summaries)) {
        const sum = db.createObjectStore(STORES.summaries, { keyPath: 'id' })
        sum.createIndex('sessionId', 'sessionId', { unique: false })
        sum.createIndex('personaId', 'personaId', { unique: false })
      }
    }
    req.onsuccess = () => resolve(req.result)
    req.onerror = () => reject(req.error)
  })
}

async function txRun<T>(
  storeName: string,
  mode: IDBTransactionMode,
  fn: (store: IDBObjectStore) => IDBRequest<T>
): Promise<T> {
  const db = await openDB()
  return new Promise((resolve, reject) => {
    const tx = db.transaction(storeName, mode)
    const store = tx.objectStore(storeName)
    const req = fn(store)
    req.onsuccess = () => resolve(req.result)
    req.onerror = () => reject(req.error)
  })
}

async function txRunAll<T>(
  storeName: string,
  mode: IDBTransactionMode,
  fn: (store: IDBObjectStore) => IDBRequest<T>[]
): Promise<T[]> {
  const db = await openDB()
  return new Promise((resolve, reject) => {
    const tx = db.transaction(storeName, mode)
    const store = tx.objectStore(storeName)
    const reqs = fn(store)
    const results: T[] = []
    let done = 0
    for (const req of reqs) {
      req.onsuccess = () => { results.push(req.result); if (++done === reqs.length) resolve(results) }
      req.onerror = () => reject(req.error)
    }
  })
}

// ── 记忆 CRUD ──────────────────────

export const MemoryStore = {
  // 添加记忆
  async addMemory(entry: MemoryEntry): Promise<void> {
    await txRun(STORES.memories, 'readwrite', s => s.put(entry))
  },

  // 获取单条记忆
  async getMemory(id: string): Promise<MemoryEntry | undefined> {
    return txRun(STORES.memories, 'readonly', s => s.get(id))
  },

  // 获取指定 bank + persona 的所有记忆
  async getMemories(bank?: 'agent' | 'user', personaId?: string): Promise<MemoryEntry[]> {
    const db = await openDB()
    return new Promise((resolve, reject) => {
      const tx = db.transaction(STORES.memories, 'readonly')
      const store = tx.objectStore(STORES.memories)
      const results: MemoryEntry[] = []
      const req = store.openCursor()
      req.onsuccess = () => {
        const cursor = req.result
        if (!cursor) { resolve(results); return }
        const entry = cursor.value as MemoryEntry
        if (bank && entry.bank !== bank) { cursor.continue(); return }
        if (personaId && entry.personaId && entry.personaId !== personaId) { cursor.continue(); return }
        results.push(entry)
        cursor.continue()
      }
      req.onerror = () => reject(req.error)
    })
  },

  // 更新记忆
  async updateMemory(entry: MemoryEntry): Promise<void> {
    await txRun(STORES.memories, 'readwrite', s => s.put(entry))
  },

  // 删除记忆
  async deleteMemory(id: string): Promise<void> {
    await txRun(STORES.memories, 'readwrite', s => s.delete(id))
  },

  // ── 对话历史记录（零 token 记录）──────────────────────

  async addRecord(record: ChatRecord): Promise<void> {
    await txRun(STORES.records, 'readwrite', s => s.put(record))
  },

  async getRecords(sessionId: string): Promise<ChatRecord[]> {
    const db = await openDB()
    return new Promise((resolve, reject) => {
      const tx = db.transaction(STORES.records, 'readonly')
      const idx = tx.objectStore(STORES.records).index('sessionId')
      const req = idx.getAll(sessionId)
      req.onsuccess = () => resolve(req.result || [])
      req.onerror = () => reject(req.error)
    })
  },

  async getRecordsByPersona(personaId: string, limit = 100): Promise<ChatRecord[]> {
    const db = await openDB()
    return new Promise((resolve, reject) => {
      const tx = db.transaction(STORES.records, 'readonly')
      const idx = tx.objectStore(STORES.records).index('personaId')
      const req = idx.getAll(personaId)
      req.onsuccess = () => {
        const all = req.result || []
        all.sort((a, b) => b.timestamp - a.timestamp)
        resolve(all.slice(0, limit))
      }
      req.onerror = () => reject(req.error)
    })
  },

  // ── 摘要 ──────────────────────

  async addSummary(summary: SessionSummary): Promise<void> {
    await txRun(STORES.summaries, 'readwrite', s => s.put(summary))
  },

  async getSummaries(sessionId: string): Promise<SessionSummary[]> {
    const db = await openDB()
    return new Promise((resolve, reject) => {
      const tx = db.transaction(STORES.summaries, 'readonly')
      const idx = tx.objectStore(STORES.summaries).index('sessionId')
      const req = idx.getAll(sessionId)
      req.onsuccess = () => resolve(req.result || [])
      req.onerror = () => reject(req.error)
    })
  },

  // ── 统计 ──────────────────────

  async getStats(): Promise<{ memories: number; records: number; summaries: number }> {
    const db = await openDB()
    const count = (name: string) => new Promise<number>((resolve, reject) => {
      const tx = db.transaction(name, 'readonly')
      const req = tx.objectStore(name).count()
      req.onsuccess = () => resolve(req.result)
      req.onerror = () => reject(req.error)
    })
    const [memories, records, summaries] = await Promise.all([
      count(STORES.memories), count(STORES.records), count(STORES.summaries),
    ])
    return { memories, records, summaries }
  },

  // ── 清理 ──────────────────────

  async clearAll(): Promise<void> {
    const db = await openDB()
    const tx = db.transaction([STORES.memories, STORES.records, STORES.summaries], 'readwrite')
    tx.objectStore(STORES.memories).clear()
    tx.objectStore(STORES.records).clear()
    tx.objectStore(STORES.summaries).clear()
    return new Promise((resolve, reject) => {
      tx.oncomplete = () => resolve()
      tx.onerror = () => reject(tx.error)
    })
  },

  // 清理压缩掉的旧记录
  async cleanCompressedRecords(): Promise<number> {
    const db = await openDB()
    return new Promise((resolve, reject) => {
      const tx = db.transaction(STORES.records, 'readwrite')
      const store = tx.objectStore(STORES.records)
      const req = store.openCursor()
      let count = 0
      req.onsuccess = () => {
        const cursor = req.result
        if (!cursor) { resolve(count); return }
        if (cursor.value.compressed) {
          cursor.delete()
          count++
        }
        cursor.continue()
      }
      req.onerror = () => reject(req.error)
    })
  },
}
