import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useModel } from '../hooks/useStore'
import type { ModelConfig } from '../lib/types'
import './SettingsPage.css'

export default function SettingsPage() {
  const navigate = useNavigate()
  const { model, saveModel } = useModel()
  const [form, setForm] = useState<ModelConfig>({ ...model })
  const [saved, setSaved] = useState(false)

  const update = <K extends keyof ModelConfig>(key: K, value: ModelConfig[K]) => {
    setForm(f => ({ ...f, [key]: value }))
    setSaved(false)
  }

  const save = () => { saveModel(form); setSaved(true) }

  return (
    <div className="settings-page">
      <header className="settings-header">
        <button className="back-btn" onClick={() => navigate(-1)}>←</button>
        <span>设置</span>
        <button className="save-btn" onClick={save}>{saved ? '✓ 已保存' : '保存'}</button>
      </header>
      <div className="settings-body">
        <section>
          <h3>🤖 模型配置</h3>
          <div className="provider-chips">
            <button className={'chip' + (form.provider === 'openai' ? ' active' : '')} onClick={() => update('provider', 'openai')}>OpenAI</button>
            <button className={'chip' + (form.provider === 'anthropic' ? ' active' : '')} onClick={() => update('provider', 'anthropic')}>Anthropic</button>
          </div>
          <label>模型名称</label>
          <input value={form.model} onChange={e => update('model', e.target.value)} />
          <label>API Key</label>
          <input type="password" value={form.apiKey} onChange={e => update('apiKey', e.target.value)} />
          <label>API Host</label>
          <input value={form.apiHost} onChange={e => update('apiHost', e.target.value)} />
          <label>Temperature: {form.temperature.toFixed(1)}</label>
          <input type="range" min="0" max="2" step="0.1" value={form.temperature} onChange={e => update('temperature', +e.target.value)} />
        </section>
      </div>
    </div>
  )
}