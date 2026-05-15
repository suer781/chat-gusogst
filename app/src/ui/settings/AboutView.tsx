import React from 'react'

interface Props { onBack: () => void }

const OSS_PROJECTS = [
  { name: 'Chatbox', desc: '跨平台 AI 聊天客户端 UI', url: 'github.com/chatboxai/chatbox', license: 'MIT' },
  { name: 'Hermes Agent', desc: 'NousResearch 出品的 Agent 框架', url: 'github.com/NousResearch/hermes-agent', license: 'Apache-2.0' },
  { name: 'React', desc: '用户界面构建库', url: 'github.com/facebook/react', license: 'MIT' },
  { name: 'Zustand', desc: '轻量级状态管理', url: 'github.com/pmndrs/zustand', license: 'MIT' },
  { name: 'Capacitor', desc: '跨平台原生应用运行时', url: 'github.com/ionic-team/capacitor', license: 'MIT' },
  { name: 'react-markdown', desc: 'Markdown 渲染组件', url: 'github.com/remarkjs/react-markdown', license: 'MIT' },
  { name: 'lucide-react', desc: '图标库', url: 'github.com/lucide-icons/lucide', license: 'ISC' },
  { name: 'framer-motion', desc: '动画引擎', url: 'github.com/framer/motion', license: 'MIT' },
  { name: 'Zod', desc: 'TypeScript 优先的数据校验', url: 'github.com/colinhacks/zod', license: 'MIT' },
]

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
            <span className="tech-tag">Capacitor</span>
            <span className="tech-tag">Python</span>
            <span className="tech-tag">Hermes Agent</span>
            <span className="tech-tag">MCP</span>
          </div>
        </div>

        <div className="setting-section">
          <label>🙏 开源致谢</label>
          <div className="about-credits">
            {OSS_PROJECTS.map(p => (
              <div key={p.name} className="credit-item">
                <div className="credit-main">
                  <strong>{p.name}</strong>
                  <span className="credit-license">{p.license}</span>
                </div>
                <div className="credit-desc">{p.desc}</div>
                <div className="credit-url">{p.url}</div>
              </div>
            ))}
          </div>
        </div>

        <div className="setting-section">
          <label>项目地址</label>
          <p className="about-url" style={{ fontFamily: 'monospace' }}>github.com/suer781/chat-gusogst</p>
        </div>

        <div className="about-footer">
          <p>本项目遵循各开源组件的许可协议，感谢所有贡献者。</p>
        </div>
      </div>
    </div>
  )
}
