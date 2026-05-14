import React from 'react'

interface Props { onBack: () => void }

export default function AboutView({ onBack }: Props) {
  return (
    <div className="settings-container" style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      <div className="settings-header">
        <button className="back-btn" onClick={onBack}>←</button>
        <h2>ℹ️ 关于</h2>
      </div>
      <div className="settings-content">
        <div className="about-card">
          <div className="about-logo">💜</div>
          <h2>chat-gusogst</h2>
          <p className="about-version">v0.1.0</p>
          <p className="about-desc">AI 虚拟恋人 · 融合 Agent 能力<br />基于 Hermes Agent 框架</p>
        </div>
        <div className="setting-section">
          <label>技术栈</label>
          <div className="about-tech">
            <span className="tech-tag">React</span>
            <span className="tech-tag">TypeScript</span>
            <span className="tech-tag">Zustand</span>
            <span className="tech-tag">Python</span>
            <span className="tech-tag">Hermes Agent</span>
            <span className="tech-tag">MCP</span>
          </div>
        </div>
        <div className="setting-section">
          <label>开源组件</label>
          <p className="setting-hint">前端 UI 基于 Chatbox 项目<br />Agent 后端基于 NousResearch Hermes Agent</p>
        </div>
        <div className="setting-section">
          <label>项目地址</label>
          <p className="setting-hint" style={{ fontFamily: 'monospace' }}>github.com/suer781/chat-gusogst</p>
        </div>
      </div>
    </div>
  )
}
