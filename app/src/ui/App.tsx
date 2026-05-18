import { useState, useEffect, useRef } from 'react'
import { initApp } from './init'
import { useSettingsStore, type EyeCareMapping } from './stores'
import { ChatView } from './chat/ChatView'
import { SettingsView } from './settings/SettingsView'
import { PersonaView } from './persona/PersonaView'
import PersonaProfileView from './persona/PersonaProfileView'
import { ProviderSettings } from './providers/ProviderSettings'
import { ChevronLeft, Settings, MessageSquare, Users, Server } from 'lucide-react'
import { t, onLangChange } from './i18n'
import TestDisclaimer from './components/TestDisclaimer'
import { light as hapticLight, setHapticEnabled } from './haptics'

type View = 'chat' | 'settings' | 'persona' | 'personaProfile' | 'providers'

const NAV_ITEMS = [
  { id: 'chat' as View, icon: MessageSquare, labelKey: 'nav.chat' },
  { id: 'persona' as View, icon: Users, labelKey: 'nav.persona' },
  { id: 'providers' as View, icon: Server, labelKey: 'nav.providers' },
  { id: 'settings' as View, icon: Settings, labelKey: 'nav.settings' },
]

export default function App() {
  const [view, setView] = useState<View>('chat')
  const [selectedPersona, setSelectedPersona] = useState<any>(null)
  const [, forceUpdate] = useState(0)
  const persona = useSettingsStore((s) => s.persona)
  const themeMode = useSettingsStore((s) => s.themeMode)
  const fontSize = useSettingsStore((s) => s.fontSize)
  const eyeCareEnabled = useSettingsStore((s) => s.eyeCareEnabled)
  const eyeCareColors = useSettingsStore((s) => s.eyeCareColors)
  const eyeCareIntensity = useSettingsStore((s) => s.eyeCareIntensity)
  const glassEnabled = useSettingsStore((s) => s.glassEnabled)
  const glassOpacity = useSettingsStore((s) => s.glassOpacity)
  const hapticEnabled = useSettingsStore((s) => s.hapticEnabled)
  const hdrEnabled = useSettingsStore((s) => s.hdrEnabled)

  // 导航指示器位置
  const activeIdx = NAV_ITEMS.findIndex((item) =>
    view === item.id || (view === 'personaProfile' && item.id === 'persona')
  )

  useEffect(() => {
    const root = document.documentElement
    const body = document.body
    root.setAttribute('data-theme', themeMode)
    root.style.setProperty('--app-font-size', String(fontSize))
    root.style.setProperty('--app-font-size-px', fontSize + 'px')
    body.style.fontSize = fontSize + 'px'
    if (eyeCareEnabled) {
      root.style.setProperty('--eyecare-intensity', String(eyeCareIntensity / 100))
      eyeCareColors.forEach((m: EyeCareMapping, i: number) => {
        root.style.setProperty(`--eyecare-src-${i}`, m.sourceColor)
        root.style.setProperty(`--eyecare-tgt-${i}`, m.targetColor)
      })
      for (let i = eyeCareColors.length; i < 50; i++) {
        root.style.removeProperty(`--eyecare-src-${i}`)
        root.style.removeProperty(`--eyecare-tgt-${i}`)
      }
      root.style.setProperty('--eyecare-bg', eyeCareColors[0]?.targetColor || 'var(--gray-900)')
      root.style.setProperty('--eyecare-text', eyeCareColors[2]?.targetColor || 'var(--gray-50)')
      root.setAttribute('data-eyecare', 'on')
    } else {
      root.removeAttribute('data-eyecare')
    }
    root.setAttribute('data-glass', glassEnabled ? 'on' : 'off')
    root.style.setProperty('--glass-opacity', String(glassOpacity / 100))
    setHapticEnabled(hapticEnabled)
    root.setAttribute('data-hdr', hdrEnabled ? 'on' : 'off')
  }, [themeMode, fontSize, eyeCareEnabled, eyeCareColors, eyeCareIntensity, glassEnabled, glassOpacity, hapticEnabled, hdrEnabled])

  useEffect(() => {
    const unsub = onLangChange(() => forceUpdate((n) => n + 1))
    return () => { if (typeof unsub === 'function') unsub() }
  }, [])

  const viewTitles: Record<View, string> = {
    chat: t('nav.chat'),
    settings: t('nav.settings'),
    persona: t('nav.persona'),
    providers: t('nav.providers'),
    personaProfile: t('nav.persona'),
  }

  initApp()

  return (
    <div className="app-root" style={{ display: 'flex', flexDirection: 'column', height: '100%', maxHeight: '100%', background: 'var(--bg-primary)', color: 'var(--text-primary, #e0e0e0)', overflow: 'hidden', transition: 'background-color 0.4s ease, color 0.4s ease' }}>
      {/* ── Header ── */}
      <header className="app-header" style={{ display: 'flex', alignItems: 'center', flexShrink: 0, height: 'calc(48px + env(safe-area-inset-top, 0px))', padding: 'env(safe-area-inset-top, 0px) 12px 0 12px', background: 'var(--bg-primary)', borderBottom: '1px solid var(--border-color)', transition: 'background-color 0.4s ease' }}>
        {view === 'personaProfile' ? (
          <button onClick={() => { hapticLight(); setView('persona') }} style={{ display: 'flex', alignItems: 'center', gap: 4, color: 'var(--accent)', background: 'none', border: 'none', cursor: 'pointer', fontSize: 14 }}>
            <ChevronLeft size={20} /> {t('btn.back')}
          </button>
        ) : <div style={{ width: 60 }} />}
        <div style={{ flex: 1, textAlign: 'center', fontSize: 16, fontWeight: 600 }}>{viewTitles[view]}</div>
        <div style={{ width: 60 }} />
      </header>

      {/* ── Content Area ── 修复：允许垂直滚动 */}
      <div className="app-content" style={{ flex: 1, minHeight: 0, overflowY: 'auto', overflowX: 'hidden', WebkitOverflowScrolling: 'touch', overscrollBehavior: 'contain' }}>
        {view === 'chat' && <ChatView onNavigate={setView} />}
        {view === 'settings' && <SettingsView onDone={() => setView('chat')} />}
        {view === 'persona' && <PersonaView onDone={() => setView('chat')} onProfile={(p) => { setSelectedPersona(p); setView('personaProfile') }} />}
        {view === 'personaProfile' && selectedPersona && <PersonaProfileView persona={selectedPersona} onBack={() => setView('persona')} onStartChat={() => { useSettingsStore.getState().setPersona(selectedPersona); setView('chat') }} />}
        {view === 'providers' && <ProviderSettings onDone={() => setView('settings')} />}
      </div>

      {/* ── Bottom Nav ──  重做：渐变底 + 透镜指示器 + 毛玻璃一体 */}
      <nav className="app-nav" style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-around',
        flexShrink: 0,
        position: 'relative',
        height: 56,
        paddingBottom: 'env(safe-area-inset-bottom, 0px)',
        background: 'linear-gradient(180deg, var(--bg-secondary) 0%, var(--bg-primary) 100%)',
        borderTop: '1px solid var(--border-color)',
        transition: 'background 0.4s ease',
      }}>
        {/* 透镜指示器 */}
        <div
          className="nav-indicator"
          style={{
            position: 'absolute',
            top: 4,
            left: `${activeIdx * 25}%`,
            width: '25%',
            height: 'calc(100% - 8px)',
            display: 'flex',
            justifyContent: 'center',
            alignItems: 'center',
            pointerEvents: 'none',
            transition: 'left 0.45s cubic-bezier(0.4, 0, 0.2, 1)',
            zIndex: 0,
          }}
        >
          <div style={{
            width: 48,
            height: 36,
            borderRadius: 18,
            background: 'var(--accent-soft, rgba(233, 69, 96, 0.12))',
            boxShadow: '0 0 12px var(--accent-glow, rgba(233, 69, 96, 0.15))',
            transition: 'all 0.45s cubic-bezier(0.4, 0, 0.2, 1)',
          }} />
        </div>

        {NAV_ITEMS.map((item) => {
          const isActive = view === item.id || (view === 'personaProfile' && item.id === 'persona')
          return (
            <button
              key={item.id}
              onClick={() => { hapticLight(); setView(item.id) }}
              className="nav-btn"
              style={{
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                justifyContent: 'center',
                gap: 2,
                background: 'none',
                border: 'none',
                cursor: 'pointer',
                padding: '10px 18px',
                minWidth: 64,
                minHeight: 48,
                position: 'relative',
                zIndex: 1,
                color: isActive ? 'var(--accent)' : 'var(--gray-400)',
                transition: 'color 0.3s ease',
              }}
            >
              <item.icon size={20} />
              <span style={{ fontSize: 10 }}>{t(item.labelKey)}</span>
            </button>
          )
        })}
      </nav>

      <TestDisclaimer />
    </div>
  )
}
