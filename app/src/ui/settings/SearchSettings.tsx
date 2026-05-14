import React, { useState } from 'react'
import { useSettingsStore } from '../stores'

interface Props { onBack: () => void }

const ENGINES = [
  { value: 'duckduckgo', label: 'DuckDuckGo', desc: '免费，无需 API Key' },
  { value: 'tavily', label: 'Tavily', desc: '需 API Key，结果更精准' },
  { value: 'baidu', label: '百度搜索', desc: '国内搜索，免费' },
]

export default function SearchSettings({ onBack }: Props) {
  const { config, updateConfig } = useSettingsStore()
  const [enabled, setEnabled] = useState(config.searchEnabled)
  const [engine, setEngine] = useState(config.searchEngine)
  const [apiKey, setApiKey] = useState(config.searchApiKey || '')
  const [saved, setSaved] = useState(false)

  const handleSave = () => {
    updateConfig({ searchEnabled: enabled, searchEngine: engine, searchApiKey: apiKey })
    setSaved(true)
    setTimeout(() => setSaved(false), 1500)
  }

  return (
    <div className="settings-container" style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      <div className="settings-header">
        <button className="back-btn" onClick={onBack}>←</button>
        <h2>🔍 联网搜索</h2>
        <button className="save-btn" onClick={handleSave}>{saved ? '✓ 已保存' : '保存'}</button>
      </div>
      <div className="settings-content">
        <div className="setting-section">
          <div className="setting-row">
            <label>启用联网搜索</label>
            <button className={`toggle-btn ${enabled ? 'on' : 'off'}`} onClick={() => setEnabled(!enabled)}>{enabled ? 'ON' : 'OFF'}</button>
          </div>
          <p className="setting-hint">开启后 AI 可以搜索互联网获取实时信息</p>
        </div>
        {enabled && (<>
          <div className="setting-section">
            <label>搜索引擎</label>
            <div className="setting-options">
              {ENGINES.map((e) => (
                <button key={e.value} className={engine === e.value ? 'active' : ''} onClick={() => setEngine(e.value as typeof engine)}>{e.label}</button>
              ))}
            </div>
            <p className="setting-hint">{ENGINES.find((e) => e.value === engine)?.desc}</p>
          </div>
          {engine === 'tavily' && (
            <div className="setting-section">
              <label>Tavily API Key</label>
              <input className="setting-input" type="password" placeholder="tvly-..." value={apiKey} onChange={(e) => setApiKey(e.target.value)} />
              <p className="setting-hint">在 tavily.com 免费注册获取</p>
            </div>
          )}
        </>)}
      </div>
    </div>
  )
}
