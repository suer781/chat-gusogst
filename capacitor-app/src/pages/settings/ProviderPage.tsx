import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useModel } from '../../hooks/useStore'
import type { ModelConfig } from '../../lib/types'

const PROVIDERS = [
  // 云服务商
  { id: 'openai', name: 'OpenAI', icon: '🤖', models: ['gpt-4o', 'gpt-4o-mini', 'gpt-4-turbo', 'o1-preview'], host: 'https://api.openai.com', category: 'cloud' },
  { id: 'anthropic', name: 'Anthropic', icon: '🧠', models: ['claude-3.5-sonnet', 'claude-3-opus', 'claude-3-haiku'], host: 'https://api.anthropic.com', category: 'cloud' },
  // 第三方
  { id: 'deepseek', name: 'DeepSeek', icon: '🔮', models: ['deepseek-chat', 'deepseek-reasoner'], host: 'https://api.deepseek.com', category: 'third' },
  { id: 'zhipu', name: '智谱 GLM', icon: '🧩', models: ['glm-4', 'glm-4-flash'], host: 'https://open.bigmodel.cn/api', category: 'third' },
  { id: 'moonshot', name: '月之暗面', icon: '🌙', models: ['moonshot-v1-128k', 'moonshot-v1-32k'], host: 'https://api.moonshot.cn', category: 'third' },
  // 本地
  { id: 'custom', name: '自定义', icon: '⚙️', models: ['custom-model'], host: '', category: 'local' },
]

const CATEGORY_LABELS: Record<string, string> = {
  cloud: '云服务商',
  third: '第三方',
  local: '本地',
}

export default function ProviderPage() {
  const navigate = useNavigate()
  const { model, saveModel } = useModel()

  const [showForm, setShowForm] = useState(false)
  const [form, setForm] = useState<ModelConfig>({
    provider: 'openai', model: 'gpt-4o', apiKey: '', apiHost: '', temperature: 0.7, maxTokens: 4096,
  })
  const [saved, setSaved] = useState(false)

  const handleSelectProvider = (providerId: string) => {
    const provider = PROVIDERS.find(p => p.id === providerId)
    if (!provider) return
    setForm({ provider: providerId, model: provider.models[0], apiKey: model.apiKey, apiHost: provider.host, temperature: model.temperature, maxTokens: model.maxTokens })
    setShowForm(true)
  }

  const handleSave = () => {
    saveModel(form)
    setSaved(true)
    setTimeout(() => setSaved(false), 1500)
  }

  // ── 配置表单视图 ──
  if (showForm) {
    const provider = PROVIDERS.find(p => p.id === form.provider)
    return (
      <div className="sub-page">
        <div className="sub-header">
          <button className="sub-back" onClick={() => setShowForm(false)}>
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M15 18l-6-6 6-6"/></svg>
          </button>
          <h2 className="sub-title">配置 {provider?.name}</h2>
        </div>
        <div className="sub-body" style={{ padding: '0 16px 24px' }}>
          {/* 当前模型概览 */}
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px', padding: '20px 0 16px' }}>
            <span style={{ fontSize: '32px' }}>{provider?.icon}</span>
            <div>
              <div style={{ fontSize: '17px', fontWeight: '600', color: 'var(--text)' }}>{provider?.name}</div>
              <div style={{ fontSize: '13px', color: 'var(--text-secondary)', marginTop: '2px' }}>{form.model}</div>
            </div>
          </div>

          {/* 表单卡片 */}
          <div style={{ background: 'var(--card-bg)', borderRadius: '14px', overflow: 'hidden' }}>
            <FormRow label="模型">
              <select
                value={form.model}
                onChange={e => setForm({ ...form, model: e.target.value })}
                style={inputStyle}
              >
                {provider?.models.map(m => <option key={m} value={m}>{m}</option>)}
              </select>
            </FormRow>
            <FormDivider />
            <FormRow label="API 密钥">
              <input
                type="password"
                value={form.apiKey}
                onChange={e => setForm({ ...form, apiKey: e.target.value })}
                placeholder="sk-..."
                style={inputStyle}
              />
            </FormRow>
            <FormDivider />
            <FormRow label="API 地址">
              <input
                type="text"
                value={form.apiHost}
                onChange={e => setForm({ ...form, apiHost: e.target.value })}
                placeholder={provider?.host}
                style={inputStyle}
              />
            </FormRow>
            <FormDivider />
            <div style={{ padding: '14px 16px' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '10px' }}>
                <span style={{ fontSize: '14px', color: 'var(--text)' }}>Temperature</span>
                <span style={{ fontSize: '14px', color: 'var(--accent)', fontWeight: '600', fontVariantNumeric: 'tabular-nums' }}>{form.temperature}</span>
              </div>
              <input
                type="range" min="0" max="2" step="0.1"
                value={form.temperature}
                onChange={e => setForm({ ...form, temperature: parseFloat(e.target.value) })}
                style={{ width: '100%' }}
              />
            </div>
          </div>

          {/* 保存按钮 */}
          <button
            onClick={handleSave}
            style={{
              width: '100%', padding: '15px',
              background: saved ? '#22c55e' : 'var(--accent)',
              color: '#fff', border: 'none', borderRadius: '14px',
              fontSize: '16px', fontWeight: '600', cursor: 'pointer',
              marginTop: '20px', transition: 'all 0.2s',
            }}
          >
            {saved ? '✓ 已保存' : '保存配置'}
          </button>
        </div>
      </div>
    )
  }

  // ── 提供商网格视图 ──
  const categories = ['cloud', 'third', 'local'] as const

  return (
    <div className="sub-page">
      <div className="sub-header">
        <button className="sub-back" onClick={() => navigate('/settings')}>
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M15 18l-6-6 6-6"/></svg>
        </button>
        <h2 className="sub-title">模型提供商</h2>
      </div>
      <div className="sub-body" style={{ padding: '0 16px 24px' }}>
        {/* 当前使用中 */}
        <div style={{
          display: 'flex', alignItems: 'center', gap: '12px',
          padding: '16px', marginBottom: '24px',
          background: 'var(--card-bg)', borderRadius: '14px',
          border: '1px solid var(--accent)', borderLeftWidth: '4px',
        }}>
          <span style={{ fontSize: '28px' }}>
            {PROVIDERS.find(p => p.id === model.provider)?.icon || '⚙️'}
          </span>
          <div style={{ flex: 1 }}>
            <div style={{ fontSize: '15px', fontWeight: '600', color: 'var(--text)' }}>
              {PROVIDERS.find(p => p.id === model.provider)?.name || model.provider}
            </div>
            <div style={{ fontSize: '13px', color: 'var(--text-secondary)', marginTop: '2px' }}>
              {model.model}
            </div>
          </div>
          <span style={{
            fontSize: '11px', color: '#fff', background: 'var(--accent)',
            padding: '3px 10px', borderRadius: '20px', fontWeight: '600',
          }}>使用中</span>
        </div>

        {/* 分类网格 */}
        {categories.map(cat => {
          const items = PROVIDERS.filter(p => p.category === cat)
          if (items.length === 0) return null
          return (
            <div key={cat} style={{ marginBottom: '20px' }}>
              <div style={{
                fontSize: '13px', fontWeight: '600', color: 'var(--text-secondary)',
                marginBottom: '10px', paddingLeft: '4px',
                textTransform: 'uppercase', letterSpacing: '0.5px',
              }}>
                {CATEGORY_LABELS[cat]}
              </div>
              <div style={{
                display: 'grid',
                gridTemplateColumns: 'repeat(3, 1fr)',
                gap: '10px',
              }}>
                {items.map(p => {
                  const isActive = model.provider === p.id
                  return (
                    <button
                      key={p.id}
                      onClick={() => handleSelectProvider(p.id)}
                      style={{
                        position: 'relative',
                        display: 'flex', flexDirection: 'column',
                        alignItems: 'center', justifyContent: 'center',
                        gap: '8px', padding: '18px 8px',
                        background: 'var(--card-bg)',
                        border: isActive ? '2px solid var(--accent)' : '2px solid transparent',
                        borderRadius: '14px',
                        cursor: 'pointer',
                        transition: 'all 0.15s',
                      }}
                    >
                      {isActive && (
                        <span style={{
                          position: 'absolute', top: '6px', right: '6px',
                          width: '18px', height: '18px',
                          background: 'var(--accent)', borderRadius: '50%',
                          display: 'flex', alignItems: 'center', justifyContent: 'center',
                          fontSize: '10px', color: '#fff',
                        }}>✓</span>
                      )}
                      <span style={{ fontSize: '28px', lineHeight: 1 }}>{p.icon}</span>
                      <span style={{
                        fontSize: '12px', fontWeight: '500', color: 'var(--text)',
                        textAlign: 'center', lineHeight: '1.3',
                        overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap',
                        maxWidth: '100%',
                      }}>
                        {p.name}
                      </span>
                    </button>
                  )
                })}
              </div>
            </div>
          )
        })}
      </div>
    </div>
  )
}

// ── 表单子组件 ──
function FormRow({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div style={{ display: 'flex', alignItems: 'center', padding: '0 16px', minHeight: '48px' }}>
      <span style={{ fontSize: '14px', color: 'var(--text)', minWidth: '72px', flexShrink: 0 }}>{label}</span>
      <div style={{ flex: 1 }}>{children}</div>
    </div>
  )
}

function FormDivider() {
  return <div style={{ height: '0.5px', background: 'var(--border)', marginLeft: '16px' }} />
}

const inputStyle: React.CSSProperties = {
  width: '100%', padding: '12px 0', border: 'none',
  background: 'transparent', fontSize: '14px',
  color: 'var(--text)', outline: 'none', textAlign: 'right',
}
