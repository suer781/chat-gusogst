import { useState, useEffect } from 'react'
import { useSettingsStore } from '../stores'
import { ChevronRight, Brain, Globe, Sliders, Languages } from 'lucide-react'
import { t, onLangChange, type Lang } from '../i18n'

export function SettingsView({ onDone }: { onDone: () => void }) {
  const config = useSettingsStore((s) => s.config)
  const language = useSettingsStore((s) => s.language)
  const setSearchEnabled = useSettingsStore((s) => s.setSearchEnabled)
  const setMemoryEnabled = useSettingsStore((s) => s.setMemoryEnabled)
  const setTemperature = useSettingsStore((s) => s.setTemperature)
  const setMaxTokens = useSettingsStore((s) => s.setMaxTokens)
  const setLanguage = useSettingsStore((s) => s.setLanguage)
  const [showAdv, setShowAdv] = useState(false)
  const [, forceUpdate] = useState(0)

  useEffect(() => onLangChange(() => forceUpdate((n) => n + 1)), [])

  return (
    <div className="h-full overflow-y-auto" style={{ background: '#0f0f23' }}>
      <div style={{ padding: 16 }}>

        {/* Language */}
        <div style={{ marginBottom: 24 }}>
          <div style={{ fontSize: 12, fontWeight: 600, color: '#666688', marginBottom: 8, textTransform: 'uppercase', letterSpacing: '0.05em' }}>{t('settings.language')}</div>
          <div className="flex gap-2">
            {([['zh', '中文'], ['en', 'English']] as [Lang, string][]).map(([lang, label]) => (
              <button key={lang} onClick={() => setLanguage(lang)} className="flex-1 flex items-center justify-center gap-2" style={{
                background: language === lang ? '#e9456015' : '#1a1a3a',
                border: '1px solid ' + (language === lang ? '#e94560' : '#2a2a4a'),
                borderRadius: 12, padding: '10px 16px', cursor: 'pointer',
                color: language === lang ? '#e94560' : '#e0e0e0', fontSize: 14, fontWeight: language === lang ? 600 : 400,
              }}>
                <Languages size={16} />
                {label}
              </button>
            ))}
          </div>
        </div>

        {/* Toggles */}
        <div style={{ marginBottom: 24 }}>
          <div style={{ fontSize: 12, fontWeight: 600, color: '#666688', marginBottom: 8, textTransform: 'uppercase', letterSpacing: '0.05em' }}>{t('settings.features')}</div>
          <div style={{ background: '#1a1a3a', border: '1px solid #2a2a4a', borderRadius: 12, overflow: 'hidden' }}>
            <ToggleRow icon={<Brain size={18} />} label={t('settings.memory')} desc={t('settings.memoryDesc')} checked={config.memoryEnabled} onChange={setMemoryEnabled} />
            <div style={{ borderTop: '1px solid #2a2a4a' }} />
            <ToggleRow icon={<Globe size={18} />} label={t('settings.search')} desc={t('settings.searchDesc')} checked={config.searchEnabled} onChange={setSearchEnabled} />
          </div>
        </div>

        {/* Advanced */}
        <div style={{ marginBottom: 24 }}>
          <button onClick={() => setShowAdv(!showAdv)} className="w-full flex items-center justify-between" style={{ fontSize: 12, fontWeight: 600, color: '#666688', textTransform: 'uppercase', letterSpacing: '0.05em', background: 'none', border: 'none', cursor: 'pointer', padding: '0 0 8px 0' }}>
            <span className="flex items-center gap-2"><Sliders size={14} /> {t('settings.advanced')}</span>
            <ChevronRight size={14} style={{ transform: showAdv ? 'rotate(90deg)' : 'none', transition: 'transform 0.2s' }} />
          </button>
          {showAdv && (
            <div style={{ background: '#1a1a3a', border: '1px solid #2a2a4a', borderRadius: 12, padding: 16 }}>
              <SliderRow label={t('settings.temperature')} value={config.model.temperature} min={0} max={2} step={0.1} onChange={setTemperature} />
              <SliderRow label={t('settings.maxTokens')} value={config.model.maxTokens} min={256} max={16384} step={256} onChange={setMaxTokens} />
            </div>
          )}
        </div>

      </div>
    </div>
  )
}

function ToggleRow({ icon, label, desc, checked, onChange }: { icon: React.ReactNode; label: string; desc: string; checked: boolean; onChange: (v: boolean) => void }) {
  return (
    <div className="flex items-center justify-between" style={{ padding: '12px 16px' }}>
      <div className="flex items-center gap-3">
        <div style={{ color: '#8888aa' }}>{icon}</div>
        <div>
          <div style={{ fontSize: 14, fontWeight: 500 }}>{label}</div>
          <div style={{ fontSize: 12, color: '#666688' }}>{desc}</div>
        </div>
      </div>
      <button onClick={() => onChange(!checked)} style={{ width: 44, height: 24, borderRadius: 12, border: 'none', cursor: 'pointer', background: checked ? '#e94560' : '#2a2a4a', position: 'relative', transition: 'background 0.2s' }}>
        <div style={{ width: 18, height: 18, borderRadius: '50%', background: '#fff', position: 'absolute', top: 3, left: checked ? 23 : 3, transition: 'left 0.2s' }} />
      </button>
    </div>
  )
}

function SliderRow({ label, value, min, max, step, onChange }: { label: string; value: number; min: number; max: number; step: number; onChange: (v: number) => void }) {
  return (
    <div style={{ marginBottom: 16 }}>
      <div className="flex items-center justify-between" style={{ marginBottom: 6 }}>
        <span style={{ fontSize: 13, color: '#e0e0e0' }}>{label}</span>
        <span style={{ fontSize: 13, color: '#e94560', fontWeight: 500 }}>{value}</span>
      </div>
      <input type="range" min={min} max={max} step={step} value={value} onChange={(e) => onChange(Number(e.target.value))} className="w-full" style={{ accentColor: '#e94560' }} />
    </div>
  )
}
