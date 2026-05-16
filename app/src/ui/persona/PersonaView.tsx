import { useState } from 'react'
import { useSettingsStore } from '../stores'
import { ArrowLeft, Plus, X } from 'lucide-react'

export function PersonaView({ onDone }: { onDone: () => void }) {
  const { config, personaManager, switchPersona, addCustomPersona } = useSettingsStore()
  const [showAdd, setShowAdd] = useState(false)
  const [newName, setNewName] = useState('')
  const [newPrompt, setNewPrompt] = useState('')
  const personas = personaManager.listAll()

  const handleAdd = () => {
    if (!newName.trim() || !newPrompt.trim()) return
    addCustomPersona(newName.trim(), newPrompt.trim())
    setNewName('')
    setNewPrompt('')
    setShowAdd(false)
  }

  return (
    <div className="persona-view">
      <div className="settings-header">
        <button onClick={onDone} className="icon-btn"><ArrowLeft size={20} /></button>
        <h2>选择人设</h2>
        <button onClick={() => setShowAdd(!showAdd)} className="icon-btn">
          {showAdd ? <X size={20} /> : <Plus size={20} />}
        </button>
      </div>

      {showAdd && (
        <div className="persona-add-form">
          <input type="text" value={newName} onChange={e => setNewName(e.target.value)} placeholder="人设名称" />
          <textarea value={newPrompt} onChange={e => setNewPrompt(e.target.value)} placeholder="系统提示词：描述这个人设的性格和说话方式..." rows={4} />
          <button onClick={handleAdd} className="primary-btn">添加</button>
        </div>
      )}

      <div className="persona-list">
        {personas.map(p => (
          <div
            key={p.id}
            className={`persona-card ${p.id === config.persona.id ? 'active' : ''}`}
            onClick={() => switchPersona(p.id)}
          >
            <div className="persona-card-name">{p.name}</div>
            <div className="persona-card-preview">{p.systemPrompt.slice(0, 60)}...</div>
            <div className="persona-card-tags">
              {p.tags.map(t => <span key={t} className="tag">{t}</span>)}
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}
