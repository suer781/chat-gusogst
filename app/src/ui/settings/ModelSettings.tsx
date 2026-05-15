import React, { useState } from 'react'
import { useSettingsStore } from '../stores'

interface Props { onBack: () => void }

const PROVIDER_GROUPS = [
  {
    label: '☁️ 云服务商',
    providers: [
      { id: 'openai', name: 'OpenAI', desc: 'GPT-4o / o1 / o3' },
      { id: 'anthropic', name: 'Anthropic', desc: 'Claude 3.5/4' },
      { id: 'google', name: 'Google AI', desc: 'Gemini 2.5' },
      { id: 'deepseek', name: 'DeepSeek', desc: 'V3 / R1' },
      { id: 'moonshot', name: 'Moonshot', desc: 'Kimi' },
      { id: 'zhipu', name: '智谱 AI', desc: 'GLM-4' },
      { id: 'baidu_ai', name: '百度千帆', desc: '文心 4.5' },
      { id: 'alibaba', name: '阿里通义', desc: 'Qwen3' },
      { id: 'bytedance', name: '字节豆包', desc: 'Doubao' },
      { id: 'minimax', name: 'MiniMax', desc: 'M1' },
      { id: 'yi', name: '零一万物', desc: 'Yi' },
    ],
  },
  {
    label: '🏢 第三方',
    providers: [
      { id: 'groq', name: 'Groq', desc: '超快推理' },
      { id: 'together', name: 'Together', desc: '开源托管' },
      { id: 'openrouter', name: 'OpenRouter', desc: '200+ 模型' },
      { id: 'fireworks', name: 'Fireworks', desc: '快速推理' },
      { id: 'siliconflow', name: 'SiliconFlow', desc: '硅基流动' },
      { id: 'volcengine', name: '火山引擎', desc: '字节云' },
    ],
  },
  {
    label: '🏠 本地',
    providers: [
      { id: 'ollama', name: 'Ollama', desc: '本地 LLM' },
      { id: 'lmstudio', name: 'LM Studio', desc: '本地 GUI' },
      { id: 'llamacpp', name: 'llama.cpp', desc: '高性能' },
      { id: 'custom', name: '自定义', desc: '兼容 API' },
    ],
  },
]

const MODEL_PRESETS: Record<string, { name: string; tokens?: string }[]> = {
  openai: [
    { name: 'gpt-4o', tokens: '16K' },
    { name: 'gpt-4o-mini', tokens: '16K' },
    { name: 'o1', tokens: '100K' },
    { name: 'o3', tokens: '100K' },
    { name: 'o4-mini', tokens: '100K' },
  ],
  anthropic: [
    { name: 'claude-sonnet-4-20250514', tokens: '16K' },
    { name: 'claude-opus-4-20250514', tokens: '16K' },
    { name: 'claude-3-5-sonnet-20241022', tokens: '8K' },
    { name: 'claude-3-5-haiku-20241022', tokens: '8K' },
  ],
  google: [
    { name: 'gemini-2.5-pro', tokens: '65K' },
    { name: 'gemini-2.5-flash', tokens: '65K' },
  ],
  deepseek: [
    { name: 'deepseek-chat', tokens: '8K' },
    { name: 'deepseek-reasoner', tokens: '8K' },
  ],
  moonshot: [
    { name: 'moonshot-v1-128k', tokens: '8K' },
    { name: 'kimi-latest', tokens: '8K' },
  ],
  zhipu: [
    { name: 'glm-4-plus', tokens: '4K' },
    { name: 'glm-4-flash', tokens: '4K' },
  ],
  ollama: [
    { name: 'llama3.1', tokens: '4K' },
    { name: 'qwen2.5', tokens: '4K' },
  ],
}

const DEFAULT_ENDPOINTS: Record<string, string> = {
  openai: 'https://api.openai.com/v1',
  anthropic: 'https://api.anthropic.com',
  google: 'https://generativelanguage.googleapis.com/v1beta',
  deepseek: 'https://api.deepseek.com/v1',
  moonshot: 'https://api.moonshot.cn/v1',
  zhipu: 'https://open.bigmodel.cn/api/paas/v4',
  baidu_ai: 'https://aip.baidubce.com/rpc/2.0/ai_custom/v1',
  alibaba: 'https://dashscope.aliyuncs.com/compatible-mode/v1',
  bytedance: 'https://ark.cn-beijing.volces.com/api/v3',
  minimax: 'https://api.minimax.chat/v1',
  yi: 'https://api.lingyiwanwu.com/v1',
  groq: 'https://api.groq.com/openai/v1',
  together: 'https://api.together.xyz/v1',
  openrouter: 'https://openrouter.ai/api/v1',
  fireworks: 'https://api.fireworks.ai/inference/v1',
  siliconflow: 'https://api.siliconflow.cn/v1',
  volcengine: 'https://ark.cn-beijing.volces.com/api/v3',
  ollama: 'http://localhost:11434/v1',
  lmstudio: 'http://localhost:1234/v1',
  llamacpp: 'http://localhost:8080/v1',
  custom: '',
}

export default function ModelSettings({ onBack }: Props) {
  const { config, updateConfig } = useSettingsStore()
  const model = config.model
  const [expandedGroup, setExpandedGroup] = useState<string | null>('☁️ 云服务商')
  const [showEndpoint, setShowEndpoint] = useState(false)
  const [saved, setSaved] = useState(false)

  const handleProviderChange = (provider: string) => {
    const endpoint = DEFAULT_ENDPOINTS[provider] || ''
    const models = MODEL_PRESETS[provider]
    const modelName = models?.[0]?.name || ''
    updateConfig({
      model: { ...model, provider: provider as any, baseUrl: endpoint, model: modelName },
    })
  }

  const handleSave = () => {
    setSaved(true)
    setTimeout(() => setSaved(false), 1500)
  }

  const models = MODEL_PRESETS[model.provider] || []

  return (
    <div className="settings-container" style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      <div className="settings-header">
        <button className="back-btn" onClick={onBack}>←</button>
        <h2>🤖 模型设置</h2>
        <button className="save-btn" onClick={handleSave}>{saved ? '✓ 已保存' : '自动保存'}</button>
      </div>
      <div className="settings-content">
        {PROVIDER_GROUPS.map((group) => (
          <div key={group.label} className="setting-section">
            <label
              style={{ cursor: 'pointer' }}
              onClick={() => setExpandedGroup(expandedGroup === group.label ? null : group.label)}
            >
              {group.label} {expandedGroup === group.label ? '▲' : '▼'}
            </label>
            {expandedGroup === group.label && (
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: 8, marginTop: 8 }}>
                {group.providers.map((p) => (
                  <button
                    key={p.id}
                    className={model.provider === p.id ? 'active' : ''}
                    onClick={() => handleProviderChange(p.id)}
                    style={{ textAlign: 'left', padding: '8px 12px' }}
                  >
                    <div style={{ fontWeight: 600 }}>{p.name}</div>
                    <div style={{ fontSize: 11, opacity: 0.6 }}>{p.desc}</div>
                  </button>
                ))}
              </div>
            )}
          </div>
        ))}

        <div className="setting-section">
          <label>模型</label>
          {models.length > 0 ? (
            <div className="setting-options">
              {models.map((m) => (
                <button
                  key={m.name}
                  className={model.model === m.name ? 'active' : ''}
                  onClick={() => updateConfig({ model: { ...model, model: m.name } })}
                >
                  {m.name} {m.tokens && <span style={{ fontSize: 11, opacity: 0.5 }}>({m.tokens})</span>}
                </button>
              ))}
            </div>
          ) : (
            <input
              type="text"
              value={model.model}
              onChange={(e) => updateConfig({ model: { ...model, model: e.target.value } })}
              placeholder="输入模型名称"
              style={{ width: '100%', padding: '8px', borderRadius: 6, border: '1px solid var(--border-color)', background: 'var(--bg-secondary)', color: 'var(--text-primary)' }}
            />
          )}
        </div>

        <div className="setting-section">
          <label>🔑 API Key</label>
          <input
            type="password"
            value={model.apiKey}
            onChange={(e) => updateConfig({ model: { ...model, apiKey: e.target.value } })}
            placeholder="sk-..."
            style={{ width: '100%', padding: '8px', borderRadius: 6, border: '1px solid var(--border-color)', background: 'var(--bg-secondary)', color: 'var(--text-primary)' }}
          />
        </div>

        <div className="setting-section">
          <label style={{ cursor: 'pointer' }} onClick={() => setShowEndpoint(!showEndpoint)}>
            🌐 API 地址 {showEndpoint ? '▲' : '▼'}
          </label>
          {showEndpoint ? (
            <input
              type="text"
              value={model.baseUrl}
              onChange={(e) => updateConfig({ model: { ...model, baseUrl: e.target.value } })}
              placeholder="https://api.example.com/v1"
              style={{ width: '100%', padding: '8px', borderRadius: 6, border: '1px solid var(--border-color)', background: 'var(--bg-secondary)', color: 'var(--text-primary)' }}
            />
          ) : (
            <div style={{ fontSize: 12, opacity: 0.5, marginTop: 4 }}>
              {model.baseUrl || DEFAULT_ENDPOINTS[model.provider] || '点击展开设置'}
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
