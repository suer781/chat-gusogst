import { useEffect, useState } from 'react'
import { useSettingsStore, useChatStore } from './stores'
import { ChatView } from './chat/ChatView'
import SettingsView from './settings/SettingsView'
import { PersonaView } from './persona/PersonaView'

type View = 'chat' | 'settings' | 'persona'

export default function App() {
  const [view, setView] = useState<View>('chat')
  const init = useSettingsStore(s => s.init)
  const initialized = useSettingsStore(s => s.initialized)
  const config = useSettingsStore(s => s.config)

  useEffect(() => { init() }, [])

  if (!initialized) {
    return (
      <div className="app-loading">
        <div className="loading-spinner" />
        <p>正在启动...</p>
      </div>
    )
  }

  // 未配置 API Key 时强制跳到设置
  if (!config.model.apiKey) {
    return <SettingsView />
  }

  return (
    <div className="app">
      {view === 'chat' && <ChatView onOpenSettings={() => setView('settings')} onOpenPersona={() => setView('persona')} />}
      {view === 'settings' && <SettingsView />}
      {view === 'persona' && <PersonaView onDone={() => setView('chat')} />}
    </div>
  )
}
