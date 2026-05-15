import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { usePersona, usePersonas } from '../hooks/useStore'
import { PRESETS } from '../lib/types'
import type { Persona } from '../lib/types'
import './PersonaPage.css'

type EditMode = { type: 'create' } | { type: 'edit'; persona: Persona } | null

export default function PersonaPage() {
  const navigate = useNavigate()
  const { persona, savePersona } = usePersona()
  const { customPersonas, addPersona, updatePersona, deletePersona } = usePersonas()
  const [editMode, setEditMode] = useState<EditMode>(null)
  const [editName, setEditName] = useState('')
  const [editAvatar, setEditAvatar] = useState('🤖')
  const [editPrompt, setEditPrompt] = useState('')
  const [showDeleteConfirm, setShowDeleteConfirm] = useState<string | null>(null)

  const startCreate = () => {
    setEditName('')
    setEditAvatar('🤖')
    setEditPrompt('')
    setEditMode({ type: 'create' })
  }

  const startEdit = (p: Persona) => {
    setEditName(p.name)
    setEditAvatar(p.avatar)
    setEditPrompt(p.systemPrompt)
    setEditMode({ type: 'edit', persona: p })
  }

  const saveEdit = () => {
    if (!editName.trim() || !editPrompt.trim()) return
    if (editMode?.type === 'create') {
      const newP: Persona = {
        id: 'custom_' + Date.now(),
        name: editName.trim(),
        avatar: editAvatar,
        systemPrompt: editPrompt.trim()
      }
      addPersona(newP)
      savePersona(newP)
    } else if (editMode?.type === 'edit') {
      updatePersona(editMode.persona.id, {
        name: editName.trim(),
        avatar: editAvatar,
        systemPrompt: editPrompt.trim()
      })
    }
    setEditMode(null)
  }

  const handleDelete = (id: string) => {
    if (persona.id === id) savePersona(PRESETS[0])
    deletePersona(id)
    setShowDeleteConfirm(null)
  }

  // 编辑模式
  if (editMode) {
    return (
      <div className="persona-page">
        <header className="persona-header">
          <button className="back-btn" onClick={() => setEditMode(null)}>←</button>
          <span>{editMode.type === 'create' ? '创建角色' : '编辑角色'}</span>
          <button className="save-btn" onClick={saveEdit}>保存</button>
        </header>
        <div className="persona-editor">
          <div className="editor-avatar-row">
            <input
              className="editor-avatar-input"
              value={editAvatar}
              onChange={e => setEditAvatar(e.target.value || '🤖')}
              maxLength={2}
            />
            <input
              className="editor-name-input"
              value={editName}
              onChange={e => setEditName(e.target.value)}
              placeholder="角色名称"
            />
          </div>
          <label>系统提示词</label>
          <textarea
            className="editor-prompt-input"
            value={editPrompt}
            onChange={e => setEditPrompt(e.target.value)}
            placeholder="描述这个角色的性格、说话方式、行为模式..."
            rows={6}
          />
          <p className="editor-hint">💡 越详细的角色描述，AI 越能演好这个角色。</p>
        </div>
      </div>
    )
  }

  // 选择模式
  return (
    <div className="persona-page">
      <header className="persona-header">
        <span>选择角色</span>
      </header>

      {/* 预设角色 */}
      <div className="persona-section-label">预设角色</div>
      <div className="persona-list">
        {PRESETS.map(p => (
          <div
            key={p.id}
            className={'persona-card' + (persona.id === p.id ? ' selected' : '')}
            onClick={() => savePersona(p)}
          >
            <span className="persona-avatar">{p.avatar}</span>
            <div className="persona-info">
              <span className="persona-name">{p.name}</span>
              <span className="persona-desc">{p.systemPrompt.slice(0, 40)}...</span>
            </div>
            {persona.id === p.id && <span className="check">✓</span>}
          </div>
        ))}
      </div>

      {/* 自定义角色 */}
      <div className="persona-section-label">
        我的角色
        <button className="add-persona-btn" onClick={startCreate}>+ 新建</button>
      </div>
      <div className="persona-list">
        {customPersonas.length === 0 && (
          <div className="persona-empty">还没有自定义角色，点击上方「+ 新建」创建一个吧</div>
        )}
        {customPersonas.map(p => (
          <div
            key={p.id}
            className={'persona-card' + (persona.id === p.id ? ' selected' : '')}
            onClick={() => savePersona(p)}
          >
            <span className="persona-avatar">{p.avatar}</span>
            <div className="persona-info">
              <span className="persona-name">{p.name}</span>
              <span className="persona-desc">{p.systemPrompt.slice(0, 40)}...</span>
            </div>
            <div className="persona-actions">
              <button className="persona-edit-btn" onClick={(e) => { e.stopPropagation(); startEdit(p) }}>✏️</button>
              {showDeleteConfirm === p.id ? (
                <div className="delete-confirm">
                  <button className="confirm-yes" onClick={(e) => { e.stopPropagation(); handleDelete(p.id) }}>删</button>
                  <button className="confirm-no" onClick={(e) => { e.stopPropagation(); setShowDeleteConfirm(null) }}>×</button>
                </div>
              ) : (
                <button className="persona-delete-btn" onClick={(e) => { e.stopPropagation(); setShowDeleteConfirm(p.id) }}>✕</button>
              )}
              {persona.id === p.id && <span className="check">✓</span>}
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}
