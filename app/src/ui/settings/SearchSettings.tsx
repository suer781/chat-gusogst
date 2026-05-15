import React, { useState } from 'react'
import { useSettingsStore } from '../stores'

interface Props { onBack: () => void }

// ── 引擎定义 ──────────────────────

interface EngineDef { id: string; name: string; desc: string }

const GENERAL_ENGINES: EngineDef[] = [
  { id: 'baidu', name: '百度', desc: '国内最大' },
  { id: 'bing', name: '必应', desc: '微软' },
  { id: 'duckduckgo', name: 'DuckDuckGo', desc: '隐私优先' },
  { id: 'brave', name: 'Brave', desc: '独立搜索' },
  { id: 'so360', name: '360', desc: '奇虎' },
  { id: 'sogou', name: '搜狗', desc: '腾讯' },
  { id: 'shenma', name: '神马', desc: 'UC' },
  { id: 'toutiao', name: '头条', desc: '字节' },
  { id: 'chinaso', name: '中国搜索', desc: '国家级' },
  { id: 'quark', name: '夸克', desc: '阿里' },
]

const VERTICAL_ENGINES: EngineDef[] = [
  { id: 'zhihu', name: '知乎', desc: '问答' },
  { id: 'weixin', name: '微信', desc: '公众号' },
  { id: 'baidu_dev', name: '百度开发', desc: '技术文档' },
  { id: 'baidu_academic', name: '百度学术', desc: '论文' },
  { id: 'bocha', name: '博查 AI', desc: 'AI 搜索' },
  { id: 'smzdm', name: '值得买', desc: '比价' },
  { id: '58', name: '58同城', desc: '生活' },
]

const CLOUD_ENGINES: EngineDef[] = [
  { id: 'pansou', name: 'Pansou', desc: '聚合' },
  { id: 'panclub', name: '网盘俱乐部', desc: '资源' },
  { id: 'xiongdipan', name: '兄弟盘', desc: '资源' },
  { id: 'pikasou', name: '皮卡搜索', desc: '网盘' },
  { id: 'pandashi', name: '盘大师', desc: '网盘' },
  { id: 'haisou', name: '海搜', desc: '网盘' },
  { id: 'xuebapan', name: '学霸盘', desc: '学习' },
  { id: 'duanjuso', name: '短剧搜', desc: '短剧' },
  { id: 'yunso', name: '小云', desc: '综合' },
  { id: 'qupansou', name: '趣盘搜', desc: '网盘' },
  { id: 'xiaobaipan', name: '小白盘', desc: '网盘' },
  { id: 'liangyiniao', name: '两仪鸟', desc: '网盘' },
  { id: 'yunpanem', name: '云盘恶魔', desc: '网盘' },
  { id: 'woaisoupan', name: '我爱搜盘', desc: '网盘' },
]

const ALL_ENGINES = [...GENERAL_ENGINES, ...VERTICAL_ENGINES, ...CLOUD_ENGINES]

// ── 预设组合 ──────────────────────

const PRESETS = [
  { id: 'general', name: '通用', engines: ['duckduckgo', 'baidu', 'bing', 'brave'] },
  { id: 'china', name: '国内优先', engines: ['baidu', 'bing', 'sogou', 'so360'] },
  { id: 'tech', name: '技术向', engines: ['baidu_dev', 'duckduckgo', 'zhihu'] },
  { id: 'academic', name: '学术向', engines: ['baidu_academic', 'duckduckgo'] },
  { id: 'shopping', name: '购物', engines: ['smzdm', 'baidu', 'bing'] },
  { id: 'cloud', name: '网盘资源', engines: ['pansou', 'panclub', 'xiongdipan', 'pikasou'] },
  { id: 'privacy', name: '隐私优先', engines: ['duckduckgo', 'brave'] },
]

// ── 组件 ──────────────────────

export default function SearchSettings({ onBack }: Props) {
  const { config, updateConfig } = useSettingsStore()
  const [expandedCat, setExpandedCat] = useState<string | null>('通用搜索')
  const [showAll, setShowAll] = useState(false)

  const searchEnabled = config.searchEnabled ?? false
  // searchConfig is on Persona, not AgentConfig
  const [enabled, setEnabled] = useState<string[]>(['duckduckgo', 'baidu'])

  const updateEngines = (engines: string[]) => {
    setEnabled(engines)
  }

  const toggleEngine = (id: string) => {
    const next = enabled.includes(id) ? enabled.filter((e) => e !== id) : [...enabled, id]
    updateEngines(next)
  }

  const categories = [
    { label: '通用搜索', engines: GENERAL_ENGINES },
    { label: '垂直/AI', engines: VERTICAL_ENGINES },
    { label: '网盘资源', engines: CLOUD_ENGINES },
  ]

  return (
    <div className="settings-container" style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      <div className="settings-header">
        <button className="back-btn" onClick={onBack}>←</button>
        <h2>🔍 搜索引擎</h2>
        <span style={{ fontSize: 12, opacity: 0.5 }}>{enabled.length} / {ALL_ENGINES.length}</span>
      </div>
      <div className="settings-content">
        {/* ── 总开关 ── */}
        <div className="setting-section">
          <label>联网搜索</label>
          <div className="setting-options">
            <button
              className={searchEnabled ? 'active' : ''}
              onClick={() => updateConfig({ searchEnabled: !searchEnabled })}
            >
              {searchEnabled ? '✅ 已开启' : '开启'}
            </button>
          </div>
        </div>

        {searchEnabled && (
          <>
            {/* ── 预设组合 ── */}
            <div className="setting-section">
              <label>快速预设</label>
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: 8, marginTop: 8 }}>
                {PRESETS.map((preset) => {
                  const isActive = enabled.length === preset.engines.length && preset.engines.every((e) => enabled.includes(e))
                  return (
                    <button
                      key={preset.id}
                      className={isActive ? 'active' : ''}
                      onClick={() => updateEngines(preset.engines)}
                      style={{ textAlign: 'left', padding: '8px 12px' }}
                    >
                      <div style={{ fontWeight: 600 }}>{preset.name}</div>
                      <div style={{ fontSize: 11, opacity: 0.5 }}>{preset.engines.length} 个引擎</div>
                    </button>
                  )
                })}
              </div>
            </div>

            {/* ── 已选引擎 ── */}
            <div className="setting-section">
              <label>已选引擎</label>
              <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6, marginTop: 8 }}>
                {enabled.map((id) => {
                  const engine = ALL_ENGINES.find((e) => e.id === id)
                  return (
                    <span
                      key={id}
                      onClick={() => toggleEngine(id)}
                      style={{
                        padding: '4px 10px', borderRadius: 12, fontSize: 12,
                        background: 'var(--accent-color, #3b82f6)', color: '#fff',
                        cursor: 'pointer', opacity: 0.9,
                      }}
                      title="点击移除"
                    >
                      {engine?.name || id} ×
                    </span>
                  )
                })}
                {enabled.length === 0 && <span style={{ fontSize: 12, opacity: 0.4 }}>未选择</span>}
              </div>
            </div>

            {/* ── 全部引擎 ── */}
            <div className="setting-section">
              <label style={{ cursor: 'pointer' }} onClick={() => setShowAll(!showAll)}>
                全部引擎 ({ALL_ENGINES.length}) {showAll ? '▲' : '▼'}
              </label>

              {categories.map((cat) => (
                <div key={cat.label} style={{ marginTop: 8 }}>
                  <div
                    style={{ fontSize: 13, fontWeight: 600, opacity: 0.7, cursor: 'pointer', marginBottom: 4 }}
                    onClick={() => setExpandedCat(expandedCat === cat.label ? null : cat.label)}
                  >
                    {cat.label} ({cat.engines.length}) {expandedCat === cat.label ? '▲' : '▼'}
                  </div>
                  {(expandedCat === cat.label || showAll) && (
                    <div style={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                      {cat.engines.map((engine) => (
                        <div
                          key={engine.id}
                          style={{
                            display: 'flex', alignItems: 'center', justifyContent: 'space-between',
                            padding: '6px 8px', borderRadius: 6, cursor: 'pointer',
                            background: enabled.includes(engine.id) ? 'var(--accent-bg, rgba(59,130,246,0.1))' : 'transparent',
                          }}
                          onClick={() => toggleEngine(engine.id)}
                        >
                          <div>
                            <span style={{ fontWeight: 500 }}>{engine.name}</span>
                            <span style={{ fontSize: 11, opacity: 0.4, marginLeft: 8 }}>{engine.desc}</span>
                          </div>
                          <span style={{ fontSize: 14 }}>{enabled.includes(engine.id) ? '✅' : '○'}</span>
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              ))}
            </div>
          </>
        )}
      </div>
    </div>
  )
}
