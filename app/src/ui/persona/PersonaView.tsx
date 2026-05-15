import { useState } from 'react'
import { useSettingsStore } from '../stores'
import { ArrowLeft, Plus, X, Pencil, Trash2, Check } from 'lucide-react'

type PersonaItem = { id: string; name: string; systemPrompt: string; tags: string[]; isCustom?: boolean }

export function PersonaView({ onDone }: { onDone: () => void }) {
  const { config, personaManager, switchPersona, addCustomPersona } = useSettingsStore()
  const [showAdd, setShowAdd] = useState(false)
  const [newName, setNewName] = useState('')
  const [newPrompt, setNewPrompt] = useState('')
  const [editId, setEditId] = useState<string | null>(null)
  const [editName, setEditName] = useState('')
  const [editPrompt, setEditPrompt] = useState('')
  const [refreshKey, setRefreshKey] = useState(0)
  const personas = personaManager.listAll()

  const refresh = () => setRefreshKey(k => k + 1)

  const handleAdd = () => {
    const n = newName.trim(), p = newPrompt.trim()
    if (!n || !p) return
    addCustomPersona(n, p)
    setNewName(''); setNewPrompt(''); setShowAdd(false)
    refresh()
  }

  const startEdit = (per: PersonaItem) => {
    setEditId(per.id); setEditName(per.name); setEditPrompt(per.systemPrompt)
  }

  const handleSave = () => {
    if (!editId) return
    personaManager.update(editId, { name: editName.trim(), systemPrompt: editPrompt.trim() })
    setEditId(null); refresh()
  }

  const handleDelete = (id: string) => {
    personaManager.delete(id)
    if (config.persona.id === id) switchPersona('default')
    refresh()
  }

  return (
    <div className="view persona-view">
      <div className="view-header">
        <button onClick={onDone}><ArrowLeft size={20} /></button>
        <span>人设管理</span>
        <button onClick={() => setShowAdd(true)}><Plus size={20} /></button>
      </div>

      {showAdd && (
        <div className="persona-add-form">
          <input value={newName} onChange={e => setNewName(e.target.value)} placeholder="人设名称" />
          <textarea value={newPrompt} onChange={e => setNewPrompt(e.target.value)} placeholder="系统提示词" rows={4} />
          <button onClick={handleAdd} disabled={!newName.trim() || !newPrompt.trim()}>保存</button>
          <button onClick={() => setShowAdd(false)}>取消</button>
        </div>
      )}

      <div className="persona-list">
        {personas.map(p => (
          editId === p.id ? (
            <div key={p.id} className="persona-card edit">
              <input value={editName} onChange={e => setEditName(e.target.value)} />
              <textarea value={editPrompt} onChange={e => setEditPrompt(e.target.value)} rows={4} />
              <div className="actions">
                <button onClick={handleSave}><Check size={16} /></button>
                <button onClick={() => setEditId(null)}><X size={16} /></button>
              </div>
            </div>
          ) : (
            <div key={p.id} className="persona-card" onClick={() => switchPersona(p.id)}>
              <div>{p.name}</div>
              <div>{p.systemPrompt.slice(0, 50)}...</div>
              <div className="actions">
                <button onClick={e => { e.stopPropagation(); startEdit(p) }}><Pencil size={16} /></button>
                {!p.isDefault && (
                  <button onClick={e => { e.stopPropagation(); handleDelete(p.id) }}><Trash2 size={16} /></button>
                )}
              </div>
            </div>
          )
        ))}
      </div>
    </div>
  )
}
