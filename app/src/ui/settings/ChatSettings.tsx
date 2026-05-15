import React, { useState } from 'react'
import { useSettingsStore } from '../stores'

interface Props { onBack: () => void }

export default function ChatSettings({ onBack }: Props) {
  const { config, updateConfig } = useSettingsStore()
  const [maxRounds, setMaxRounds] = useState(config.maxRounds)
  const [maxHistoryTokens, setMaxHistoryTokens] = useState((config.maxHistoryTokens ?? 10000))
  const [memoryEnabled, setMemoryEnabled] = useState(config.memoryEnabled)
  const [autoSave, setAutoSave] = useState(true)
  const [saved, setSaved] = useState(false)

  const handleSave = () => {
    updateConfig({ maxRounds, maxHistoryTokens, memoryEnabled })
    localStorage.setItem('chat-gusogst-autosave', String(autoSave))
    setSaved(true)
    setTimeout(() => setSaved(false), 1500)
  }

  return (
    <div className="settings-container" style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      <div className="settings-header">
        <button className="back-btn" onClick={onBack}>←</button>
        <h2>💬 对话设置</h2>
        <button className="save-btn" onClick={handleSave}>{saved ? '✓ 已保存' : '保存'}</button>
      </div>
      <div className="settings-content">
        <div className="setting-section">
          <div className="setting-row">
            <label>自动保存对话</label>
            <button className={`toggle-btn ${autoSave ? 'on' : 'off'}`} onClick={() => setAutoSave(!autoSave)}>{autoSave ? 'ON' : 'OFF'}</button>
          </div>
          <p className="setting-hint">关闭后需手动保存对话记录</p>
        </div>
        <div className="setting-section">
          <div className="setting-row">
            <label>记忆系统</label>
            <button className={`toggle-btn ${memoryEnabled ? 'on' : 'off'}`} onClick={() => setMemoryEnabled(!memoryEnabled)}>{memoryEnabled ? 'ON' : 'OFF'}</button>
          </div>
          <p className="setting-hint">开启后 AI 会记住你的偏好和重要信息</p>
        </div>
        <div className="setting-section">
          <label>上下文轮次: {maxRounds} 轮</label>
          <input type="range" min={1} max={50} step={1} value={maxRounds} onChange={(e) => setMaxRounds(parseInt(e.target.value))} style={{ width: '100%' }} />
          <p className="setting-hint">AI 能记住的对话轮数，越大越消耗 Token</p>
        </div>
        <div className="setting-section">
          <label>历史 Token 上限</label>
          <input className="setting-input" type="number" min={1000} max={200000} step={1000} value={maxHistoryTokens} onChange={(e) => setMaxHistoryTokens(parseInt(e.target.value) || 8000)} />
          <p className="setting-hint">发送给模型的最大历史 Token 数（当前 {(maxHistoryTokens / 1000).toFixed(0)}K）</p>
        </div>
      </div>
    </div>
  )
}
