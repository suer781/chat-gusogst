import { useState, useMemo, useCallback, useEffect } from 'react'
import { useSettingsStore } from '../stores'
import { ChevronRight, Search, RefreshCw, ExternalLink, Check, X } from 'lucide-react'
import { t, onLangChange } from '../i18n'
import providersData from '../../data/providers-registry.json'

type ModelInfo = { id: string; name: string; context_length: number; max_output: number; cost_input: number; cost_output: number }
type ProviderInfo = { id: string; name: string; env_key: string; base_url: string; doc: string; api: string; models: ModelInfo[] }

const providers = providersData as ProviderInfo[]

function fmtCtx(n: number) {
  if (n >= 1_000_000) return (n / 1_000_000).toFixed(1).replace(/\.0$/, '') + 'M'
  if (n >= 1_000) return Math.round(n / 1_000) + 'K'
  return String(n)
}
function fmtCost(n: number) { if (n === 0) return ''; if (n < 0.01) return '$' + n.toFixed(4); return '$' + n.toFixed(2) }

export function ProviderSettings({ onDone }: { onDone: () => void }) {
  const [search, setSearch] = useState('')
  const [expandedId, setExpandedId] = useState<string | null>(null)
  const [modelSearch, setModelSearch] = useState('')
  const [apiKeys, setApiKeys] = useState<Record<string, string>>({})
  const [baseUrls, setBaseUrls] = useState<Record<string, string>>({})
  const [fetching, setFetching] = useState<string | null>(null)
  const [liveModels, setLiveModels] = useState<Record<string, ModelInfo[]>>({})
  const [connected, setConnected] = useState<Record<string, boolean>>({})
  const [, forceUpdate] = useState(0)
  const setModel = useSettingsStore((s) => s.setModel)
  const currentModel = useSettingsStore((s) => s.model)

  useEffect(() => { onLangChange(() => forceUpdate((n) => n + 1)); }, [])

  const filtered = useMemo(() => {
    if (!search) return providers
    const q = search.toLowerCase()
    return providers.filter((p) => p.id.includes(q) || p.name.toLowerCase().includes(q) || p.models.some((m) => m.id.includes(q) || m.name.toLowerCase().includes(q)))
  }, [search])

  const totalModels = useMemo(() => providers.reduce((s, p) => s + p.models.length, 0), [])

  const fetchLive = useCallback(async (p: ProviderInfo) => {
    const key = apiKeys[p.id]; const url = baseUrls[p.id] || p.base_url; if (!url) return
    setFetching(p.id)
    try {
      const headers: Record<string, string> = {}; if (key) headers['Authorization'] = 'Bearer ' + key
      const resp = await fetch(url + '/models', { headers })
      if (!resp.ok) throw new Error('HTTP ' + resp.status)
      const data = await resp.json()
      const models: ModelInfo[] = (data.data || []).map((m: any) => ({ id: m.id, name: m.id, context_length: m.context_length ?? 0, max_output: 0, cost_input: 0, cost_output: 0 }))
      setLiveModels((prev) => ({ ...prev, [p.id]: models }))
      setConnected((prev) => ({ ...prev, [p.id]: true }))
    } catch { setConnected((prev) => ({ ...prev, [p.id]: false })) }
    setFetching(null)
  }, [apiKeys, baseUrls])

  const getDisplayModels = (p: ProviderInfo) => {
    const source = liveModels[p.id] ?? p.models
    if (!modelSearch || expandedId !== p.id) return source
    const q = modelSearch.toLowerCase()
    return source.filter((m) => m.id.includes(q) || m.name.toLowerCase().includes(q))
  }

  return (
    <div className="h-full flex flex-col" style={{ background: 'var(--bg-primary)' }}>
      <div style={{ padding: '12px 16px', borderBottom: '1px solid var(--bg-tertiary)' }}>
        <div className="flex items-center justify-between">
          <div>
            <div style={{ fontSize: "var(--text-base)", fontWeight: 600 }}>{providers.length}{t('provider.providers')} · {totalModels}{t('provider.models')}</div>
            <div style={{ fontSize: "var(--text-xs)", color: 'var(--gray-400)' }}>{t('provider.dataFrom')}</div>
          </div>
          {currentModel.model && <div style={{ fontSize: "var(--text-sm)", color: 'var(--accent)', fontWeight: 500 }}>{currentModel.model}</div>}
        </div>
      </div>
      <div style={{ padding: '8px 16px', borderBottom: '1px solid var(--bg-tertiary)' }}>
        <div className="relative">
          <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2" style={{ color: 'var(--gray-400)' }} />
          <input type="text" placeholder={t('provider.search')} value={search} onChange={(e) => setSearch(e.target.value)} className="w-full outline-none" style={{ background: 'var(--bg-tertiary)', border: '1px solid var(--border)', borderRadius: 10, padding: '8px 12px 8px 36px', fontSize: "var(--text-base)", color: 'var(--text-primary)' }} />
        </div>
      </div>
      <div className="flex-1 overflow-y-auto" style={{ padding: "8px 16px", overscrollBehavior: "contain" }}>
        {filtered.map((p) => {
          const expanded = expandedId === p.id; const hasKey = !!apiKeys[p.id]; const isSelected = currentModel.provider === p.id
          return (
            <div key={p.id} style={{ background: 'var(--bg-tertiary)', border: '1px solid ' + (isSelected ? 'var(--accent)40' : 'var(--border)'), borderRadius: 12, marginBottom: 8, overflow: 'hidden' }}>
              <button onClick={() => { setExpandedId(expanded ? null : p.id); setModelSearch('') }} className="w-full flex items-center gap-3" style={{ padding: '12px 16px', background: 'none', border: 'none', cursor: 'pointer', color: 'var(--text-primary)', textAlign: 'left' }}>
                <div className="flex items-center justify-center rounded-lg shrink-0" style={{ width: 32, height: 32, background: 'var(--border)' }}>
                  <img src={'https://cdn.models.dev/icons/' + p.id + '.svg'} alt="" style={{ width: 18, height: 18 }} onError={(e) => { (e.target as HTMLImageElement).style.display = 'none' }} />
                </div>
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2">
                    <span style={{ fontSize: "var(--text-base)", fontWeight: 500 }}>{p.name}</span>
                    {hasKey && <div style={{ width: 6, height: 6, borderRadius: '50%', background: 'var(--success)' }} />}
                  </div>
                  <span style={{ fontSize: "var(--text-sm)", color: 'var(--gray-400)' }}>{p.models.length} {t('provider.models').replace('个', '')}</span>
                </div>
                <ChevronRight size={16} style={{ color: 'var(--gray-400)', transform: expanded ? 'rotate(90deg)' : 'none', transition: 'transform 0.2s' }} />
              </button>
              {expanded && (
                <div style={{ borderTop: '1px solid var(--border)', padding: '12px 16px' }}>
                  <div className="grid grid-cols-2 gap-2" style={{ marginBottom: 8 }}>
                    <div>
                      <label style={{ fontSize: "var(--text-xs)", color: 'var(--gray-400)', marginBottom: 4, display: 'block' }}>{t('provider.apiKey')} {p.env_key && <span style={{ color: 'var(--gray-500)' }}>{'(' + p.env_key + ')'}</span>}</label>
                      <input type="password" placeholder="sk-..." value={apiKeys[p.id] || ''} onChange={(e) => setApiKeys((prev) => ({ ...prev, [p.id]: e.target.value }))} className="w-full outline-none" style={{ background: 'var(--bg-primary)', border: '1px solid var(--border)', borderRadius: 8, padding: '6px 10px', fontSize: "var(--text-base)", color: 'var(--text-primary)' }} />
                    </div>
                    <div>
                      <label style={{ fontSize: "var(--text-xs)", color: 'var(--gray-400)', marginBottom: 4, display: 'block' }}>{t('provider.baseUrl')}</label>
                      <input type="text" placeholder={p.base_url || 'https://...'} value={baseUrls[p.id] || ''} onChange={(e) => setBaseUrls((prev) => ({ ...prev, [p.id]: e.target.value }))} className="w-full outline-none" style={{ background: 'var(--bg-primary)', border: '1px solid var(--border)', borderRadius: 8, padding: '6px 10px', fontSize: "var(--text-base)", color: 'var(--text-primary)' }} />
                    </div>
                  </div>
                  <div className="flex items-center gap-2" style={{ marginBottom: 8 }}>
                    <button onClick={() => fetchLive(p)} disabled={fetching === p.id} style={{ background: 'var(--accent)15', border: 'none', borderRadius: 8, padding: '6px 12px', cursor: 'pointer', color: 'var(--accent)', fontSize: "var(--text-sm)", fontWeight: 500 }}>
                      <RefreshCw size={12} className={fetching === p.id ? 'animate-spin' : ''} style={{ display: 'inline', marginRight: 4 }} /> {t('provider.fetchLive')}
                    </button>
                    {connected[p.id] === true && <Check size={14} style={{ color: 'var(--success)' }} />}
                    {connected[p.id] === false && <X size={14} style={{ color: 'var(--danger)' }} />}
                    {p.doc && <a href={p.doc} target="_blank" rel="noreferrer" style={{ fontSize: "var(--text-sm)", color: 'var(--gray-400)' }}><ExternalLink size={12} style={{ display: 'inline', marginRight: 2 }} /> {t('provider.docs')}</a>}
                  </div>
                  <input type="text" placeholder={p.models.length + t('provider.searchModels')} value={expandedId === p.id ? modelSearch : ''} onChange={(e) => setModelSearch(e.target.value)} className="w-full outline-none" style={{ background: 'var(--bg-primary)', border: '1px solid var(--border)', borderRadius: 8, padding: '6px 10px', fontSize: "var(--text-base)", color: 'var(--text-primary)', marginBottom: 8 }} />
                  <div style={{ maxHeight: 250, overflowY: 'auto' }}>
                    {getDisplayModels(p).map((m) => {
                      const active = currentModel.provider === p.id && currentModel.model === m.id
                      return (
                        <button key={m.id} onClick={() => setModel(p.id, m.id, apiKeys[p.id] || '', baseUrls[p.id] || '')} className="w-full flex items-center gap-2" style={{ background: active ? 'var(--accent)15' : 'transparent', border: 'none', borderRadius: 8, padding: '6px 10px', cursor: 'pointer', color: active ? 'var(--accent)' : 'var(--text-primary)', textAlign: 'left', fontSize: "var(--text-base)" }}>
                          <span className="flex-1 truncate">{m.name || m.id}</span>
                          {m.context_length > 0 && <span style={{ fontSize: "var(--text-xs)", color: 'var(--gray-400)' }}>{fmtCtx(m.context_length)}</span>}
                          {m.cost_input > 0 && <span style={{ fontSize: "var(--text-xs)", color: 'var(--success)80' }}>{fmtCost(m.cost_input)}</span>}
                          {active && <Check size={14} />}
                        </button>
                      )
                    })}
                  </div>
                </div>
              )}
            </div>
          )
        })}
        {filtered.length === 0 && <div className="text-center" style={{ padding: '48px 0', color: 'var(--gray-400)', fontSize: "var(--text-base)" }}>{t('provider.noMatch')}</div>}
      </div>
    </div>
  )
}
