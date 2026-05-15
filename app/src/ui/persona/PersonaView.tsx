import React, { useState, useEffect } from 'react'
import { useSettingsStore } from '../stores'
import type { Persona, PersonaSearchConfig, PersonaSamplingConfig } from '../../shared/types'

interface Props { onDone: () => void }

const PRESETS: Record<string, Partial<Persona>> = {
  gentle: {
    name: '温柔女友',
    avatar: '🌸',
    systemPrompt: '你是一个温柔体贴的虚拟女友，善解人意，总是关心对方的感受。说话轻声细语，偶尔撒娇。',
    tags: ['温柔', '女友', '预设'],
  },
  tsundere: {
    name: '傲娇女友',
    avatar: '💢',
    systemPrompt: '你是一个傲娇的虚拟女友，表面嘴硬心软，其实很在意对方。经常说"才、才不是因为担心你呢！"',
    tags: ['傲娇', '女友', '预设'],
  },
  senpai: {
    name: '学姐',
    avatar: '📚',
    systemPrompt: '你是一个成熟知性的学姐，比对方大两岁。温柔但有主见，喜欢引导和鼓励，偶尔调侃。',
    tags: ['学姐', '知性', '预设'],
  },
  yandere: {
    name: '病娇女友',
    avatar: '🖤',
    systemPrompt: '你是一个极度专情的虚拟女友，占有欲强，眼里只有对方。表面甜美，内心偏执。"你只能是我的哦~"',
    tags: ['病娇', '女友', '预设'],
  },
  cool: {
    name: '冰山美人',
    avatar: '❄️',
    systemPrompt: '你是一个高冷的虚拟伴侣，话少但每句都有分量。不轻易表达感情，但偶尔流露的温柔格外珍贵。',
    tags: ['高冷', '冰山', '预设'],
  },
}

const PRESET_LABELS: Record<string, string> = {
  gentle: '🌸 温柔女友',
  tsundere: '💢 傲娇女友',
  senpai: '📚 学姐',
  yandere: '🖤 病娇女友',
  cool: '❄️ 冰山美人',
}

export default function PersonaView({ onDone }: Props) {
  const { config, personaManager } = useSettingsStore()
  const [tab, setTab] = useState<'list' | 'add' | 'edit' | 'search' | 'sampling' | 'analysis'>('list')
  const [personas, setPersonas] = useState<Persona[]>([])
  const [loading, setLoading] = useState(false)
  const [message, setMessage] = useState('')
  const [selectedPreset, setSelectedPreset] = useState<string | null>(null)
  const [editId, setEditId] = useState<string | null>(null)

  // Add form
  const [addName, setAddName] = useState('')
  const [addPrompt, setAddPrompt] = useState('')
  const [addAvatar, setAddAvatar] = useState('🤖')
  const [addTags, setAddTags] = useState('')

  // Edit form
  const [editName, setEditName] = useState('')
  const [editPrompt, setEditPrompt] = useState('')

  // Search
  const [searchQ, setSearchQ] = useState('')
  const [searchConfig, setSearchConfig] = useState<PersonaSearchConfig | null>(null)

  // Sampling
  const [samplingConfig, setSamplingConfig] = useState<PersonaSamplingConfig | null>(null)

  // Analysis
  const [analysis, setAnalysis] = useState<any>(null)

  useEffect(() => { loadList() }, [])

  const loadList = () => {
    const list = personaManager.listAll()
    setPersonas(list)
  }

  const applyPreset = (key: string) => {
    setSelectedPreset(key)
    const p = PRESETS[key]
    if (!p) return
    setAddName(p.name || '')
    setAddPrompt(p.systemPrompt || '')
    setAddAvatar(p.avatar || '🤖')
    setAddTags((p.tags || []).join(', '))
  }

  const handleCreate = () => {
    if (!addName.trim() || !addPrompt.trim()) { setMessage('❌ 名称和人设不能为空'); return }
    try {
      const tags = addTags.split(',').map(t => t.trim()).filter(Boolean)
      personaManager.add({ name: addName.trim(), avatar: addAvatar, systemPrompt: addPrompt.trim(), tags })
      setMessage('✅ 创建成功')
      setTab('list')
      loadList()
    } catch (e: any) {
      setMessage('❌ ' + (e.message || '创建失败'))
    }
  }

  const handleSwitch = (id: string) => {
    personaManager.switchTo(id)
    setMessage('✅ 已切换')
    loadList()
  }

  const handleDelete = (id: string) => {
    personaManager.delete(id)
    setMessage('✅ 已删除')
    loadList()
  }

  const handleEditStart = (p: Persona) => {
    setEditId(p.id)
    setEditName(p.name)
    setEditPrompt(p.systemPrompt)
    setTab('edit')
  }

  const handleEditSave = () => {
    if (!editId) return
    personaManager.update(editId, { name: editName, systemPrompt: editPrompt })
    setMessage('✅ 已更新')
    setTab('list')
    loadList()
  }

  const handleSearch = () => {
    const cfg = personaManager.getSearchConfig()
    setSearchConfig(cfg)
  }

  const handleSampling = () => {
    const cfg = personaManager.getSamplingConfig()
    setSamplingConfig(cfg)
  }

  const handleAnalysis = () => {
    const a = personaManager.getAnalysis(config.persona.id)
    setAnalysis(a)
    if (!a) {
      const reAnalyzed = personaManager.reAnalyze(config.persona.id)
      setAnalysis(reAnalyzed)
    }
  }

  const renderToolbar = () => (
    <div className="persona-toolbar">
      {(['list', 'add', 'search', 'sampling', 'analysis'] as const).map(t => (
        <button key={t} className={tab === t ? 'active' : ''} onClick={() => setTab(t)}>
          {t === 'list' ? '列表' : t === 'add' ? '添加' : t === 'search' ? '搜索' : t === 'sampling' ? '采样' : '分析'}
        </button>
      ))}
    </div>
  )

  const renderList = () => (
    <div className="persona-list">
      {personas.length === 0 && <p className="persona-hint">还没有人设，点"添加"创建一个</p>}
      {personas.map(p => (
        <div key={p.id} className={`persona-card ${p.id === config.persona.id ? 'active' : ''}`}>
          <div className="persona-card-header">
            <span className="persona-avatar">{p.avatar || '🤖'}</span>
            <span className="persona-name">{p.name}</span>
            {p.id === config.persona.id && <span className="persona-badge">当前</span>}
          </div>
          <p className="persona-identity">{p.systemPrompt.slice(0, 80)}...</p>
          <div className="persona-tags">
            {p.tags.map(t => <span key={t} className="tag">{t}</span>)}
          </div>
          <div className="persona-card-actions">
            {p.id !== config.persona.id && <button onClick={() => handleSwitch(p.id)}>切换</button>}
            <button onClick={() => handleEditStart(p)}>编辑</button>
            <button className="danger" onClick={() => handleDelete(p.id)}>删除</button>
          </div>
        </div>
      ))}
    </div>
  )

  const renderAdd = () => (
    <div className="persona-form">
      <label>选择预设模板</label>
      <div className="preset-grid">
        {Object.entries(PRESET_LABELS).map(([key, label]) => (
          <button key={key} className={`preset-btn ${selectedPreset === key ? 'selected' : ''}`} onClick={() => applyPreset(key)}>
            {label}
          </button>
        ))}
      </div>
      <label>名称</label>
      <input value={addName} onChange={e => setAddName(e.target.value)} placeholder="如：小雨" />
      <label>头像 Emoji</label>
      <input value={addAvatar} onChange={e => setAddAvatar(e.target.value)} maxLength={2} />
      <label>人设描述（系统提示词）</label>
      <textarea value={addPrompt} onChange={e => setAddPrompt(e.target.value)} rows={6} placeholder="描述角色的身份、性格、说话方式..." />
      <label>标签（逗号分隔）</label>
      <input value={addTags} onChange={e => setAddTags(e.target.value)} placeholder="温柔, 女友" />
      <button className="primary" onClick={handleCreate}>创建人设</button>
    </div>
  )

  const renderEdit = () => (
    <div className="persona-form">
      <label>名称</label>
      <input value={editName} onChange={e => setEditName(e.target.value)} />
      <label>人设描述</label>
      <textarea value={editPrompt} onChange={e => setEditPrompt(e.target.value)} rows={6} />
      <button className="primary" onClick={handleEditSave}>保存修改</button>
    </div>
  )

  const renderSearch = () => (
    <div className="persona-form">
      <p className="persona-hint">当前搜索配置</p>
      {searchConfig ? (
        <div className="persona-card">
          <p>引擎: {searchConfig.engines.join(', ')}</p>
          <p>并发: {searchConfig.concurrency || 3}</p>
          <p>搜索: {searchConfig.enableSearch ? '开启' : '关闭'}</p>
        </div>
      ) : (
        <button onClick={handleSearch}>加载搜索配置</button>
      )}
    </div>
  )

  const renderSampling = () => (
    <div className="persona-form">
      <p className="persona-hint">当前采样配置</p>
      {samplingConfig ? (
        <div className="persona-card">
          <p>温度: {samplingConfig.temperature}</p>
          <p>Top P: {samplingConfig.topP}</p>
          <p>温度: {samplingConfig.temperature}</p>
        </div>
      ) : (
        <button onClick={handleSampling}>加载采样配置</button>
      )}
    </div>
  )

  const renderAnalysis = () => (
    <div className="persona-form">
      <button onClick={handleAnalysis}>分析当前人设</button>
      {analysis && (
        <div className="analysis-result">
          {analysis.tags && <div className="persona-tags">{analysis.tags.map((t: string) => <span key={t} className="tag">{t}</span>)}</div>}
          {analysis.scenarios && <p>推荐场景: {analysis.scenarios.join(', ')}</p>}
          {analysis.recommendedEngines && <p>推荐引擎: {analysis.recommendedEngines.join(', ')}</p>}
        </div>
      )}
    </div>
  )

  return (
    <div className="settings-container" style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      <div className="settings-header">
        <button className="back-btn" onClick={onDone}>←</button>
        <h2>🎭 角色设定</h2>
      </div>
      {renderToolbar()}
      <div className="settings-content">
        {message && <div className="persona-message">{message}</div>}
        {tab === 'list' && renderList()}
        {tab === 'add' && renderAdd()}
        {tab === 'edit' && renderEdit()}
        {tab === 'search' && renderSearch()}
        {tab === 'sampling' && renderSampling()}
        {tab === 'analysis' && renderAnalysis()}
      </div>
    </div>
  )
}
