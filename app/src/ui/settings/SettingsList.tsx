import React from 'react'

interface Props { onNavigate: (page: string) => void }

const SECTIONS = [
  { label: '模型 & API', items: [
    { key: 'model', icon: '🤖', label: '模型配置', desc: '模型、API Key、温度、Token' },
  ]},
  { label: '对话 & 记忆', items: [
    { key: 'chat', icon: '💬', label: '对话设置', desc: '发送方式、上下文、自动命名' },
    { key: 'persona', icon: '🎭', label: '角色设定', desc: '人设管理、预设模板、切换设定' },
    { key: 'memory', icon: '🧠', label: '记忆库', desc: 'AI 记忆管理、搜索、标记' },
  ]},
  { label: '搜索 & 文档', items: [
    { key: 'search', icon: '🔍', label: '搜索设置', desc: '搜索引擎、安全搜索' },
    { key: 'document', icon: '📄', label: '文档设置', desc: '支持格式、OCR、解析模式' },
  ]},
  { label: '其他', items: [
    { key: 'general', icon: '⚙️', label: '通用设置', desc: '外观、语言、导出、重置' },
    { key: 'about', icon: 'ℹ️', label: '关于', desc: '版本、开源致谢、项目地址' },
  ]},
]

export default function SettingsList({ onNavigate }: Props) {
  return (
    <div className="settings-container" style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      <div className="settings-header">
        <h2>⚙️ 设置</h2>
      </div>
      <div className="settings-content">
        {SECTIONS.map((section) => (
          <div className="setting-section" key={section.label}>
            <label>{section.label}</label>
            {section.items.map((item) => (
              <div
                key={item.key}
                className="setting-item clickable"
                onClick={() => onNavigate(item.key)}
              >
                <div className="setting-info">
                  <span className="setting-icon">{item.icon}</span>
                  <div>
                    <div className="setting-label">{item.label}</div>
                    <div className="setting-hint">{item.desc}</div>
                  </div>
                </div>
                <span className="setting-arrow">›</span>
              </div>
            ))}
          </div>
        ))}
      </div>
    </div>
  )
}
