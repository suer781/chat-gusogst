import { useState, useCallback } from 'react'
import { useSettingsStore } from '../stores'
import { t } from '../i18n'
import { Bot, Key, Globe, Thermometer, Hash, Sparkles, Loader2 } from 'lucide-react'
import { light as hapticLight, medium as hapticMedium, success as hapticSuccess, error as hapticError, selectionStart, selectionChangedThrottled, selectionEnd, glassTap } from '../haptics'

const PROVIDERS = [
  { id: 'openai',    label: 'OpenAI',       placeholder: 'sk-...' },
  { id: 'anthropic', label: 'Anthropic',    placeholder: 'sk-ant-...' },
  { id: 'ollama',    label: 'Ollama',       placeholder: '' },
  { id: 'custom',    label: t('provider.custom'),        placeholder: 'API Key' },
]

export function ModelSettings({ onBack }: { onBack: () => void }) {
  const model = useSettingsStore((s) => s.model)
  const setModel = useSettingsStore((s) => s.setModel)
  const setApiKey = useSettingsStore((s) => s.setApiKey)
  const setBaseUrl = useSettingsStore((s) => s.setBaseUrl)
  const setApiHost = useSettingsStore((s) => s.setApiHost)
  const setTemperature = useSettingsStore((s) => s.setTemperature)
  const setMaxTokens = useSettingsStore((s) => s.setMaxTokens)
  const persona = useSettingsStore((s) => s.persona)
  const setPersona = useSettingsStore((s) => s.setPersona)
  const maxRounds = useSettingsStore((s) => s.maxRounds)
  const setMaxRounds = useSettingsStore((s) => s.setMaxRounds)
  const apiKey = useSettingsStore((s) => s.model.apiKey)
  const baseUrl = useSettingsStore((s) => s.model.baseUrl)
  const apiHost = useSettingsStore((s) => s.model.apiHost)

  const [analyzing, setAnalyzing] = useState(false)
  const [analyzeResult, setAnalyzeResult] = useState('')

  /** 自主理解：调模型分析系统提示词，自动推荐参数 */
  const autoUnderstand = useCallback(async () => {
    if (!apiKey) { setAnalyzeResult('请先填写 API Key'); return }
    setAnalyzing(true)
    setAnalyzeResult('')
    try {
      const host = (apiHost || baseUrl || 'https://api.openai.com').replace(/\/$/, '')
      const endpoint = host + '/v1/chat/completions'
      const systemPrompt = persona?.systemPrompt || ''

      const metaPrompt = `你是一个 AI 参数调优专家。根据以下系统提示词描述的角色性格和情绪特征，推荐最合适的模型参数。

系统提示词：
${systemPrompt || '（未设置自定义提示词，使用默认助手角色）'}

请分析这个角色的性格特征（如温柔、活泼、严谨、幽默等），然后推荐以下三个参数，并返回严格 JSON 格式：
{
  "temperature": 0.0-2.0 之间的浮点数（活泼/创意角色偏高，严谨/专业角色偏低）,
  "maxTokens": 256-16384 之间的整数（话痨角色偏大，简洁角色偏小）,
  "maxRounds": 3-50 之间的整数（需要深度对话的角色偏大，简单角色偏小）
}

只返回 JSON，不要任何其他文字。`

      const resp = await fetch(endpoint, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + apiKey },
        body: JSON.stringify({ model: model.model, messages: [{ role: 'user', content: metaPrompt }], temperature: 0.3, max_tokens: 256 }),
      })
      if (!resp.ok) throw new Error('API 请求失败: ' + resp.status)
      const data = await resp.json()
      const content = data.choices?.[0]?.message?.content || ''
      const jsonMatch = content.match(/\{[\s\S]*\}/)
      if (!jsonMatch) throw new Error('模型返回格式异常')

      const params = JSON.parse(jsonMatch[0])
      const newTemp = Math.max(0, Math.min(2, Number(params.temperature) || 0.7))
      const newTokens = Math.max(256, Math.min(16384, Math.round(Number(params.maxTokens) || 4096)))
      const newRounds = Math.max(3, Math.min(50, Math.round(Number(params.maxRounds) || 10)))

      setTemperature(newTemp)
      setMaxTokens(newTokens)
      setMaxRounds(newRounds)

      // 将推荐结果写入 persona.modelParamsConfig
      if (persona) {
        setPersona({ ...persona, modelParamsConfig: { temperature: newTemp, maxTokens: newTokens, maxRounds: newRounds, analyzedAt: new Date().toISOString() } })
      }

      hapticSuccess()
      setAnalyzeResult(`✅ 已调整: temperature=${newTemp}, maxTokens=${newTokens}, maxRounds=${newRounds}`)
    } catch (e: any) {
      hapticError()
      setAnalyzeResult('❌ ' + (e.message || '分析失败'))
    } finally {
      setAnalyzing(false)
    }
  }, [apiKey, baseUrl, apiHost, model.model, persona, setTemperature, setMaxTokens, setMaxRounds, setPersona])

  const provider = PROVIDERS.find((p) => p.id === model.provider) || PROVIDERS[3]

  return (
    <div className="flex-1 flex flex-col overflow-y-auto" style={{ minHeight: 0, background: 'var(--bg-primary)' }}>
      <Header title="AI 模型" onBack={onBack} />

      {/* Provider */}
      <Section title="模型提供商" icon={<Bot size={18} />}>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 8 }}>
          {PROVIDERS.map((p) => (
            <button key={p.id} onClick={() => { glassTap(); setModel(p.id, model.model) }} style={{
              padding: '12px 4px', borderRadius: "var(--radius-md)", cursor: 'pointer',
              background: model.provider === p.id ? 'var(--purple-soft)' : 'rgba(255,255,255,0.04)',
              border: model.provider === p.id ? '1.5px solid rgba(108, 92, 231, 0.5)' : '1.5px solid transparent',
              color: model.provider === p.id ? 'var(--purple)' : 'var(--gray-300)',
              fontSize: "var(--text-base)", fontWeight: model.provider === p.id ? 600 : 400,
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
          <span style={{ color: 'var(--gray-400)', fontSize: "var(--text-sm)", minWidth: 32 }}>精确</span>
          <input type="range" min={0} max={100}
            value={Math.round(model.temperature * 100)}
            onPointerDown={() => selectionStart()}
            onChange={(e) => { selectionChangedThrottled(); setTemperature(Number(e.target.value) / 100) }}
            onPointerUp={() => selectionEnd()}
            style={{ flex: 1, accentColor: 'var(--purple)', height: 4, touchAction: 'pan-y' }} />
          <span style={{ color: 'var(--gray-400)', fontSize: "var(--text-sm)", minWidth: 32, textAlign: 'right' }}>随机</span>
          <span style={{ color: 'var(--gray-300)', fontSize: "var(--text-base)", fontWeight: 600, minWidth: 32, textAlign: 'right' }}>
            {model.temperature.toFixed(2)}
          </span>
        </div>
      </Section>

      {/* Max Tokens */}
      <Section title="最大 Token" icon={<Hash size={18} />}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          {[1024, 2048, 4096, 8192, 16384].map((t) => (
            <button key={t} onClick={() => { glassTap(); setMaxTokens(t) }} style={{
              padding: '8px 12px', borderRadius: "var(--radius-md)", cursor: 'pointer',
              background: model.maxTokens === t ? 'var(--purple-soft)' : 'rgba(255,255,255,0.04)',
              border: model.maxTokens === t ? '1px solid rgba(108,92,231,0.4)' : '1px solid transparent',
              color: model.maxTokens === t ? 'var(--purple)' : 'var(--gray-400)',
              fontSize: "var(--text-sm)", fontWeight: model.maxTokens === t ? 600 : 400,
              transition: 'all 0.2s',
            }}>{t >= 1024 ? `${t / 1024}K` : t}</button>
          ))}
        </div>
      </Section>

      {/* 自主理解 */}
      <Section title={t('settings.model.autoUnderstand') || '自主理解'} icon={<Sparkles size={18} />}>
        <p style={{ color: 'var(--gray-400)', fontSize: 'var(--text-sm)', margin: '0 0 12px', lineHeight: 1.5 }}>
          {t('settings.model.autoUnderstandDesc') || '根据当前系统提示词描述的性格情绪，调用模型自动推荐最佳参数'}
        </p>
        <button onClick={() => { hapticMedium(); autoUnderstand() }} disabled={analyzing} style={{
          display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8,
          width: '100%', padding: '12px', borderRadius: 'var(--radius-md)', cursor: analyzing ? 'wait' : 'pointer',
          background: analyzing ? 'var(--purple-soft)' : 'linear-gradient(135deg, var(--purple), #6c5ce7)',
          border: 'none', color: '#fff', fontSize: 'var(--text-base)', fontWeight: 600,
          opacity: analyzing ? 0.7 : 1, transition: 'all 0.2s',
        }}>
          {analyzing ? <Loader2 size={16} style={{ animation: 'spin 1s linear infinite' }} /> : <Sparkles size={16} />}
          {analyzing ? t('settings.model.analyzing') : t('settings.model.startAnalyze')}
        </button>
        {analyzeResult && (
          <p style={{ color: analyzeResult.startsWith('✅') ? '#00b894' : '#ff6b6b', fontSize: 'var(--text-sm)', margin: '10px 0 0', lineHeight: 1.5 }}>
            {analyzeResult}
          </p>
        )}
      </Section>
    </div>
  )
}

/* ── Shared ── */
function Header({ title, onBack }: { title: string; onBack: () => void }) {
  return (
    <div style={{
      display: 'flex', alignItems: 'center', gap: 12, padding: '16px 20px', position: 'sticky', top: 0,
      background: 'var(--bg-overlay)', backdropFilter: 'blur(20px)', zIndex: 10,
      borderBottom: '1px solid var(--divider)',
    }}>
      <button onClick={() => { glassTap(); onBack() }} style={{
        background: 'none', border: 'none', color: 'var(--accent)', fontSize: "var(--text-2xl)", cursor: 'pointer', padding: 4,
        display: 'flex', alignItems: 'center',
      }}>{'<-'}</button>
      <span style={{ fontSize: "var(--text-xl)", fontWeight: 600, color: 'var(--text-primary)' }}>{title}</span>
    </div>
  )
}

function Section({ title, icon, children }: { title: string; icon?: React.ReactNode; children: React.ReactNode }) {
  return (
    <div style={{ margin: '16px 16px 0', background: 'rgba(255,255,255,0.03)', borderRadius: "var(--radius-lg)", padding: '18px 16px', border: '1px solid rgba(255,255,255,0.05)' }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 14, color: 'var(--text-primary)', fontSize: "var(--text-base)", fontWeight: 600 }}>
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
        width: '100%', padding: '10px 14px', borderRadius: "var(--radius-md)",
        background: 'var(--divider)', border: '1px solid var(--border-color)',
        color: 'var(--gray-50)', fontSize: "var(--text-base)", outline: 'none',
        boxSizing: 'border-box',
      }} />
  )
}
