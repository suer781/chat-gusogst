import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useModel } from '../../hooks/useStore'

/* ─── 数据 ─── */
type ProviderId = 'openai' | 'anthropic' | 'deepseek' | 'zhipu' | 'moonshot' | 'custom'

interface Provider {
  id: ProviderId
  name: string
  icon: string
  models: string[]
  host: string
  category: 'cloud' | 'third' | 'local'
}

const PROVIDERS: Provider[] = [
  { id: 'openai',     name: 'OpenAI',    icon: '🤖', models: ['gpt-4o', 'gpt-4o-mini', 'gpt-4-turbo', 'o1-preview'],  host: 'https://api.openai.com',         category: 'cloud' },
  { id: 'anthropic',  name: 'Anthropic',  icon: '🧠', models: ['claude-3.5-sonnet', 'claude-3-opus', 'claude-3-haiku'], host: 'https://api.anthropic.com',      category: 'cloud' },
  { id: 'deepseek',   name: 'DeepSeek',   icon: '🔮', models: ['deepseek-chat', 'deepseek-reasoner'],                  host: 'https://api.deepseek.com',       category: 'third' },
  { id: 'zhipu',      name: '智谱 GLM',   icon: '🧩', models: ['glm-4', 'glm-4-flash'],                                host: 'https://open.bigmodel.cn/api',   category: 'third' },
  { id: 'moonshot',   name: '月之暗面',   icon: '🌙', models: ['moonshot-v1-128k', 'moonshot-v1-32k'],                 host: 'https://api.moonshot.cn',        category: 'third' },
  { id: 'custom',     name: '自定义',     icon: '⚙️', models: ['custom-model'],                                         host: '',                               category: 'local' },
]

const CATEGORY_LABELS: Record<string, string> = {
  cloud: '云服务商',
  third: '第三方',
  local: '本地',
}

const CATEGORY_ORDER = ['cloud', 'third', 'local'] as const

/* ─── 样式 ─── */
const css = {
  page: {
    display: 'flex', flexDirection: 'column' as const, height: '100%',
    background: 'var(--surface)', overflow: 'hidden',
  },
  header: {
    padding: '16px 20px', paddingTop: 'calc(16px + var(--safe-top, 0px))',
    background: 'linear-gradient(135deg, var(--primary) 0%, var(--primary-light) 100%)',
    display: 'flex', alignItems: 'center', gap: '12px',
    boxShadow: '0 2px 12px rgba(30,58,95,0.15)',
    transition: 'all 0.35s cubic-bezier(0.4,0,0.2,1)',
  },
  backBtn: {
    background: 'none', border: 'none', color: '#fff', fontSize: '20px',
    cursor: 'pointer', padding: '4px', borderRadius: '8px',
    display: 'flex', alignItems: 'center',
    transition: 'all 0.25s cubic-bezier(0.4,0,0.2,1)',
  },
  headerTitle: {
    fontSize: '17px', fontWeight: '600' as const, color: '#fff',
    letterSpacing: '0.3px',
  },
  body: {
    flex: 1, overflowY: 'auto' as const, padding: '16px',
    WebkitOverflowScrolling: 'touch' as const,
  },

  /* ── 当前使用中卡片 ── */
  activeCard: {
    display: 'flex', alignItems: 'center', gap: '14px',
    padding: '18px 20px', marginBottom: '24px',
    borderRadius: '16px',
    background: 'linear-gradient(135deg, var(--primary) 0%, var(--primary-light) 100%)',
    boxShadow: '0 4px 20px rgba(30,58,95,0.2)',
    color: '#fff',
    animation: 'cardFadeIn 0.4s cubic-bezier(0.4,0,0.2,1) both',
  },
  activeIcon: {
    fontSize: '36px', width: '52px', height: '52px',
    display: 'flex', alignItems: 'center', justifyContent: 'center',
    borderRadius: '14px',
    background: 'rgba(255,255,255,0.15)',
    backdropFilter: 'blur(8px)',
    transition: 'transform 0.3s cubic-bezier(0.4,0,0.2,1)',
  },
  activeInfo: { flex: 1, minWidth: 0 },
  activeName: {
    fontSize: '17px', fontWeight: '600' as const,
    letterSpacing: '0.3px', marginBottom: '2px',
  },
  activeModel: {
    fontSize: '13px', opacity: 0.8,
    whiteSpace: 'nowrap' as const, overflow: 'hidden', textOverflow: 'ellipsis',
  },
  activeBadge: {
    fontSize: '11px', padding: '4px 10px',
    borderRadius: '20px',
    background: 'rgba(255,255,255,0.2)',
    backdropFilter: 'blur(6px)',
    fontWeight: '500' as const, letterSpacing: '0.5px',
    whiteSpace: 'nowrap' as const,
  },

  /* ── 分类标题 ── */
  categoryTitle: {
    fontSize: '13px', fontWeight: '600' as const,
    color: 'var(--text-secondary)',
    textTransform: 'uppercase' as const, letterSpacing: '1px',
    margin: '0 0 10px 4px',
    transition: 'color 0.3s ease',
  },

  /* ── 提供商网格 ── */
  grid: {
    display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)',
    gap: '12px', marginBottom: '24px',
  },
  providerCard: (isActive: boolean) => ({
    display: 'flex', flexDirection: 'column' as const,
    alignItems: 'center', gap: '10px',
    padding: '20px 12px 16px',
    borderRadius: '16px',
    border: isActive
      ? '2px solid var(--accent)'
      : '2px solid transparent',
    background: isActive
      ? 'linear-gradient(145deg, var(--accent-glow) 0%, var(--card) 100%)'
      : 'var(--card)',
    boxShadow: isActive
      ? '0 4px 16px rgba(74,144,217,0.18), 0 0 0 1px var(--accent)'
      : '0 1px 4px rgba(0,0,0,0.05)',
    cursor: 'pointer',
    transition: 'all 0.35s cubic-bezier(0.4,0,0.2,1)',
    position: 'relative' as const,
    animation: 'cardFadeIn 0.4s cubic-bezier(0.4,0,0.2,1) both',
  }),
  checkMark: {
    position: 'absolute' as const, top: '10px', right: '10px',
    width: '22px', height: '22px', borderRadius: '50%',
    background: 'linear-gradient(135deg, var(--accent) 0%, #6CB4EE 100%)',
    color: '#fff', fontSize: '12px',
    display: 'flex', alignItems: 'center', justifyContent: 'center',
    boxShadow: '0 2px 8px rgba(74,144,217,0.3)',
    animation: 'checkPop 0.35s cubic-bezier(0.175,0.885,0.32,1.275) both',
  },
  providerIcon: {
    fontSize: '32px',
    transition: 'transform 0.3s cubic-bezier(0.4,0,0.2,1)',
  },
  providerName: {
    fontSize: '14px', fontWeight: '600' as const,
    color: 'var(--text)', textAlign: 'center' as const,
    letterSpacing: '0.2px',
  },
  providerModel: {
    fontSize: '11px', color: 'var(--text-secondary)',
    textAlign: 'center' as const,
    whiteSpace: 'nowrap' as const, overflow: 'hidden',
    textOverflow: 'ellipsis', maxWidth: '100%',
  },

  /* ── 表单视图 ── */
  formOverview: {
    display: 'flex', alignItems: 'center', gap: '16px',
    padding: '20px', marginBottom: '20px',
    borderRadius: '16px',
    background: 'linear-gradient(135deg, var(--primary) 0%, var(--primary-light) 100%)',
    boxShadow: '0 4px 20px rgba(30,58,95,0.15)',
    color: '#fff',
    animation: 'slideUp 0.35s cubic-bezier(0.4,0,0.2,1) both',
  },
  formIcon: {
    fontSize: '40px', width: '56px', height: '56px',
    display: 'flex', alignItems: 'center', justifyContent: 'center',
    borderRadius: '16px',
    background: 'rgba(255,255,255,0.12)',
    backdropFilter: 'blur(8px)',
  },
  formOverviewName: {
    fontSize: '18px', fontWeight: '600' as const,
    letterSpacing: '0.3px',
  },
  formOverviewModel: {
    fontSize: '13px', opacity: 0.75, marginTop: '2px',
  },

  formCard: {
    background: 'var(--card)', borderRadius: '16px',
    overflow: 'hidden',
    boxShadow: '0 1px 8px rgba(0,0,0,0.06)',
    animation: 'slideUp 0.4s cubic-bezier(0.4,0,0.2,1) 0.08s both',
  },
  formRow: {
    display: 'flex', alignItems: 'center',
    justifyContent: 'space-between',
    padding: '14px 18px',
    borderBottom: '1px solid var(--border)',
    transition: 'background 0.25s ease',
  },
  formLabel: {
    fontSize: '14px', color: 'var(--text-secondary)',
    fontWeight: '500' as const, flexShrink: 0 as const,
    minWidth: '72px',
  },
  formInput: {
    flex: 1, textAlign: 'right' as const,
    border: 'none', outline: 'none',
    background: 'transparent',
    fontSize: '14px', color: 'var(--text)',
    fontWeight: '500' as const,
    transition: 'all 0.25s ease',
  },
  formSelect: {
    flex: 1, textAlign: 'right' as const,
    border: 'none', outline: 'none',
    background: 'transparent',
    fontSize: '14px', color: 'var(--text)',
    fontWeight: '500' as const,
    appearance: 'none' as const,
    WebkitAppearance: 'none' as const,
    cursor: 'pointer',
    transition: 'all 0.25s ease',
  },
  sliderRow: {
    padding: '14px 18px',
    animation: 'slideUp 0.4s cubic-bezier(0.4,0,0.2,1) 0.2s both',
  },
  sliderHeader: {
    display: 'flex', justifyContent: 'space-between',
    alignItems: 'center', marginBottom: '10px',
  },
  slider: {
    width: '100%', height: '6px',
    appearance: 'none' as const,
    WebkitAppearance: 'none' as const,
    borderRadius: '3px',
    background: 'linear-gradient(90deg, var(--accent) 0%, var(--border) 0%)',
    outline: 'none',
    transition: 'all 0.25s ease',
  },

  saveBtn: (saved: boolean) => ({
    width: '100%', padding: '16px',
    border: 'none', borderRadius: '14px',
    background: saved
      ? 'linear-gradient(135deg, #22c55e 0%, #16a34a 100%)'
      : 'linear-gradient(135deg, var(--accent) 0%, var(--primary-light) 100%)',
    color: '#fff', fontSize: '15px',
    fontWeight: '600' as const, letterSpacing: '0.5px',
    cursor: 'pointer',
    marginTop: '20px',
    boxShadow: saved
      ? '0 4px 16px rgba(34,197,94,0.3)'
      : '0 4px 16px rgba(74,144,217,0.3)',
    transition: 'all 0.4s cubic-bezier(0.4,0,0.2,1)',
    animation: 'slideUp 0.4s cubic-bezier(0.4,0,0.2,1) 0.28s both',
  }),
}

/* ─── 内联 keyframes（通过 style 标签注入） ─── */
const KEYFRAMES = `
@keyframes cardFadeIn {
  from { opacity: 0; transform: translateY(12px); }
  to   { opacity: 1; transform: translateY(0); }
}
@keyframes slideUp {
  from { opacity: 0; transform: translateY(16px); }
  to   { opacity: 1; transform: translateY(0); }
}
@keyframes checkPop {
  from { opacity: 0; transform: scale(0.3); }
  to   { opacity: 1; transform: scale(1); }
}
@keyframes viewSwitchIn {
  from { opacity: 0; transform: translateX(30px); }
  to   { opacity: 1; transform: translateX(0); }
}
@keyframes viewSwitchOut {
  from { opacity: 1; transform: translateX(0); }
  to   { opacity: 0; transform: translateX(-30px); }
}
@keyframes pulse {
  0%, 100% { transform: scale(1); }
  50%      { transform: scale(1.05); }
}
`

/* ─── 组件 ─── */
export default function ProviderPage() {
  const navigate = useNavigate()
  const { model, saveModel } = useModel()
  const [showForm, setShowForm] = useState(false)
  const [saved, setSaved] = useState(false)
  const [form, setForm] = useState({
    provider: '' as ProviderId,
    model: '',
    apiKey: '',
    apiHost: '',
    temperature: 0.7,
    maxTokens: 4096,
  })

  const activeProvider = PROVIDERS.find(p => p.id === model.provider)

  /* ── 选择提供商 ── */
  const handleSelectProvider = (providerId: ProviderId) => {
    const p = PROVIDERS.find(x => x.id === providerId)!
    setForm({
      provider: p.id,
      model: p.models[0],
      apiKey: '',
      apiHost: p.host,
      temperature: 0.7,
      maxTokens: 4096,
    })
    setTimeout(() => setShowForm(true), 120)
  }

  /* ── 保存 ── */
  const handleSave = () => {
    saveModel({
      provider: form.provider,
      model: form.model,
      apiKey: form.apiKey,
      apiHost: form.apiHost,
      temperature: form.temperature,
      maxTokens: form.maxTokens,
    })
    setSaved(true)
    setTimeout(() => setSaved(false), 2000)
  }

  /* ═══ 网格视图 ═══ */
  const GridView = () => (
    <>
      {/* 当前使用中 */}
      {activeProvider && (
        <div style={css.activeCard}>
          <div style={css.activeIcon}>{activeProvider.icon}</div>
          <div style={css.activeInfo}>
            <div style={css.activeName}>{activeProvider.name}</div>
            <div style={css.activeModel}>{model.model}</div>
          </div>
          <div style={css.activeBadge}>使用中</div>
        </div>
      )}

      {/* 分类网格 */}
      {CATEGORY_ORDER.map((cat, catIdx) => {
        const items = PROVIDERS.filter(p => p.category === cat)
        if (!items.length) return null
        return (
          <div key={cat} style={{ marginBottom: '24px' }}>
            <div style={css.categoryTitle}>{CATEGORY_LABELS[cat]}</div>
            <div style={css.grid}>
              {items.map((p, idx) => {
                const isActive = model.provider === p.id
                return (
                  <button
                    key={p.id}
                    style={{
                      ...css.providerCard(isActive),
                      animationDelay: `${(catIdx * items.length + idx) * 0.06}s`,
                    }}
                    onClick={() => handleSelectProvider(p.id)}
                    onMouseDown={e => {
                      e.currentTarget.style.transform = 'scale(0.95)'
                    }}
                    onMouseUp={e => {
                      e.currentTarget.style.transform = ''
                    }}
                    onMouseLeave={e => {
                      e.currentTarget.style.transform = ''
                    }}
                  >
                    {isActive && <div style={css.checkMark}>✓</div>}
                    <span style={css.providerIcon}>{p.icon}</span>
                    <span style={css.providerName}>{p.name}</span>
                    <span style={css.providerModel}>
                      {p.models[0]}
                    </span>
                  </button>
                )
              })}
            </div>
          </div>
        )
      })}
    </>
  )

  /* ═══ 表单视图 ═══ */
  const FormView = () => {
    const p = PROVIDERS.find(x => x.id === form.provider)!
    const pct = Math.round((form.temperature / 2) * 100)
    return (
      <>
        {/* 概览卡片 */}
        <div style={css.formOverview}>
          <div style={css.formIcon}>{p.icon}</div>
          <div>
            <div style={css.formOverviewName}>{p.name}</div>
            <div style={css.formOverviewModel}>{form.model}</div>
          </div>
        </div>

        {/* 表单卡片 */}
        <div style={css.formCard}>
          {/* 模型 */}
          <div style={css.formRow}>
            <span style={css.formLabel}>模型</span>
            <select
              style={css.formSelect}
              value={form.model}
              onChange={e => setForm(f => ({ ...f, model: e.target.value }))}
            >
              {p.models.map(m => (
                <option key={m} value={m}>{m}</option>
              ))}
            </select>
          </div>

          {/* API Key */}
          <div style={css.formRow}>
            <span style={css.formLabel}>API Key</span>
            <input
              type="password"
              style={css.formInput}
              placeholder="输入密钥"
              value={form.apiKey}
              onChange={e => setForm(f => ({ ...f, apiKey: e.target.value }))}
            />
          </div>

          {/* API 地址 */}
          <div style={css.formRow}>
            <span style={css.formLabel}>API 地址</span>
            <input
              type="text"
              style={css.formInput}
              placeholder={p.host || '自定义地址'}
              value={form.apiHost}
              onChange={e => setForm(f => ({ ...f, apiHost: e.target.value }))}
            />
          </div>

          {/* Temperature */}
          <div style={css.sliderRow}>
            <div style={css.sliderHeader}>
              <span style={css.formLabel}>Temperature</span>
              <span style={{
                fontSize: '13px', fontWeight: '600', color: 'var(--accent)',
                background: 'var(--accent-glow)',
                padding: '3px 10px', borderRadius: '20px',
                transition: 'all 0.3s ease',
              }}>
                {form.temperature.toFixed(1)}
              </span>
            </div>
            <input
              type="range"
              min="0" max="2" step="0.1"
              style={{
                ...css.slider,
                background: `linear-gradient(90deg, var(--accent) 0%, var(--accent) ${pct}%, var(--border) ${pct}%, var(--border) 100%)`,
              }}
              value={form.temperature}
              onChange={e => setForm(f => ({ ...f, temperature: parseFloat(e.target.value) }))}
            />
          </div>
        </div>

        {/* 保存按钮 */}
        <button style={css.saveBtn(saved)} onClick={handleSave}>
          {saved ? '✓ 已保存' : '保存配置'}
        </button>
      </>
    )
  }

  /* ═══ 主渲染 ═══ */
  return (
    <div style={css.page}>
      <style>{KEYFRAMES}</style>

      <div style={css.header}>
        <button style={css.backBtn} onClick={() => showForm ? setShowForm(false) : navigate(-1)}>
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
            <path d="M15 18l-6-6 6-6"/>
          </svg>
        </button>
        <span style={css.headerTitle}>
          {showForm ? `配置 ${PROVIDERS.find(x => x.id === form.provider)?.name ?? ''}` : '模型提供商'}
        </span>
      </div>

      <div style={css.body}>
        {showForm ? <FormView /> : <GridView />}
      </div>
    </div>
  )
}
