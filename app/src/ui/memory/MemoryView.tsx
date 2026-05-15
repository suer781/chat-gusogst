import { useState, useEffect, useCallback } from 'react'
import { useSettingsStore } from '../stores'
import { ArrowLeft, Search, Trash2, Plus, RefreshCw } from 'lucide-react'
import type { MemoryEntry } from '../../shared/types'

type Bank = 'all' | 'agent' | 'user'

export function MemoryView({ onDone }: { onDone: () => void }) {
  const { memoryManager } = useSettingsStore()
  const [entries, setEntries] = useState<MemoryEntry[]>([])
  const [bank, setBank] = useState<Bank>('all')
  const [query, setQuery] = useState('')
  const [stats, setStats] = useState<any>(null)
  const [loading, setLoading] = useState(false)

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const store = (memoryManager as any).store
      const all = await store.getMemories(bank === 'all' ? undefined : bank)
      setEntries(all)
      const s = await store.getStats()
      setStats(s)
    } finally { setLoading(false) }
  }, [bank, memoryManager])

  useEffect(() => { load() }, [load])

  const handleSearch = async () => {
    if (!query.trim()) { load(); return }
    setLoading(true)
    try {
      const results = await memoryManager.search(query.trim(), 50)
      setEntries(results.map((r: any) => r.entry))
    } finally { setLoading(false) }
  }

  const handleDelete = async (id: string) => {
    const store = (memoryManager as any).store
    await store.deleteMemory(id)
    load()
  }

  const handleMark = async (id: string, helpful: boolean) => {
    await memoryManager.markMemory(id, helpful)
    load()
  }

  const fmtDate = (d: string | Date | number) => {
    const date = typeof d === 'number' ? new Date(d) : typeof d === 'string' ? new Date(d) : d
    return date.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })
  }

  return (
    <div className="view memory-view">
      <div className="view-header">
        <button onClick={onDone}><ArrowLeft size={20} /></button>
        <span>🧠 记忆库</span>
        <button onClick={load}><RefreshCw size={18} className={loading ? 'spin' : ''} /></button>
      </div>

      {/* Stats */}
      {stats && (
        <div className="memory-stats">
          <span>📝 {stats.total ?? 0}</span>
          <span>🤖 {stats.agent ?? 0}</span>
          <span>👤 {stats.user ?? 0}</span>
          <span>📊 均信 {(stats.avgTrust ?? 0).toFixed(2)}</span>
        </div>
      )}

      {/* Bank filter + Search */}
      <div className="memory-toolbar">
        <div className="bank-tabs">
          {(['all', 'agent', 'user'] as Bank[]).map(b => (
            <button key={b} className={bank === b ? 'active' : ''} onClick={() => setBank(b)}>
              {{ all: '全部', agent: '🤖', user: '👤' }[b]}
            </button>
          ))}
        </div>
        <div className="memory-search">
          <input value={query} onChange={e => setQuery(e.target.value)}
            onKeyDown={e => e.key === 'Enter' && handleSearch()}
            placeholder="搜索记忆..." />
          <button onClick={handleSearch}><Search size={16} /></button>
        </div>
      </div>

      {/* List */}
      <div className="memory-list">
        {entries.length === 0 && !loading && (
          <div className="empty">暂无记忆</div>
        )}
        {entries.map(e => (
          <div key={e.id} className="memory-card">
            <div className="mc-header">
              <span className="mc-bank">{(e as any).bank === 'agent' ? '🤖' : '👤'}</span>
              <span className="mc-category">{e.category}</span>
              <span className="mc-trust">信 {((e as any).trustScore ?? 0).toFixed(2)}</span>
              <span className="mc-date">{fmtDate(e.createdAt)}</span>
            </div>
            <div className="mc-content">{e.content}</div>
            <div className="mc-footer">
              <div className="mc-tags">{(e.tags || []).map((t: string) => <span key={t} className="tag">{t}</span>)}</div>
              <div className="mc-actions">
                <button onClick={() => handleMark(e.id, true)} title="有用">👍</button>
                <button onClick={() => handleMark(e.id, false)} title="无用">👎</button>
                <button onClick={() => handleDelete(e.id)} title="删除"><Trash2 size={14} /></button>
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}
