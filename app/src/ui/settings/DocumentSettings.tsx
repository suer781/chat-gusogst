import React, { useState } from 'react'

interface Props { onBack: () => void }

const FORMATS = [
  { key: 'pdf', label: 'PDF', icon: '📕' },
  { key: 'docx', label: 'Word (.docx)', icon: '📘' },
  { key: 'xlsx', label: 'Excel (.xlsx)', icon: '📗' },
  { key: 'txt', label: '纯文本 (.txt)', icon: '📄' },
  { key: 'md', label: 'Markdown (.md)', icon: '📝' },
  { key: 'csv', label: 'CSV', icon: '📊' },
]

export default function DocumentSettings({ onBack }: Props) {
  const [enabled, setEnabled] = useState(() => {
    const v = localStorage.getItem('chat-gusogst-docparser')
    return v !== 'false'
  })
  const [formats, setFormats] = useState<string[]>(() => {
    const v = localStorage.getItem('chat-gusogst-docformats')
    return v ? JSON.parse(v) : FORMATS.map((f) => f.key)
  })
  const [saved, setSaved] = useState(false)

  const toggleFormat = (key: string) => {
    setFormats((prev) => prev.includes(key) ? prev.filter((f) => f !== key) : [...prev, key])
  }

  const handleSave = () => {
    localStorage.setItem('chat-gusogst-docparser', String(enabled))
    localStorage.setItem('chat-gusogst-docformats', JSON.stringify(formats))
    setSaved(true)
    setTimeout(() => setSaved(false), 1500)
  }

  return (
    <div className="settings-container" style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      <div className="settings-header">
        <button className="back-btn" onClick={onBack}>←</button>
        <h2>📄 文档解析器</h2>
        <button className="save-btn" onClick={handleSave}>{saved ? '✓ 已保存' : '保存'}</button>
      </div>
      <div className="settings-content">
        <div className="setting-section">
          <div className="setting-row">
            <label>启用文档解析</label>
            <button className={`toggle-btn ${enabled ? 'on' : 'off'}`} onClick={() => setEnabled(!enabled)}>{enabled ? 'ON' : 'OFF'}</button>
          </div>
          <p className="setting-hint">开启后可以发送文档让 AI 阅读和分析</p>
        </div>
        {enabled && (<>
          <div className="setting-section">
            <label>支持的文件格式</label>
            <div className="setting-options">
              {FORMATS.map((f) => (
                <button key={f.key} className={formats.includes(f.key) ? 'active' : ''} onClick={() => toggleFormat(f.key)}>{f.icon} {f.label}</button>
              ))}
            </div>
          </div>
          <div className="setting-section">
            <p className="setting-hint" style={{ lineHeight: 1.6 }}>
              💡 文档解析原理：<br />
              • 文本类（txt/md/csv）→ 直接读取内容<br />
              • PDF/Word/Excel → 提取文本后发送给 AI<br />
              • 单文件建议 50 页以内，过长会截断<br />
              • 表格类文件会转为 Markdown 格式
            </p>
          </div>
        </>)}
      </div>
    </div>
  )
}
