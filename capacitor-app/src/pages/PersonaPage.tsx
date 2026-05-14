import { usePersona } from '../hooks/useStore'
import { PRESETS } from '../lib/types'
import './PersonaPage.css'

export default function PersonaPage() {
  const { persona, savePersona } = usePersona()

  return (
    <div className="persona-page">
      <header className="persona-header">
        <span>选择人设</span>
      </header>
      <div className="persona-list">
        {PRESETS.map(p => (
          <div
            key={p.id}
            className={'persona-card' + (persona.id === p.id ? ' selected' : '')}
            onClick={() => savePersona(p)}
          >
            <span className="persona-avatar">{p.avatar}</span>
            <div className="persona-info">
              <span className="persona-name">{p.name}</span>
              <span className="persona-desc">{p.systemPrompt.slice(0, 40)}...</span>
            </div>
            {persona.id === p.id && <span className="check">✓</span>}
          </div>
        ))}
      </div>
    </div>
  )
}