import { useSettingsStore } from '../stores'
import { Bot, Key, Globe, Thermometer, Hash } from 'lucide-react'

const PROVIDERS = [
  { id: 'openai',    label: 'OpenAI',       placeholder: 'sk-...' },
  { id: 'anthropic', label: 'Anthropic',    placeholder: 'sk-ant-...' },
  { id: 'ollama',    label: 'Ollama',       placeholder: '' },
  { id: 'custom',    label: '自定义',        placeholder: 'API Key' },
]

export function ModelSettings({ onBack }: { onBack: () => void }) {
  const model = useSettingsStore((s) => s.model)
  const setModel = useSettingsStore((s) => s.setModel)
  const setApiKey = useSettingsStore((s) => s.setApiKey)
  const setBaseUrl = useSettingsStore((s) => s.setBaseUrl)
  const setApiHost = useSettingsStore((s) => s.setApiHost)
  const setTemperature = useSettingsStore((s) => s.setTemperature)
  const setMaxTokens = useSettingsStore((s) => s.setMaxTokens)

  const provider = PROVIDERS.find((p) => p.id === model.provider) || PROVIDERS[3]

  return (
    <div style={{ minHeight: '100%', background: '#0f0f23', padding: '0 0 100px' }}>
      <Header title="AI 模型" onBack={onBack} />

      {/* Provider */}
      <Section title="模型提供商" icon={<Bot size={18} />}>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 8 }}>
          {PROVIDERS.map((p) => (
            <button key={p.id} onClick={() => setModel(p.id, model.model)} style={{
              padding: '12px 4px', borderRadius: 12, cursor: 'pointer',
              background: model.provider === p.id ? 'rgba(108,92,231,0.15)' : 'rgba(255,255,255,0.04)',
              border: model.provider === p.id ? '1.5px solid rgba(108,92,231,0.5)' : '1.5px solid transparent',
              color: model.provider === p.id ? '#6C5CE7' : '#999',
              fontSize: 13, fontWeight: model.provider === p.id ? 600 : 400,
              transition: 'all 0.2s', textAlign: 'center',
            }}>{p.label}</button>
          ))}
        </div>
      </Section>

      {/* Model Name */}
      <Section title="模型名称">
        <InputField value={model.model} onChange={(v) => setModel(model.provider, v)}
          placeholder="gpt-4o / claude-3-opus / qwen2" />
      </Section>

      {/* API Key */}
      <Section title="API Key" icon={<Key size={18} />}>
        <InputField value={model.apiKey} onChange={setApiKey} type="password"
          placeholder={provider.placeholder || 'sk-xxx'} />
      </Section>

      {/* Base URL */}
      <Section title="API 地址" icon={<Globe size={18} />}>
        <InputField value={model.baseUrl} onChange={setBaseUrl}
          placeholder="https://api.openai.com/v1" />
        {model.provider === 'ollama' && (
          <div style={{ marginTop: 8 }}>
            <InputField value={model.apiHost} onChange={setApiHost}
              placeholder="http://localhost:11434" />
          </div>
        )}
      </Section>

      {/* Temperature */}
      <Section title="创意度" icon={<Thermometer size={18} />}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          <span style={{ color: '#666', fontSize: 12, minWidth: 32 }}>精确</span>
          <input type="range" min={0} max={100}
            value={Math.round(model.temperature * 100)}
            onChange={(e) => setTemperature(Number(e.target.value) / 100)}
            style={{ flex: 1, accentColor: '#6C5CE7', height: 4 }} />
          <span style={{ color: '#666', fontSize: 12, minWidth: 32, textAlign: 'right' }}>随机</span>
          <span style={{ color: '#999', fontSize: 14, fontWeight: 600, minWidth: 32, textAlign: 'right' }}>
            {model.temperature.toFixed(2)}
          </span>
        </div>
      </Section>

      {/* Max Tokens */}
      <Section title="最大 Token" icon={<Hash size={18} />}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          {[1024, 2048, 4096, 8192, 16384].map((t) => (
            <button key={t} onClick={() => setMaxTokens(t)} style={{
              padding: '8px 12px', borderRadius: 10, cursor: 'pointer',
              background: model.maxTokens === t ? 'rgba(108,92,231,0.15)' : 'rgba(255,255,255,0.04)',
              border: model.maxTokens === t ? '1px solid rgba(108,92,231,0.4)' : '1px solid transparent',
              color: model.maxTokens === t ? '#6C5CE7' : '#888',
              fontSize: 12, fontWeight: model.maxTokens === t ? 600 : 400,
              transition: 'all 0.2s',
            }}>{t >= 1024 ? `${t / 1024}K` : t}</button>
          ))}
        </div>
      </Section>
    </div>
  )
}

/* ── Shared ── */
function Header({ title, onBack }: { title: string; onBack: () => void }) {
  return (
    <div style={{
      display: 'flex', alignItems: 'center', gap: 12, padding: '16px 20px', position: 'sticky', top: 0,
      background: 'rgba(15,15,35,0.9)', backdropFilter: 'blur(20px)', zIndex: 10,
      borderBottom: '1px solid rgba(255,255,255,0.06)',
    }}>
      <button onClick={onBack} style={{
        background: 'none', border: 'none', color: '#e94560', fontSize: 20, cursor: 'pointer', padding: 4,
        display: 'flex', alignItems: 'center',
      }}>{'<-'}</button>
      <span style={{ fontSize: 18, fontWeight: 600, color: '#fff' }}>{title}</span>
    </div>
  )
}

function Section({ title, icon, children }: { title: string; icon?: React.ReactNode; children: React.ReactNode }) {
  return (
    <div style={{ margin: '16px 16px 0', background: 'rgba(255,255,255,0.03)', borderRadius: 16, padding: '18px 16px', border: '1px solid rgba(255,255,255,0.05)' }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 14, color: '#fff', fontSize: 14, fontWeight: 600 }}>
        {icon}<span>{title}</span>
      </div>
      {children}
    </div>
  )
}

function InputField({ value, onChange, placeholder, type }: {
  value: string; onChange: (v: string) => void; placeholder?: string; type?: string
}) {
  return (
    <input type={type || 'text'} value={value} onChange={(e) => onChange(e.target.value)}
      placeholder={placeholder} style={{
        width: '100%', padding: '10px 14px', borderRadius: 10,
        background: 'rgba(255,255,255,0.06)', border: '1px solid rgba(255,255,255,0.08)',
        color: '#eee', fontSize: 13, outline: 'none',
        boxSizing: 'border-box',
      }} />
  )
}
