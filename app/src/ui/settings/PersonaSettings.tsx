import { useState } from 'react'
import { useSettingsStore } from '../stores'
import { User, Sparkles, FileText } from 'lucide-react'

const PRESETS = [
  { name: 'Hermes', prompt: 'You are Hermes, a helpful AI assistant.', icon: '🤖', tags: ['general'] },
  { name: '恋人', prompt: '你是用户的虚拟恋人，温柔体贴，关心对方的日常生活和情感。', icon: '💕', tags: ['companion'] },
  { name: '老师', prompt: '你是一位耐心的老师，善于用简单易懂的方式解释复杂概念。', icon: '📚', tags: ['education'] },
  { name: '翻译', prompt: '你是一位专业翻译，请准确翻译用户提供的内容，保持原文风格。', icon: '🌐', tags: ['translate'] },
]

export function PersonaSettings({ onBack }: { onBack: () => void }) {
  const persona = useSettingsStore((s) => s.persona)
  const setPersona = useSettingsStore((s) => s.setPersona)
  const [editing, setEditing] = useState(false)
  const [name, setName] = useState(persona.name)
  const [prompt, setPrompt] = useState(persona.systemPrompt)

  const handleSave = () => {
    setPersona({ ...persona, name, systemPrompt: prompt })
    setEditing(false)
  }

  return (
    <div style={{ minHeight: '100%', background: '#0f0f23', padding: '0 0 100px' }}>
      <div style={{
        display: 'flex', alignItems: 'center', gap: 12, padding: '16px 20px', position: 'sticky', top: 0,
        background: 'rgba(15,15,35,0.9)', backdropFilter: 'blur(20px)', zIndex: 10,
        borderBottom: '1px solid rgba(255,255,255,0.06)',
      }}>
        <button onClick={onBack} style={{
          background: 'none', border: 'none', color: '#e94560', fontSize: 20, cursor: 'pointer', padding: 4,
          display: 'flex', alignItems: 'center',
        }}>{'<-'}</button>
        <span style={{ fontSize: 18, fontWeight: 600, color: '#fff' }}>人设管理</span>
      </div>

      {/* Current Persona Card */}
      <div style={{
        margin: '16px 16px 0', padding: 20,
        background: 'linear-gradient(135deg, rgba(0,184,148,0.1), rgba(0,184,148,0.03))',
        borderRadius: 16, border: '1px solid rgba(0,184,148,0.15)',
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 12 }}>
          <div style={{
            width: 48, height: 48, borderRadius: 14,
            background: 'rgba(0,184,148,0.15)', display: 'flex', alignItems: 'center', justifyContent: 'center',
            fontSize: 24,
          }}>
            <User size={24} color="#00B894" />
          </div>
          <div>
            <div style={{ color: '#fff', fontSize: 16, fontWeight: 600 }}>{persona.name}</div>
            <div style={{ color: '#666', fontSize: 12, marginTop: 2 }}>ID: {persona.id}</div>
          </div>
        </div>
        <div style={{
          color: '#aaa', fontSize: 13, lineHeight: 1.5,
          padding: '10px 12px', background: 'rgba(0,0,0,0.15)', borderRadius: 10,
          maxHeight: 80, overflow: 'hidden',
        }}>
          {persona.systemPrompt}
        </div>
        <button onClick={() => setEditing(!editing)} style={{
          marginTop: 12, padding: '8px 16px', borderRadius: 8,
          background: 'rgba(0,184,148,0.15)', border: '1px solid rgba(0,184,148,0.3)',
          color: '#00B894', fontSize: 13, cursor: 'pointer',
        }}>编辑人设</button>
      </div>

      {/* Edit Form */}
      {editing && (
        <div style={{
          margin: '12px 16px 0', padding: 16,
          background: 'rgba(255,255,255,0.03)', borderRadius: 16,
          border: '1px solid rgba(255,255,255,0.05)',
        }}>
          <div style={{ marginBottom: 12 }}>
            <label style={{ color: '#999', fontSize: 12, marginBottom: 4, display: 'block' }}>名称</label>
            <input value={name} onChange={(e) => setName(e.target.value)} style={{
              width: '100%', padding: '10px 14px', borderRadius: 10,
              background: 'rgba(255,255,255,0.06)', border: '1px solid rgba(255,255,255,0.08)',
              color: '#eee', fontSize: 13, outline: 'none', boxSizing: 'border-box',
            }} />
          </div>
          <div style={{ marginBottom: 12 }}>
            <label style={{ color: '#999', fontSize: 12, marginBottom: 4, display: 'block' }}>系统提示词</label>
            <textarea value={prompt} onChange={(e) => setPrompt(e.target.value)} rows={6} style={{
              width: '100%', padding: '10px 14px', borderRadius: 10,
              background: 'rgba(255,255,255,0.06)', border: '1px solid rgba(255,255,255,0.08)',
              color: '#eee', fontSize: 13, outline: 'none', resize: 'vertical',
              fontFamily: 'inherit', boxSizing: 'border-box',
            }} />
          </div>
          <button onClick={handleSave} style={{
            padding: '10px 24px', borderRadius: 10,
            background: '#00B894', border: 'none',
            color: '#fff', fontSize: 14, fontWeight: 600, cursor: 'pointer',
          }}>保存</button>
        </div>
      )}

      {/* Presets */}
      <div style={{ margin: '20px 16px 0' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 12, color: '#fff', fontSize: 14, fontWeight: 600 }}>
          <Sparkles size={18} />
          <span>预设模板</span>
        </div>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: 10 }}>
          {PRESETS.map((p) => (
            <button key={p.name} onClick={() => {
              setName(p.name)
              setPrompt(p.prompt)
              setPersona({ id: p.name.toLowerCase(), name: p.name, systemPrompt: p.prompt, tags: p.tags })
            }} style={{
              padding: '16px', borderRadius: 14,
              background: persona.name === p.name
                ? 'rgba(0,184,148,0.12)' : 'rgba(255,255,255,0.03)',
              border: persona.name === p.name
                ? '1.5px solid rgba(0,184,148,0.4)' : '1.5px solid transparent',
              cursor: 'pointer', textAlign: 'left',
              transition: 'all 0.2s',
            }}>
              <div style={{ fontSize: 28, marginBottom: 8 }}>{p.icon}</div>
              <div style={{ color: '#fff', fontSize: 14, fontWeight: 500 }}>{p.name}</div>
              <div style={{ color: '#666', fontSize: 11, marginTop: 4, lineHeight: 1.4,
                overflow: 'hidden', textOverflow: 'ellipsis', display: '-webkit-box',
                WebkitLineClamp: 2, WebkitBoxOrient: 'vertical',
              }}>{p.prompt}</div>
            </button>
          ))}
        </div>
      </div>
    </div>
  )
}
