/**
 * Vector Store — TF-IDF + Cosine Similarity RAG
 * 纯本地实现，无需 API 调用，IndexedDB 持久化
 */

const VECTOR_DB_NAME = 'chat-gusogst-vectors'
const VECTOR_DB_VERSION = 1
const VECTOR_STORE = 'vectors'

// ===== IndexedDB helpers =====
function openVectorDB(): Promise<IDBDatabase> {
  return new Promise((resolve, reject) => {
    const req = indexedDB.open(VECTOR_DB_NAME, VECTOR_DB_VERSION)
    req.onupgradeneeded = () => {
      const db = req.result
      if (!db.objectStoreNames.contains(VECTOR_STORE)) {
        db.createObjectStore(VECTOR_STORE, { keyPath: 'id' })
      }
    }
    req.onsuccess = () => resolve(req.result)
    req.onerror = () => reject(req.error)
  })
}

async function vectorDBGetAll(): Promise<VectorEntry[]> {
  const db = await openVectorDB()
  return new Promise((resolve, reject) => {
    const tx = db.transaction(VECTOR_STORE, 'readonly')
    const store = tx.objectStore(VECTOR_STORE)
    const req = store.getAll()
    req.onsuccess = () => resolve(req.result)
    req.onerror = () => reject(req.error)
  })
}

async function vectorDBPut(entry: VectorEntry): Promise<void> {
  const db = await openVectorDB()
  return new Promise((resolve, reject) => {
    const tx = db.transaction(VECTOR_STORE, 'readwrite')
    const store = tx.objectStore(VECTOR_STORE)
    store.put(entry)
    tx.oncomplete = () => resolve()
    tx.onerror = () => reject(tx.error)
  })
}

async function vectorDBDelete(id: string): Promise<void> {
  const db = await openVectorDB()
  return new Promise((resolve, reject) => {
    const tx = db.transaction(VECTOR_STORE, 'readwrite')
    const store = tx.objectStore(VECTOR_STORE)
    store.delete(id)
    tx.oncomplete = () => resolve()
    tx.onerror = () => reject(tx.error)
  })
}

async function vectorDBClear(): Promise<void> {
  const db = await openVectorDB()
  return new Promise((resolve, reject) => {
    const tx = db.transaction(VECTOR_STORE, 'readwrite')
    const store = tx.objectStore(VECTOR_STORE)
    store.clear()
    tx.oncomplete = () => resolve()
    tx.onerror = () => reject(tx.error)
  })
}

// ===== Text processing =====

// 中文分词：按字/词切分（简单的 n-gram + 字符级）
function tokenize(text: string): string[] {
  const tokens: string[] = []
  // 提取中文字符（bigram）和英文单词
  const chineseChars: string[] = []
  const cleaned = text.toLowerCase().replace(/[^\u4e00-\u9fff\w]/g, ' ')

  // 英文单词
  const englishWords = cleaned.match(/[a-z][a-z0-9]*/g) || []
  tokens.push(...englishWords)

  // 中文字符 — 用 unigram + bigram
  const chinese = text.match(/[\u4e00-\u9fff]/g) || []
  for (const ch of chinese) {
    tokens.push(ch) // unigram
  }
  for (let i = 0; i < chinese.length - 1; i++) {
    tokens.push(chinese[i] + chinese[i + 1]) // bigram
  }

  return tokens
}

// ===== TF-IDF =====

interface VectorEntry {
  id: string
  tokens: string[]       // original tokens
  tf: Record<string, number>  // term frequency
  magnitude: number      // vector magnitude for cosine similarity
}

export class VectorStore {
  private entries: VectorEntry[] = []
  private idf: Record<string, number> = {}
  private loaded = false

  private async ensureLoaded(): Promise<void> {
    if (this.loaded) return
    this.entries = await vectorDBGetAll()
    this.rebuildIDF()
    this.loaded = true
  }

  // Build IDF from all entries
  private rebuildIDF(): void {
    const df: Record<string, number> = {}
    const N = this.entries.length
    for (const entry of this.entries) {
      const seen = new Set<string>()
      for (const token of entry.tokens) {
        if (!seen.has(token)) {
          df[token] = (df[token] || 0) + 1
          seen.add(token)
        }
      }
    }
    // IDF with smoothing: log((N + 1) / (df + 1)) + 1
    for (const [term, count] of Object.entries(df)) {
      this.idf[term] = Math.log((N + 1) / (count + 1)) + 1
    }
  }

  // Compute TF for a set of tokens
  private computeTF(tokens: string[]): Record<string, number> {
    const tf: Record<string, number> = {}
    for (const t of tokens) {
      tf[t] = (tf[t] || 0) + 1
    }
    // Normalize by total count
    const len = tokens.length || 1
    for (const t of Object.keys(tf)) {
      tf[t] /= len
    }
    return tf
  }

  // Compute TF-IDF vector magnitude
  private computeMagnitude(tf: Record<string, number>): number {
    let sum = 0
    for (const [term, tfVal] of Object.entries(tf)) {
      const idfVal = this.idf[term] || 1
      const w = tfVal * idfVal
      sum += w * w
    }
    return Math.sqrt(sum) || 1
  }

  // Add a document
  async add(id: string, text: string): Promise<void> {
    await this.ensureLoaded()
    const tokens = tokenize(text)
    if (tokens.length === 0) return

    const tf = this.computeTF(tokens)
    const entry: VectorEntry = { id, tokens, tf, magnitude: 1 }

    // Update or insert
    const idx = this.entries.findIndex(e => e.id === id)
    if (idx >= 0) {
      this.entries[idx] = entry
    } else {
      this.entries.push(entry)
    }

    // Rebuild IDF and recompute magnitudes
    this.rebuildIDF()
    for (const e of this.entries) {
      e.magnitude = this.computeMagnitude(e.tf)
    }

    await vectorDBPut(entry)
  }

  // Remove a document
  async remove(id: string): Promise<void> {
    await this.ensureLoaded()
    this.entries = this.entries.filter(e => e.id !== id)
    this.rebuildIDF()
    await vectorDBDelete(id)
  }

  // Cosine similarity between query and an entry
  private cosineSimilarity(queryTF: Record<string, number>, queryMag: number, entry: VectorEntry): number {
    let dotProduct = 0
    for (const [term, qTF] of Object.entries(queryTF)) {
      const eTF = entry.tf[term]
      if (eTF !== undefined) {
        const qWeight = qTF * (this.idf[term] || 1)
        const eWeight = eTF * (this.idf[term] || 1)
        dotProduct += qWeight * eWeight
      }
    }
    const entryMag = this.computeMagnitude(entry.tf)
    const denom = queryMag * entryMag
    return denom > 0 ? dotProduct / denom : 0
  }

  // Semantic search — returns [{id, score}] sorted by relevance
  async search(query: string, limit = 5, minScore = 0.05): Promise<{ id: string; score: number }[]> {
    await this.ensureLoaded()
    if (this.entries.length === 0) return []

    const queryTokens = tokenize(query)
    if (queryTokens.length === 0) return []

    const queryTF = this.computeTF(queryTokens)
    const queryMag = this.computeMagnitude(queryTF)

    const results: { id: string; score: number }[] = []
    for (const entry of this.entries) {
      const score = this.cosineSimilarity(queryTF, queryMag, entry)
      if (score >= minScore) {
        results.push({ id: entry.id, score })
      }
    }

    results.sort((a, b) => b.score - a.score)
    return results.slice(0, limit)
  }

  // Get stats
  async getStats(): Promise<{ count: number; vocabSize: number }> {
    await this.ensureLoaded()
    return {
      count: this.entries.length,
      vocabSize: Object.keys(this.idf).length,
    }
  }

  // Clear all vectors
  async clear(): Promise<void> {
    this.entries = []
    this.idf = {}
    await vectorDBClear()
  }

  // Rebuild index from scratch (call after bulk changes)
  async rebuild(items: { id: string; text: string }[]): Promise<void> {
    await vectorDBClear()
    this.entries = []
    this.idf = {}
    this.loaded = false

    for (const item of items) {
      await this.add(item.id, item.text)
    }
  }
}

// Singleton
export const vectorStore = new VectorStore()
