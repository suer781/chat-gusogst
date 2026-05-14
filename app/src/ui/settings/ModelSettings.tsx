import React, { useState } from 'react'
import { useSettingsStore } from '../stores'

interface Props { onBack: () => void }

const PROVIDERS = [
  { value: 'openai', label: 'OpenAI 兼容' },
  { value: 'anthropic', label: 'Anthropic' },
]

const MODELS: Record<string, { value: string; label: string }[]> = {
  openai: [
    { value: 'gpt-4o', label: 'GPT-4o' },
    { value: 'gpt-4o-mini', label: 'GPT-4o Mini' },
    { value: 'gpt-4-turbo', label: 'GPT-4 Turbo' },
    { value: 'deepseek-chat', label: 'DeepSeek Chat' },
    { value: 'gemini-2.0-flash', label: 'Gemini 2.0 Flash' },
  ],
  anthropic: [
    { value: 'claude-sonnet-4-20250514', label: 'Claude Sonnet 4' },
    { value: 'claude-3-5-haiku-20241022', label: 'Claude 3.5 Haiku' },
  ],
}

const PLACEHOLDER_HOST: Record<string, string> = {
  openai: 'https://api.openai.com',
  anthropic: 'https://api.anthropic.com',
}

export default function ModelSettings({ onBack }: Props) {
  const { config, updateConfig } = useSettingsStore()

  const [provider, setProvider] = useState(config.model.provider)
  const [model, setModel] = useState(config.model.model)
  const [customModel, setCustomModel] = useState('')
  const [apiKey, setApiKey] = useState(config.model.apiKey)
  const [apiHost, setApiHost] = useState(config.model.apiHost || '')
  const [temperature, setTemperature] = useState(config.model.temperature ?? 0.7)
  const [maxTokens, setMaxTokens] = useState(config.model.maxTokens ?? 4096)
  const [saved, setSaved] = useState(false)

  const isCustom = !MODELS[provider]?.some((m) => m.value === model)

  const handleProviderChange = (p: string) => {
    setProvider(p)
    setModel(MODELS[p]?.[0]?.value || '')
    setCustomModel('')
  }

  const handleSave = () => {
    updateConfig({
      model: {
        ...config.model,
        provider,
        model: isCustom ? customModel || model : model,
        apiKey,
        apiHost,
        temperature,
        maxTokens,
      },
    })
    setSaved(true)
    setTimeout(() => setSaved(false), 1500)
  }

  return (
    <div className="settings-container" style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      <div className="settings-header">
        <button className="back-btn" onClick={onBack}>←</button>
        <h2>🤖 模型设置</h2>
        <button className="save-btn" onClick={handleSave}>
          {saved ? '✓ 已保存' : '保存'}
        </button>
      </div>
      <div className="settings-content">
        <div className="setting-section">
          <label>服务提供商</label>
          <div className="setting-options">
            {PROVIDERS.map((p) => (
              <button key={p.value} className={provider === p.value ? 'active' : ''} onClick={() => handleProviderChange(p.value)}>{p.label}</button>
            ))}
          </div>
        </div>
        <div className="setting-section">
          <label>模型</label>
          <div className="setting-options">
            {(MODELS[provider] || []).map((m) => (
              <button key={m.value} className={model === m.value && !isCustom ? 'active' : ''} onClick={() => { setModel(m.value); setCustomModel('') }}>{m.label}</button>
            ))}
            <button className={isCustom ? 'active' : ''} onClick={() => setModel('custom')}>自定义...</button>
          </div>
          {isCustom && (
            <input className="setting-input" placeholder="输入模型名称" value={customModel} onChange={(e) => setCustomModel(e.target.value)} />
          )}
        </div>
        <div className="setting-section">
          <label>API Key</label>
          <input className="setting-input" type="password" placeholder="sk-..." value={apiKey} onChange={(e) => setApiKey(e.target.value)} />
        </div>
        <div className="setting-section">
          <label>API 地址</label>
          <input className="setting-input" placeholder={PLACEHOLDER_HOST[provider] || 'https://api.openai.com'} value={apiHost} onChange={(e) => setApiHost(e.target.value)} />
        </div>
        <div className="setting-section">
          <label>温度: {temperature.toFixed(1)}</label>
          <input type="range" min={0} max={2} step={0.1} value={temperature} onChange={(e) => setTemperature(parseFloat(e.target.value))} style={{ width: '100%' }} />
        </div>
        <div className="setting-section">
          <label>最大 Token</label>
          <input className="setting-input" type="number" min={256} max={128000} value={maxTokens} onChange={(e) => setMaxTokens(parseInt(e.target.value) || 4096)} />
        </div>
      </div>
    </div>
  )
}
