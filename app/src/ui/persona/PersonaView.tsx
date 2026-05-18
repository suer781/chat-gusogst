import { useState, useEffect } from 'react'
import { useSettingsStore } from '../stores'
import type { Persona } from '../types'
import { Plus, Search, ChevronRight } from 'lucide-react'
import { t, onLangChange } from '../i18n'

export function PersonaView({ onDone, onProfile }: { onDone: () => void; onProfile?: (p: Persona) => void }) {
  const [search, setSearch] = useState('')
  const [, forceUpdate] = useState(0)
  const setPersona = useSettingsStore((s) => s.setPersona)
  const current = useSettingsStore((s) => s.persona)

  useEffect(() => { onLangChange(() => forceUpdate((n) => n + 1)); }, [])

  const PRESETS: Persona[] = [
    { id: 'default', name: 'Hermes', systemPrompt: t('persona.hermes.desc'), tags: ['general'], emoji: '🌿', personality: { calm: 0.8, warm: 0.6, analytical: 0.5 } },
    { id: 'creative', name: 'Muse', systemPrompt: t('persona.muse.desc'), tags: ['creative', 'writing'], emoji: '🎨', personality: { creative: 0.9, curious: 0.7, playful: 0.5 } },
    { id: 'coder', name: 'Hephaestus', systemPrompt: t('persona.hephaestus.desc'), tags: ['coding', 'technical'], emoji: '⚒️', personality: { precise: 0.9, analytical: 0.8, calm: 0.6 } },
    { id: 'analyst', name: 'Athena', systemPrompt: t('persona.athena.desc'), tags: ['analysis', 'strategy'], emoji: '🦉', personality: { analytical: 0.9, precise: 0.7, calm: 0.7 } },
    { id: 'tutor', name: 'Socrates', systemPrompt: t('persona.socrates.desc'), tags: ['education', 'learning'], emoji: '📚', personality: { curious: 0.9, warm: 0.7, calm: 0.6 } },
    { id: 'friend', name: 'Companion', systemPrompt: t('persona.companion.desc'), tags: ['casual', 'support'], emoji: '💛', personality: { warm: 0.9, playful: 0.6, energetic: 0.5 } },
  ]

  const filtered = PRESETS.filter((p) =>
    !search || p.name.toLowerCase().includes(search.toLowerCase()) || p.tags.some((t) => t.includes(search.toLowerCase()))
  )

  const select = (persona: Persona) => { if (onProfile) { onProfile(persona) } else { setPersona(persona); onDone() } }

  return (
    <div className="h-full flex flex-col" style={{ background: 'var(--bg-primary)' }}>
      <div style={{ padding: '12px 16px', borderBottom: '1px solid #1a1a3a' }}>
        <div className="relative">
          <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2" style={{ color: 'var(--text-secondary)' }} />
          <input type="text" placeholder={t('persona.search')} value={search} onChange={(e) => setSearch(e.target.value)} className="w-full outline-none" style={{ background: 'var(--bg-secondary)', border: '1px solid var(--border)', borderRadius: "var(--radius-md)", padding: '8px 12px 8px 36px', fontSize: "var(--text-base)", color: 'var(--text-primary)' }} />
        </div>
      </div>
      <div className="flex-1 overflow-y-auto" style={{ padding: "12px 16px", overscrollBehavior: "contain" }}>
        {filtered.map((p) => (
          <button key={p.id} onClick={() => select(p)} className="w-full flex items-center gap-3" style={{ background: current.id === p.id ? 'var(--accent-soft)' : 'var(--bg-tertiary)', border: '1px solid ' + (current.id === p.id ? 'var(--accent-glow)' : 'var(--border)'), borderRadius: "var(--radius-md)", padding: '12px 16px', marginBottom: 8, cursor: 'pointer', color: 'var(--text-primary)', textAlign: 'left' }}>
            <div className="flex items-center justify-center rounded-full shrink-0" style={{ width: 40, height: 40, background: current.id === p.id ? 'var(--accent-soft)' : 'var(--border)', color: current.id === p.id ? 'var(--accent)' : 'var(--gray-300)', fontSize: "var(--text-lg)", fontWeight: 600 }}>{p.name[0]}</div>
            <div className="flex-1 min-w-0">
              <div style={{ fontSize: "var(--text-base)", fontWeight: 500 }}>{p.name}</div>
              <div style={{ fontSize: "var(--text-sm)", color: 'var(--text-secondary)', marginTop: 2, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{p.systemPrompt}</div>
              <div className="flex gap-1 mt-1">
                {p.tags.map((tag) => <span key={tag} style={{ fontSize: 10, color: 'var(--text-secondary)', background: 'var(--border)', borderRadius: 4, padding: '1px 6px' }}>{tag}</span>)}
              </div>
            </div>
            <ChevronRight size={16} className="shrink-0" style={{ color: 'var(--text-secondary)' }} />
          </button>
        ))}
        <button onClick={() => { const custom = { id: 'custom-' + Date.now(), name: 'Custom', systemPrompt: 'You are a helpful assistant.', tags: ['custom'], emoji: '\u{1F3AD}' }; if (onProfile) { onProfile(custom) } else { setPersona(custom); onDone() } }} className="w-full flex items-center justify-center gap-2" style={{ background: 'transparent', border: '1px dashed var(--border-color)', borderRadius: "var(--radius-md)", padding: 16, cursor: 'pointer', color: 'var(--text-secondary)', fontSize: "var(--text-base)", marginTop: 4 }}>
          <Plus size={16} /> {t('persona.create')}
        </button>
      </div>
    </div>
  )
}
