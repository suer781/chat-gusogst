import { useState } from 'react'
import { Palette, Bot, Link2, Brain, Search, Info } from 'lucide-react'
import { t } from '../i18n'
import { BasicSettings } from './BasicSettings'
import { ModelSettings } from './ModelSettings'
import { PlatformSettings } from './PlatformSettings'
import { MemorySettings } from './MemorySettings'
import { SearchSettings } from './SearchSettings'
import { AboutSettings } from './AboutSettings'

type SubPage = null | 'basic' | 'model' | 'platform' | 'memory' | 'search' | 'about'

const CARDS = [
  { key: 'basic',    icon: Palette, label: '基础设置',   desc: '主题、字号、护眼、视觉效果', color: '#e94560' },
  { key: 'model',    icon: Bot,     label: 'AI 模型',    desc: 'Provider、模型选择、参数调节',  color: '#6C5CE7' },
  { key: 'platform', icon: Link2,   label: '平台连接',   desc: '微信、QQ、Telegram 等 16 个', color: '#0984E3' },
  { key: 'memory',   icon: Brain,   label: '记忆',       desc: '记忆开关、容量管理',          color: '#FDCB6E' },
  { key: 'search',   icon: Search,  label: '搜索',       desc: '搜索引擎、API Key',          color: '#E17055' },
  { key: 'about',    icon: Info,    label: '关于',       desc: '版本、反馈、开源协议',        color: '#636E72' },
] as const

export function SettingsView({ onDone }: { onDone: () => void }) {
  const [subPage, setSubPage] = useState<SubPage>(null)

  if (subPage === 'basic')    return <BasicSettings onBack={() => setSubPage(null)} />
  if (subPage === 'model')    return <ModelSettings onBack={() => setSubPage(null)} />
  if (subPage === 'platform') return <PlatformSettings onBack={() => setSubPage(null)} />
  if (subPage === 'memory')   return <MemorySettings onBack={() => setSubPage(null)} />
  if (subPage === 'search')   return <SearchSettings onBack={() => setSubPage(null)} />
  if (subPage === 'about')    return <AboutSettings onBack={() => setSubPage(null)} />

  return (
    <div style={{ minHeight: '100%', background: '#0f0f23', padding: '0 0 100px' }}>
      <div style={{
        display: 'flex', alignItems: 'center', justifyContent: 'space-between',
        padding: '16px 20px', position: 'sticky', top: 0,
        background: 'rgba(15,15,35,0.9)', backdropFilter: 'blur(20px)',
        zIndex: 10, borderBottom: '1px solid rgba(255,255,255,0.06)',
      }}>
        <button onClick={onDone} style={{
          background: 'none', border: 'none', color: '#e94560',
          fontSize: 20, cursor: 'pointer', padding: 4,
          display: 'flex', alignItems: 'center',
        }}>{'<-'}</button>
        <span style={{ fontSize: 18, fontWeight: 600, color: '#fff' }}>设置</span>
        <div style={{ width: 28 }} />
      </div>

      <div style={{ padding: '12px 16px', display: 'flex', flexDirection: 'column', gap: 10 }}>
        {CARDS.map(({ key, icon: Icon, label, desc, color }, i) => (
          <button key={key} onClick={() => setSubPage(key)} style={{
            display: 'flex', alignItems: 'center', gap: 14,
            padding: '16px 18px',
            background: 'rgba(255,255,255,0.03)',
            border: '1px solid rgba(255,255,255,0.05)',
            borderRadius: 16, cursor: 'pointer', textAlign: 'left', width: '100%',
            transition: 'all 0.2s cubic-bezier(0.4,0,0.2,1)',
            animation: `fadeInUp 0.3s ${i * 0.04}s both`,
          }} onMouseEnter={(e) => {
            e.currentTarget.style.background = 'rgba(255,255,255,0.06)'
            e.currentTarget.style.transform = 'translateX(4px)'
          }} onMouseLeave={(e) => {
            e.currentTarget.style.background = 'rgba(255,255,255,0.03)'
            e.currentTarget.style.transform = 'translateX(0)'
          }}>
            <div style={{
              width: 42, height: 42, borderRadius: 12,
              background: `${color}18`,
              display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0,
            }}>
              <Icon size={20} color={color} />
            </div>
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{ color: '#fff', fontSize: 15, fontWeight: 500 }}>{label}</div>
              <div style={{ color: '#666', fontSize: 12, marginTop: 2 }}>{desc}</div>
            </div>
            <span style={{ color: '#444', fontSize: 18 }}>{'>'}</span>
          </button>
        ))}
      </div>

      <style>{`
        @keyframes fadeInUp {
          from { opacity: 0; transform: translateY(12px); }
          to   { opacity: 1; transform: translateY(0); }
        }
      `}</style>
    </div>
  )
}
