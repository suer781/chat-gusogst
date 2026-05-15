import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useModel, usePersona, usePersonas } from '../hooks/useStore'
import type { ModelConfig } from '../lib/types'
import './SettingsPage.css'

export default function SettingsPage() {
  const navigate = useNavigate()
  const { model, saveModel } = useModel()
  const { persona } = usePersona()
  const { allPersonas, deletePersona } = usePersonas()
  const [form, setForm] = useState<ModelConfig>({ ...model })
  const [saved, setSaved] = useState(false)
  const [showDeleteConfirm, setShowDeleteConfirm] = useState<string | null>(null)

  const update = <K extends keyof ModelConfig>(key: K, value: ModelConfig[K]) => {
    setForm(f => ({ ...f, [key]: value }))
    setSaved(false)
  }

  const save = () => { saveModel(form); setSaved(true) }

  const handleDelete = (id: string) => {
    deletePersona(id)
    setShowDeleteConfirm(null)
  }

  return (
    <div className="settings-page">
      <header className="settings-header">
        <button className="back-btn" onClick={() => navigate(-1)}>←</button>
        <span>设置</span>
        <button className="save-btn" onClick={save}>{saved ? '✓ 已保存' : '保存'}</button>
      </header>
      <div className="settings-body">
        {/* 模型配置 */}
        <section>
          <h3>🤖 模型配置</h3>
          <div className="provider-chips">
            <button className={'chip' + (form.provider === 'openai' ? ' active' : '')} onClick={() => update('provider', 'openai')}>OpenAI</button>
            <button className={'chip' + (form.provider === 'anthropic' ? ' active' : '')} onClick={() => update('provider', 'anthropic')}>Anthropic</button>
          </div>
          <label>模型名称</label>
          <input value={form.model} onChange={e => update('model', e.target.value)} />
          <label>API Key</label>
          <input type="password" value={form.apiKey} onChange={e => update('apiKey', e.target.value)} />
          <label>API Host</label>
          <input value={form.apiHost} onChange={e => update('apiHost', e.target.value)} />
          <label>Temperature: {form.temperature.toFixed(1)}</label>
          <input type="range" min="0" max="2" step="0.1" value={form.temperature} onChange={e => update('temperature', +e.target.value)} />
        </section>

        {/* 角色设定管理 */}
        <section>
          <div className="section-header">
            <h3>🎭 角色设定</h3>
            <button className="section-btn" onClick={() => navigate('/persona')}>管理 →</button>
          </div>
          <div className="persona-list-compact">
            {allPersonas.map(p => (
              <div key={p.id} className={'persona-item' + (persona.id === p.id ? ' active' : '')}>
                <span className="pi-avatar">{p.avatar}</span>
                <span className="pi-name">{p.name}</span>
                {persona.id === p.id && <span className="pi-badge">当前</span>}
                {!['default','tsundere','cool','bookworm','genki'].includes(p.id) && (
                  showDeleteConfirm === p.id ? (
                    <div className="pi-confirm">
                      <button className="pi-confirm-yes" onClick={() => handleDelete(p.id)}>删除</button>
                      <button className="pi-confirm-no" onClick={() => setShowDeleteConfirm(null)}>取消</button>
                    </div>
                  ) : (
                    <button className="pi-delete" onClick={(e) => { e.stopPropagation(); setShowDeleteConfirm(p.id) }}>✕</button>
                  )
                )}
              </div>
            ))}
          </div>
        </section>

        {/* 记忆库 */}
        <section>
          <div className="section-header">
            <h3>🧠 记忆库</h3>
          </div>
          <div className="memory-section">
            <p className="memory-desc">AI 会记住你们的对话，在新的聊天中延续记忆。</p>
            <div className="memory-stats">
              <div className="stat-item">
                <span className="stat-label">记忆状态</span>
                <span className="stat-value">✅ 已启用</span>
              </div>
            </div>
          </div>
        </section>
      </div>
    </div>
  )
}
