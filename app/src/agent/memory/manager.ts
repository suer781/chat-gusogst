export interface MemoryEntry {
  id: string
  content: string
  type: 'fact' | 'preference' | 'event' | 'emotion' | 'context'
  importance: number
  tags: string[]
  timestamp: number
  accessCount: number
  lastAccessed: number
}

const DB_NAME = 'chat-gusogst-memory'
const DB_VERSION = 1
const MAX_MEMORIES = 500  // max memory entries to prevent unbounded growth
const STORE_NAME = 'memories'

function openDB(): Promise<IDBDatabase> {
  return new Promise((resolve, reject) => {
    const req = indexedDB.open(DB_NAME, DB_VERSION)
    req.onupgradeneeded = () => {
      const db = req.result
      if (!db.objectStoreNames.contains(STORE_NAME)) {
        const store = db.createObjectStore(STORE_NAME, { keyPath: 'id' })
        store.createIndex('type', 'type', { unique: false })
        store.createIndex('timestamp', 'timestamp', { unique: false })
        store.createIndex('importance', 'importance', { unique: false })
      }
    }
    req.onsuccess = () => resolve(req.result)
    req.onerror = () => reject(req.error)
  })
}

async function dbGetAll(): Promise<MemoryEntry[]> {
  const db = await openDB()
  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORE_NAME, 'readonly')
    const req = tx.objectStore(STORE_NAME).getAll();
    req.onsuccess = () => resolve(req.result)
    req.onerror = () => reject(req.error)
  })
}

async function dbPut(entry: MemoryEntry): Promise<void> {
  const db = await openDB()
  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORE_NAME, 'readwrite')
    tx.objectStore(STORE_NAME).put(entry)
    tx.oncomplete = () => resolve()
    tx.onerror = () => reject(tx.error)
  })
}

async function dbDelete(id: string): Promise<void> {
  const db = await openDB()
  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORE_NAME, 'readwrite')
    tx.objectStore(STORE_NAME).delete(id)
    tx.oncomplete = () => resolve()
    tx.onerror = () => reject(tx.error)
  })
}

async function dbClear(): Promise<void> {
  const db = await openDB()
  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORE_NAME, 'readwrite')
    tx.objectStore(STORE_NAME).clear()
    tx.oncomplete = () => resolve()
    tx.onerror = () => reject(tx.error)
  })
}

export class MemoryManager {
  private cache: MemoryEntry[] = []
  private loaded = false

  async ensureLoaded() {
    if (!this.loaded) {
      try { this.cache = await dbGetAll() }
      catch { this.cache = [] }
      this.loaded = true
    }
  }

  async search(query: string, limit = 5): Promise<MemoryEntry[]> {
    await this.ensureLoaded()
    const queryLower = query.toLowerCase()
    const queryWords = queryLower.split(/\s+/).filter(w => w.length > 1)

    const scored = this.cache.map(m => {
      let score = 0
      const contentLower = m.content.toLowerCase()
      // keyword match
      for (const w of queryWords) {
        if (contentLower.includes(w)) score += 10
      }
      // tag match
      for (const t of m.tags) {
        if (queryWords.some(w => t.toLowerCase().includes(w))) score += 5
      }
      // importance boost
      score *= (0.5 + m.importance * 0.5)
      // time decay (24h half-life)
      const hoursAgo = (Date.now() - m.timestamp) / 3600000
      score *= Math.pow(0.5, hoursAgo / 24)
      // access frequency
      score *= (1 + Math.min(m.accessCount, 10) * 0.05)
      return { entry: m, score }
    })

    scored.sort((a, b) => b.score - a.score)
    const results = scored.filter(s => s.score > 0).slice(0, limit).map(s => s.entry)

    // update access count
    for (const r of results) {
      r.accessCount++
      r.lastAccessed = Date.now()
      await dbPut(r)
    }
    return results
  }

  async add(content: string, type: MemoryEntry['type'] = 'fact', importance = 0.5, tags: string[] = []): Promise<MemoryEntry> {
    await this.ensureLoaded()
    const entry: MemoryEntry = {
      id: 'mem_' + Date.now() + '_' + Math.random().toString(36).slice(2, 6),
      content,
      type,
      importance: Math.max(0, Math.min(1, importance)),
      tags,
      timestamp: Date.now(),
      accessCount: 0,
      lastAccessed: Date.now(),
    }
    this.cache.push(entry)
    await dbPut(entry)
    return entry
  }

  async extractAndSave(messages: { role: string; content: string }[]): Promise<void> {
    const recent = messages.slice(-10)
    for (const msg of recent) {
      if (msg.role !== 'user') continue
      const c = msg.content
      // preference patterns
      const prefMatch = c.match(/(?:我喜欢|我想要|我偏好|我不喜欢|我讨厌|my preference|i like|i prefer|i hate)(.{3,50})/i)
      if (prefMatch) {
        await this.add(c, 'preference', 0.8, ['preference'])
        continue
      }
      // fact patterns
      const factMatch = c.match(/(?:我叫|我是|我的名字|我住在|我在|i am|i live|my name)(.{3,50})/i)
      if (factMatch) {
        await this.add(c, 'fact', 0.7, ['personal'])
        continue
      }
      // emotion patterns
      const emoMatch = c.match(/(?:我很开心|我很难过|我很焦虑|我压力大|i feel|i am happy|i am sad)(.{3,50})/i)
      if (emoMatch) {
        await this.add(c, 'emotion', 0.6, ['emotion'])
      }
    }
  }

  async getContextStrings(query: string, limit = 3): Promise<string[]> {
    const results = await this.search(query, limit)
    return results.map(r => r.content)
  }

  async getAll(): Promise<MemoryEntry[]> {
    await this.ensureLoaded()
    return [...this.cache]
  }

  async clear(): Promise<void> {
    this.cache = []
    await dbClear()
  }

  getMemoryCount(): number {
    return this.cache.length
  }
}
