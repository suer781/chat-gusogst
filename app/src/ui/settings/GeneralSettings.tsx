import React, { useState } from 'react'
import { useSettingsStore } from '../stores'

interface Props { onBack: () => void }

const THEMES = [
  { value: 'dark', label: '深色', icon: '🌙' },
  { value: 'light', label: '浅色', icon: '☀️' },
  { value: 'auto', label: '跟随系统', icon: '📱' },
]
const LANGUAGES = [
  { value: 'zh-CN', label: '简体中文' },
  { value: 'zh-TW', label: '繁體中文' },
  { value: 'en', label: 'English' },
  { value: 'ja', label: '日本語' },
]
const FONT_SIZES = [
  { value: 13, label: '小' },
  { value: 14, label: '标准' },
  { value: 16, label: '大' },
  { value: 18, label: '特大' },
]

export default function GeneralSettings({ onBack }: Props) {
  const { theme, fontSize, language, setTheme, setFontSize, setLanguage } = useSettingsStore()
  const [currentTheme, setCurrentTheme] = useState(theme)
  const [currentFontSize, setCurrentFontSize] = useState(fontSize)
  const [currentLanguage, setCurrentLanguage] = useState(language)
  const [saved, setSaved] = useState(false)

  const handleSave = () => {
    setTheme(currentTheme)
    setFontSize(currentFontSize)
    setLanguage(currentLanguage)
    setSaved(true)
    setTimeout(() => setSaved(false), 1500)
  }

  return (
    <div className="settings-container" style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      <div className="settings-header">
        <button className="back-btn" onClick={onBack}>←</button>
        <h2>⚙️ 常规设置</h2>
        <button className="save-btn" onClick={handleSave}>{saved ? '✓ 已保存' : '保存'}</button>
      </div>
      <div className="settings-content">
        <div className="setting-section">
          <label>主题</label>
          <div className="setting-options">
            {THEMES.map((t) => (
              <button key={t.value} className={currentTheme === t.value ? 'active' : ''} onClick={() => setCurrentTheme(t.value as typeof currentTheme)}>{t.icon} {t.label}</button>
            ))}
          </div>
        </div>
        <div className="setting-section">
          <label>语言</label>
          <div className="setting-options">
            {LANGUAGES.map((l) => (
              <button key={l.value} className={currentLanguage === l.value ? 'active' : ''} onClick={() => setCurrentLanguage(l.value as typeof currentLanguage)}>{l.label}</button>
            ))}
          </div>
        </div>
        <div className="setting-section">
          <label>字体大小: {currentFontSize}px</label>
          <div className="setting-options">
            {FONT_SIZES.map((f) => (
              <button key={f.value} className={Number(currentFontSize) === Number(f.value) ? 'active' : ''} onClick={() => setCurrentFontSize(f.value as any)}>{f.label}</button>
            ))}
          </div>
        </div>
      </div>
    </div>
  )
}
