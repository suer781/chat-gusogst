import { useState, useEffect } from 'react'
import { initApp } from './init'
import { useSettingsStore } from './stores'
import { ChatView } from './chat/ChatView'
import { SettingsView } from './settings/SettingsView'
import { PersonaView } from './persona/PersonaView'
import PersonaProfileView from './persona/PersonaProfileView'
import { ProviderSettings } from './providers/ProviderSettings'
import { ChevronLeft, Settings, MessageSquare, Users, Server } from 'lucide-react'
import { t, onLangChange } from './i18n'
import TestDisclaimer from './components/TestDisclaimer'

type View = 'chat' | 'settings' | 'persona' | 'personaProfile' | 'providers'

export default function App() {
  const [view, setView] = useState<View>('chat')
  const [selectedPersona, setSelectedPersona] = useState<any>(null)
  const [, forceUpdate] = useState(0)
  const persona = useSettingsStore((s) => s.config.persona)

  useEffect(() => { onLangChange(() => forceUpdate((n) => n + 1)); }, [])

  const viewTitles: Record<View, string> = {
    chat: t('nav.chat'),
    settings: t('nav.settings'),
    persona: t('nav.persona'),
    providers: t('nav.providers'),
    personaProfile: t('nav.persona'),
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100%', maxHeight: '100%', background: '#0f0f23', color: '#e0e0e0', overflow: 'hidden' }}>
      <header style={{ display: 'flex', alignItems: 'center', flexShrink: 0, height: 'calc(48px + env(safe-area-inset-top, 0px))', padding: 'env(safe-area-inset-top, 0px) 12px 0 12px', background: '#0f0f23', borderBottom: '1px solid #1a1a3a' }}>
        {view === 'personaProfile' ? (
          <button onClick={() => setView('chat')} style={{ display: 'flex', alignItems: 'center', gap: 4, color: '#e94560', background: 'none', border: 'none', cursor: 'pointer', fontSize: 14 }}>
            <ChevronLeft size={20} /> {t('btn.back')}
          </button>
        ) : <div style={{ width: 60 }} />}
        <div style={{ flex: 1, textAlign: 'center', fontSize: 16, fontWeight: 600 }}>{viewTitles[view]}</div>
        {view === 'chat' ? (
          <button onClick={() => setView('settings')} style={{ color: '#8888aa', background: 'none', border: 'none', cursor: 'pointer' }}>
            <Settings size={20} />
          </button>
        ) : <div style={{ width: 60 }} />}
      </header>

      <div style={{ flex: 1, minHeight: 0, overflow: 'hidden' }}>
        {view === 'chat' && <ChatView onNavigate={setView} />}
        {view === 'settings' && <SettingsView onDone={() => setView('chat')} />}
        {view === 'persona' && <PersonaView onDone={() => setView('chat')} onProfile={(p) => { setSelectedPersona(p); setView('personaProfile') }} />}
        {view === 'personaProfile' && selectedPersona && <PersonaProfileView persona={selectedPersona} onBack={() => setView('persona')} onStartChat={() => { useSettingsStore.getState().setPersona(selectedPersona); setView('chat') }} />}
        {view === 'providers' && <ProviderSettings onDone={() => setView('settings')} />}
      </div>

      <nav style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-around', flexShrink: 0, height: 56, paddingBottom: 'env(safe-area-inset-bottom, 0px)', background: '#0a0a1a', borderTop: '1px solid #1a1a3a' }}>
        {[{ id: 'chat' as View, icon: MessageSquare, labelKey: 'nav.chat' },
          { id: 'persona' as View, icon: Users, labelKey: 'nav.persona' },
          { id: 'providers' as View, icon: Server, labelKey: 'nav.providers' },
          { id: 'settings' as View, icon: Settings, labelKey: 'nav.settings' }].map((item) => (
          <button key={item.id} onClick={() => setView(item.id)}
            style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 2, background: 'none', border: 'none', cursor: 'pointer', padding: '4px 12px', color: view === item.id ? '#e94560' : '#666688' }}>
            <item.icon size={20} />
            <span style={{ fontSize: 10 }}>{t(item.labelKey)}</span>
          </button>
        ))}
      </nav>

      <TestDisclaimer />
    </div>
  )
}
// rebuild trigger $(date)
