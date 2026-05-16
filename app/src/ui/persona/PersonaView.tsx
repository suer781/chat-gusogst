import { useState, useEffect } from 'react'
import { useSettingsStore } from '../stores'
import type { Persona } from '../types'
import { Plus, Search, ChevronRight } from 'lucide-react'
import { t, onLangChange } from '../i18n'

export function PersonaView({ onDone }: { onDone: () => void }) {
  const [search, setSearch] = useState('')
  const [, forceUpdate] = useState(0)
  const setPersona = useSettingsStore((s) => s.setPersona)
  const current = useSettingsStore((s) => s.config.persona)

  useEffect(() => onLangChange(() => forceUpdate((n) => n + 1)), [])

  const PRESETS: Persona[] = [
    { id: 'default', name: 'Hermes', systemPrompt: t('persona.hermes.desc'), tags: ['general'] },
    { id: 'creative', name: 'Muse', systemPrompt: t('persona.muse.desc'), tags: ['creative', 'writing'] },
    { id: 'coder', name: 'Hephaestus', systemPrompt: t('persona.hephaestus.desc'), tags: ['coding', 'technical'] },
    { id: 'analyst', name: 'Athena', systemPrompt: t('persona.athena.desc'), tags: ['analysis', 'strategy'] },
    { id: 'tutor', name: 'Socrates', systemPrompt: t('persona.socrates.desc'), tags: ['education', 'learning'] },
    { id: 'friend', name: 'Companion', systemPrompt: t('persona.companion.desc'), tags: ['casual', 'support'] },
  ]

  const filtered = PRESETS.filter((p) =>
    !search || p.name.toLowerCase().includes(search.toLowerCase()) || p.tags.some((t) => t.includes(search.toLowerCase()))
  )

  const select = (persona: Persona) => { setPersona(persona); onDone() }

  return (
    <div className="h-full flex flex-col" style={{ background: '#0f0f23' }}>
      <div style={{ padding: '12px 16px', borderBottom: '1px solid #1a1a3a' }}>
        <div className="relative">
          <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2" style={{ color: '#666688' }} />
          <input type="text" placeholder={t('persona.search')} value={search} onChange={(e) => setSearch(e.target.value)} className="w-full outline-none" style={{ background: '#1a1a3a', border: '1px solid #2a2a4a', borderRadius: 10, padding: '8px 12px 8px 36px', fontSize: 14, color: '#e0e0e0' }} />
        </div>
      </div>
      <div className="flex-1 overflow-y-auto" style={{ padding: '12px 16px' }}>
        {filtered.map((p) => (
          <button key={p.id} onClick={() => select(p)} className="w-full flex items-center gap-3" style={{ background: current.id === p.id ? '#e9456010' : '#1a1a3a', border: '1px solid ' + (current.id === p.id ? '#e9456040' : '#2a2a4a'), borderRadius: 12, padding: '12px 16px', marginBottom: 8, cursor: 'pointer', color: '#e0e0e0', textAlign: 'left' }}>
            <div className="flex items-center justify-center rounded-full shrink-0" style={{ width: 40, height: 40, background: current.id === p.id ? '#e9456020' : '#2a2a4a', color: current.id === p.id ? '#e94560' : '#8888aa', fontSize: 16, fontWeight: 600 }}>{p.name[0]}</div>
            <div className="flex-1 min-w-0">
              <div style={{ fontSize: 14, fontWeight: 500 }}>{p.name}</div>
              <div style={{ fontSize: 12, color: '#666688', marginTop: 2, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{p.systemPrompt}</div>
              <div className="flex gap-1 mt-1">
                {p.tags.map((tag) => <span key={tag} style={{ fontSize: 10, color: '#8888aa', background: '#2a2a4a', borderRadius: 4, padding: '1px 6px' }}>{tag}</span>)}
              </div>
            </div>
            <ChevronRight size={16} className="shrink-0" style={{ color: '#666688' }} />
          </button>
        ))}
        <button className="w-full flex items-center justify-center gap-2" style={{ background: 'transparent', border: '1px dashed #2a2a4a', borderRadius: 12, padding: 16, cursor: 'pointer', color: '#666688', fontSize: 14, marginTop: 4 }}>
          <Plus size={16} /> {t('persona.create')}
        </button>
      </div>
    </div>
  )
}
