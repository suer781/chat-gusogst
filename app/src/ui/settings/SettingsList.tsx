import React from 'react'
import { useSettingsStore } from '../stores'

type SubPage = 'model' | 'search' | 'chat' | 'document' | 'general' | 'about'

interface Props {
  onNavigate: (page: SubPage) => void
}

/** 设置主页 — 菜单列表（仿 Chatbox 风格） */
export default function SettingsList({ onNavigate }: Props) {
  const { config } = useSettingsStore()

  const menuItems: { key: SubPage; icon: string; label: string; desc: string }[] = [
    {
      key: 'model',
      icon: '🤖',
      label: '模型提供方',
      desc: `${config.model.provider} / ${config.model.model}`,
    },
    {
      key: 'search',
      icon: '🔍',
      label: '联网搜索',
      desc: config.searchEnabled
        ? `已开启 · ${config.searchEngine === 'tavily' ? 'Tavily' : config.searchEngine === 'baidu' ? '百度' : 'DuckDuckGo'}`
        : '未开启',
    },
    {
      key: 'document',
      icon: '📄',
      label: '文档解析器',
      desc: 'PDF / Word / Excel 内容理解',
    },
    {
      key: 'chat',
      icon: '💬',
      label: '对话设置',
      desc: `上下文 ${config.maxRounds} 轮 · ${config.memoryEnabled ? '记忆已开' : '记忆已关'}`,
    },
    {
      key: 'general',
      icon: '⚙️',
      label: '常规设置',
      desc: '主题、语言、字体大小',
    },
    {
      key: 'about',
      icon: 'ℹ️',
      label: '关于',
      desc: 'v0.1.0',
    },
  ]

  return (
    <div className="settings-container" style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      <div className="settings-header">
        <h2>⚙️ 设置</h2>
      </div>

      <div className="settings-content">
        <div className="settings-menu">
          {menuItems.map((item) => (
            <button
              key={item.key}
              className="settings-menu-item"
              onClick={() => onNavigate(item.key)}
            >
              <span className="menu-icon">{item.icon}</span>
              <div className="menu-text">
                <span className="menu-label">{item.label}</span>
                <span className="menu-desc">{item.desc}</span>
              </div>
              <span className="menu-arrow">›</span>
            </button>
          ))}
        </div>
      </div>
    </div>
  )
}
