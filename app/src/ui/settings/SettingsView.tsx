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
  { key: 'basic',    icon: Palette, label: t('settings.basic.label'),   desc: t('settings.basic.desc'), color: 'var(--accent)' },
  { key: 'model',    icon: Bot,     label: t('settings.model.label'),    desc: t('settings.model.desc'),  color: 'var(--purple)' },
  { key: 'platform', icon: Link2,   label: t('settings.platform.label'),   desc: t('settings.platform.desc'), color: 'var(--blue)' },
  { key: 'memory',   icon: Brain,   label: t('settings.memory.label'),       desc: t('settings.memory.cardDesc'),          color: 'var(--yellow)' },
  { key: 'search',   icon: Search,  label: t('settings.search.label'),       desc: t('settings.search.cardDesc'),          color: 'var(--warning)' },
  { key: 'about',    icon: Info,    label: t('settings.about.label'),       desc: t('settings.about.desc'),        color: 'var(--gray-500)' },
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
    <div style={{ minHeight: '100%', background: 'var(--bg-primary)', padding: '0 0 100px' }}>
      <div style={{
        display: 'flex', alignItems: 'center', justifyContent: 'space-between',
        padding: '16px 20px', position: 'sticky', top: 0,
        background: 'var(--bg-overlay)', backdropFilter: 'blur(20px)',
        zIndex: 10, borderBottom: '1px solid var(--divider)',
      }}>
        <button onClick={onDone} style={{
          background: 'none', border: 'none', color: 'var(--accent)',
          fontSize: "var(--text-2xl)", cursor: 'pointer', padding: 4,
          display: 'flex', alignItems: 'center',
        }}>{'<-'}</button>
        <span style={{ fontSize: "var(--text-xl)", fontWeight: 600, color: 'var(--text-primary)' }}>设置</span>
        <div style={{ width: 28 }} />
      </div>

      <div style={{ padding: '12px 16px', display: 'flex', flexDirection: 'column', gap: 10 }}>
        {CARDS.map(({ key, icon: Icon, label, desc, color }, i) => (
          <button key={key} onClick={() => setSubPage(key)} style={{
            display: 'flex', alignItems: 'center', gap: 14,
            padding: '16px 18px',
            background: 'rgba(255,255,255,0.03)',
            border: '1px solid rgba(255,255,255,0.05)',
            borderRadius: "var(--radius-lg)", cursor: 'pointer', textAlign: 'left', width: '100%',
            transition: 'all 0.2s cubic-bezier(0.4,0,0.2,1)',
            animation: `fadeInUp 0.3s ${i * 0.04}s both`,
          }} onMouseEnter={(e) => {
            e.currentTarget.style.background = 'var(--divider)'
            e.currentTarget.style.transform = 'translateX(4px)'
          }} onMouseLeave={(e) => {
            e.currentTarget.style.background = 'rgba(255,255,255,0.03)'
            e.currentTarget.style.transform = 'translateX(0)'
          }}>
            <div style={{
              width: 42, height: 42, borderRadius: "var(--radius-md)",
              background: `${color}18`,
              display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0,
            }}>
              <Icon size={20} color={color} />
            </div>
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{ color: 'var(--text-primary)', fontSize: "var(--text-md)", fontWeight: 500 }}>{label}</div>
              <div style={{ color: 'var(--gray-400)', fontSize: "var(--text-sm)", marginTop: 2 }}>{desc}</div>
            </div>
            <span style={{ color: 'var(--gray-500)', fontSize: "var(--text-xl)" }}>{'>'}</span>
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
