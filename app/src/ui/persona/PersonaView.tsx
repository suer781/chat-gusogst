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
  const [formName, setFormName] = useState('')
  const [formPrompt, setFormPrompt] = useState('')
  const [formTags, setFormTags] = useState('')
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

  const toggleAutoAnalyze = () => {
    if (!sel) return
    personaManager.setAutoAnalyze(sel.id, !(sel.autoAnalyzeSearch !== false))
    setSel(personaManager.getById(sel.id) as PItem); refresh()
  }

  const toggleSearch = (key: 'enableSearch' | 'enableTimeRange') => {
    if (!sel) return
    const sc = personaManager.getSearchConfig(sel.id)
    personaManager.setManualSearchConfig(sel.id, { ...sc, [key]: !sc[key] })
    setSel(personaManager.getById(sel.id) as PItem); refresh()
  }

  const setSampling = (key: string, val: string) => {
    if (!sel) return
    const sc = personaManager.getSamplingConfig(sel.id)
    personaManager.setManualSamplingConfig(sel.id, { ...sc, [key]: Number(val) })
    setSel(personaManager.getById(sel.id) as PItem); refresh()
  }

  const doDelete = (id: string) => {
    personaManager.delete(id)
    if (config.persona.id === id) switchPersona('default')
    setView('list'); refresh()
  }

  const startAdd = () => { setFormName(''); setFormPrompt(''); setFormTags(''); setView('add') }
  const startEdit = (p: PItem) => { setFormName(p.name); setFormPrompt(p.systemPrompt); setFormTags(p.tags.join(',')); setSel(p); setView('edit') }

  const saveNew = () => {
    const n = formName.trim(), p = formPrompt.trim()
    if (!n || !p) return
    addCustomPersona(n, p)
    setView('list'); refresh()
  }
  const saveEdit = () => {
    if (!sel) return
    personaManager.update(sel.id, { name: formName.trim(), systemPrompt: formPrompt.trim(), tags: formTags.split(',').map(t => t.trim()).filter(Boolean) })
    setView('list'); refresh()
  }

  if (view === 'list') return (
    <div className="view persona-view">
      <div className="view-header">
        <button onClick={onDone}><ArrowLeft size={20} /></button>
        <span>人设管理 ({personas.length})</span>
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
