import { useState } from 'react'
import { useSettingsStore } from '../stores'
import { ArrowLeft, Check } from 'lucide-react'

const PROVIDERS = [
  { value: 'openai', label: 'OpenAI 兼容', placeholder: 'https://api.openai.com' },
  { value: 'anthropic', label: 'Anthropic', placeholder: 'https://api.anthropic.com' },
]

const MODELS: Record<string, string[]> = {
  openai: ['gpt-4o', 'gpt-4o-mini', 'gpt-4-turbo', 'deepseek-chat', 'gemini-2.0-flash'],
  anthropic: ['claude-sonnet-4-20250514', 'claude-3-5-haiku-20241022'],
}

export function SettingsView({ onDone }: { onDone: () => void }) {
  const { config, updateConfig } = useSettingsStore()
  const [provider, setProvider] = useState(config.model.provider)
  const [model, setModel] = useState(config.model.model)
  const [apiKey, setApiKey] = useState(config.model.apiKey)
  const [apiHost, setApiHost] = useState(config.model.apiHost || '')
  const [temperature, setTemperature] = useState(config.model.temperature ?? 0.7)
  const [maxTokens, setMaxTokens] = useState(config.model.maxTokens ?? 4096)
  const [searchEnabled, setSearchEnabled] = useState(config.searchEnabled)
  const [searchEngine, setSearchEngine] = useState(config.searchEngine)
  const [searchApiKey, setSearchApiKey] = useState(config.searchApiKey || '')
  const [saved, setSaved] = useState(false)

  const handleSave = () => {
    updateConfig({
      model: { provider, model, apiKey, apiHost, temperature, maxTokens },
      searchEnabled,
      searchEngine: searchEngine as any,
      searchApiKey: searchApiKey || undefined,
    })
    setSaved(true)
    setTimeout(() => setSaved(false), 2000)
  }

  const providerInfo = PROVIDERS.find(p => p.value === provider)!

  return (
    <div className="settings-view">
      <div className="settings-header">
        <button onClick={onDone} className="icon-btn"><ArrowLeft size={20} /></button>
        <h2>设置</h2>
        <button onClick={handleSave} className="save-btn">
          {saved ? <><Check size={16} /> 已保存</> : '保存'}
        </button>
      </div>

      <div className="settings-body">
        {/* 模型设置 */}
        <section className="settings-section">
          <h3>🤖 模型</h3>
          <div className="form-group">
            <label>Provider</label>
            <select value={provider} onChange={e => { setProvider(e.target.value); setModel(MODELS[e.target.value]?.[0] || '') }}>
              {PROVIDERS.map(p => <option key={p.value} value={p.value}>{p.label}</option>)}
            </select>
          </div>
          <div className="form-group">
            <label>模型</label>
            <select value={model} onChange={e => setModel(e.target.value)}>
              {(MODELS[provider] || []).map(m => <option key={m} value={m}>{m}</option>)}
              <option value="custom">自定义...</option>
            </select>
            {model === 'custom' && (
              <input type="text" placeholder="输入模型名称" onChange={e => setModel(e.target.value)} />
            )}
          </div>
          <div className="form-group">
            <label>API Key</label>
            <input type="password" value={apiKey} onChange={e => setApiKey(e.target.value)} placeholder="sk-..." />
          </div>
          <div className="form-group">
            <label>API 地址</label>
            <input type="text" value={apiHost} onChange={e => setApiHost(e.target.value)} placeholder={providerInfo.placeholder} />
          </div>
          <div className="form-group">
            <label>温度: {temperature}</label>
            <input type="range" min="0" max="2" step="0.1" value={temperature} onChange={e => setTemperature(Number(e.target.value))} />
          </div>
          <div className="form-group">
            <label>最大 Token</label>
            <input type="number" value={maxTokens} onChange={e => setMaxTokens(Number(e.target.value))} />
          </div>
        </section>

        {/* 搜索设置 */}
        <section className="settings-section">
          <h3>🔍 联网搜索</h3>
          <div className="form-group">
            <label>
              <input type="checkbox" checked={searchEnabled} onChange={e => setSearchEnabled(e.target.checked)} />
              启用联网搜索
            </label>
          </div>
          {searchEnabled && (
            <>
              <div className="form-group">
                <label>搜索引擎</label>
                <select value={searchEngine} onChange={e => setSearchEngine(e.target.value as any)}>
                  <option value="duckduckgo">DuckDuckGo（免费）</option>
                  <option value="tavily">Tavily（需 API Key）</option>
                </select>
              </div>
              {searchEngine === 'tavily' && (
                <div className="form-group">
                  <label>Tavily API Key</label>
                  <input type="password" value={searchApiKey} onChange={e => setSearchApiKey(e.target.value)} placeholder="tvly-..." />
                </div>
              )}
            </>
          )}
        </section>
      </div>
    </div>
  )
}
