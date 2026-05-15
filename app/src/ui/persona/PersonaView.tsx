import { useState } from 'react'
import { useSettingsStore } from '../stores'
import { ArrowLeft, Plus, Pencil, Trash2, Check, X, RefreshCw } from 'lucide-react'

type Tab = 'info' | 'search' | 'sampling' | 'analysis'
type PItem = { id: string; name: string; systemPrompt: string; tags: string[]; isDefault?: boolean; autoAnalyzeSearch?: boolean }

export function PersonaView({ onDone }: { onDone: () => void }) {
  const { config, personaManager, switchPersona, addCustomPersona } = useSettingsStore()
  const [view, setView] = useState<'list' | 'detail' | 'add' | 'edit'>('list')
  const [sel, setSel] = useState<PItem | null>(null)
  const [tab, setTab] = useState<Tab>('info')
  const [rk, setRk] = useState(0)
  const [fn, setFn] = useState('')
  const [fp, setFp] = useState('')
  const [ft, setFt] = useState('')
  const [busy, setBusy] = useState(false)
  const refresh = () => setRk(k => k + 1)
  const personas = personaManager.listAll()
  const emo: Record<string, string> = { romantic: '💕', professional: '💼', casual: '😊', creative: '🎨', tech: '💻', learning: '📚', humor: '😄', fantasy: '🌙' }

  const openDetail = (p: PItem) => { setSel(p); setTab('info'); setView('detail') }

  const doReAnalyze = async () => {
    if (!sel) return; setBusy(true)
    try { await personaManager.reAnalyze(sel.id); setSel(personaManager.getById(sel.id) as PItem); refresh() }
    finally { setBusy(false) }
  }

  const toggleAuto = () => {
    if (!sel) return
    personaManager.setAutoAnalyze(sel.id, !(sel.autoAnalyzeSearch !== false))
    setSel(personaManager.getById(sel.id) as PItem); refresh()
  }

  const doDelete = (id: string) => {
    personaManager.delete(id)
    if (config.persona.id === id) switchPersona('default')
    setView('list'); refresh()
  }

  const startAdd = () => { setFn(''); setFp(''); setFt(''); setView('add') }
  const startEdit = (p: PItem) => { setFn(p.name); setFp(p.systemPrompt); setFt(p.tags.join(',')); setSel(p); setView('edit') }

  const saveNew = () => {
    const n = fn.trim(), p = fp.trim()
    if (!n || !p) return
    addCustomPersona(n, p); setView('list'); refresh()
  }
  const saveEdit = () => {
    if (!sel) return
    personaManager.update(sel.id, { name: fn.trim(), systemPrompt: fp.trim(), tags: ft.split(',').map(t => t.trim()).filter(Boolean) })
    setView('list'); refresh()
  }

  // ==================== LIST VIEW ====================
  if (view === 'list') return (
    <div className="view persona-view">
      <div className="view-header">
        <button onClick={onDone}><ArrowLeft size={20} /></button>
        <span>🎭 人设管理 ({personas.length})</span>
        <button onClick={startAdd}><Plus size={20} /></button>
      </div>
      <div className="persona-list">
        {personas.map((p: PItem) => (
          <div key={p.id} className="persona-card" onClick={() => openDetail(p)}>
            <div className="pc-row">
              <span className="pc-name">{p.name}</span>
              {config.persona.id === p.id && <Check size={16} className="pc-active" />}
              <span className="pc-tags">{p.tags.map(t => emo[t] || '🏷️').join('')}</span>
            </div>
            <div className="pc-prompt">{p.systemPrompt.slice(0, 60)}...</div>
          </div>
        ))}
      </div>
    </div>
  )

  // ==================== DETAIL VIEW ====================
  if (view !== 'detail' || !sel) return null

  const sc = personaManager.getSearchConfig(sel.id)
  const smp = personaManager.getSamplingConfig(sel.id)
  const ana = personaManager.getAnalysis(sel.id)

  return (
    <div className="view persona-view">
      <div className="view-header">
        <button onClick={() => setView('list')}><ArrowLeft size={20} /></button>
        <span>{sel.name}</span>
        <span>
          <button onClick={() => startEdit(sel)}><Pencil size={18} /></button>
          {!sel.isDefault && <button onClick={() => doDelete(sel.id)}><Trash2 size={18} /></button>}
          <button onClick={() => { switchPersona(sel.id); refresh() }}><Check size={18} /></button>
        </span>
      </div>
      <div className="tab-bar">
        {(['info', 'search', 'sampling', 'analysis'] as Tab[]).map(t => (
          <button key={t} className={tab === t ? 'active' : ''} onClick={() => setTab(t)}>
            {{ info: '📝信息', search: '🔍搜索', sampling: '🎛️采样', analysis: '🧠分析' }[t]}
          </button>
        ))}
      </div>
      <div className="tab-content">
        {/* ---- INFO TAB ---- */}
        {tab === 'info' && (
          <div className="tab-panel">
            <label>名称</label><div>{sel.name}</div>
            <label>标签</label><div>{sel.tags.map(t => (emo[t] || '🏷️') + ' ' + t).join('  ')}</div>
            <label>提示词</label><div className="prompt-box">{sel.systemPrompt}</div>
            <label>智能分析开关</label>
            <div className="toggle-row">
              <span>{sel.autoAnalyzeSearch !== false ? '✅ 已开启' : '❌ 已关闭'}</span>
              <button onClick={toggleAuto}>切换</button>
            </div>
            <button className="btn-reanalyze" onClick={doReAnalyze} disabled={busy}>
              <RefreshCw size={16} className={busy ? 'spin' : ''} /> {busy ? '分析中...' : '重新AI分析'}
            </button>
          </div>
        )}
        {/* ---- SEARCH TAB ---- */}
        {tab === 'search' && (
          <div className="tab-panel">
            {sc ? (
              <>
                <label>启用搜索</label><div>{sc.enableSearch ? '✅' : '❌'}</div>
                <label>启用时间范围</label><div>{sc.enableTimeRange ? '✅' : '❌'}</div>
                <label>并发数</label><div>{sc.concurrency}</div>
                <label>搜索引擎</label><div>{sc.engines.join(', ')}</div>
                {sc.engineWeights && (
                  <><label>引擎权重</label><div>{Object.entries(sc.engineWeights).map(([k, v]) => k + ': ' + v).join(' | ')}</div></>
                )}
              </>
            ) : (
              <div className="empty">搜索配置未初始化，请先运行AI分析</div>
            )}
          </div>
        )}
        {/* ---- SAMPLING TAB ---- */}
        {tab === 'sampling' && (
          <div className="tab-panel">
            {smp ? (
              <>
                <label>Temperature</label><div>{smp.temperature}</div>
                <label>Top P</label><div>{smp.topP}</div>
                <label>Max Tokens</label><div>{smp.maxTokens}</div>
                <label>频率惩罚</label><div>{smp.frequencyPenalty}</div>
                <label>存在惩罚</label><div>{smp.presencePenalty}</div>
              </>
            ) : (
              <div className="empty">采样配置未初始化，请先运行AI分析</div>
            )}
          </div>
        )}
        {/* ---- ANALYSIS TAB ---- */}
        {tab === 'analysis' && (
          <div className="tab-panel">
            {ana ? (
              <>
                <label>推荐标签</label><div>{ana.tags.join(', ')}</div>
                <label>使用场景</label><div>{ana.scenarios.join(', ')}</div>
                <label>推荐引擎</label><div>{ana.recommendedEngines.join(', ')}</div>
                <label>引擎推荐理由</label>
                <div>{Object.entries(ana.engineReasons).map(([k, v]) => k + ': ' + v).join('\n')}</div>
                <label>搜索并发数</label><div>{ana.concurrency}</div>
                <label>启用搜索</label><div>{ana.enableSearch ? '✅' : '❌'}</div>
                <label>启用时间范围</label><div>{ana.enableTimeRange ? '✅' : '❌'}</div>
                <label>采样参数</label>
                <div>T={ana.sampling.temperature} topP={ana.sampling.topP} maxT={ana.sampling.maxTokens}</div>
              </>
            ) : (
              <div className="empty">暂无分析结果，点击「信息」Tab 中的「重新AI分析」生成</div>
            )}
          </div>
        )}
      </div>
    </div>
  )

  // ==================== ADD / EDIT FORM ====================
  // (handled by early return above for detail view)
}

// Add/Edit form as separate component
export function PersonaForm({ mode, initial, onSave, onCancel }: {
  mode: 'add' | 'edit'
  initial?: PItem
  onSave: (name: string, prompt: string, tags: string[]) => void
  onCancel: () => void
}) {
  const [fn, setFn] = useState(initial?.name || '')
  const [fp, setFp] = useState(initial?.systemPrompt || '')
  const [ft, setFt] = useState(initial?.tags.join(',') || '')
  return (
    <div className="persona-form">
      <label>名称</label>
      <input value={fn} onChange={e => setFn(e.target.value)} placeholder="人设名称" />
      <label>标签（逗号分隔）</label>
      <input value={ft} onChange={e => setFt(e.target.value)} placeholder="romantic,creative" />
      <label>系统提示词</label>
      <textarea value={fp} onChange={e => setFp(e.target.value)} rows={8} placeholder="描述人设的性格、语气、行为模式..." />
      <div className="form-actions">
        <button onClick={() => onSave(fn.trim(), fp.trim(), ft.split(',').map(t => t.trim()).filter(Boolean))} disabled={!fn.trim() || !fp.trim()}>
          <Check size={16} /> 保存
        </button>
        <button onClick={onCancel}><X size={16} /> 取消</button>
      </div>
    </div>
  )
}
