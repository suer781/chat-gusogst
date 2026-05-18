import { useState } from 'react'
import { useSettingsStore, DEFAULT_EYE_CARE_MAPPINGS, genMappingId } from '../stores'
import { EyeCareColorMapper } from './EyeCareColorMapper'
import { Sun, Moon, Monitor, Eye, Droplets, Type, Palette } from 'lucide-react'
import { t } from '../i18n'

type ThemeMode = 'system' | 'light' | 'dark' | 'pureWhite' | 'pureBlack'

const THEME_OPTIONS: { key: ThemeMode; icon: typeof Sun; labelKey: string }[] = [
  { key: 'system', icon: Monitor, labelKey: 'settings.basic.system' as const },
  { key: 'light', icon: Sun, labelKey: 'settings.basic.light' as const },
  { key: 'dark', icon: Moon, labelKey: 'settings.basic.dark' as const },
  { key: 'pureWhite', icon: Sun, labelKey: 'settings.basic.pureWhite' as const },
  { key: 'pureBlack', icon: Moon, labelKey: 'settings.basic.pureBlack' as const },
]

const FONT_SIZES = [12, 13, 14, 15, 16, 17, 18, 20, 22]


export function BasicSettings({ onBack }: { onBack: () => void }) {
  const themeMode = useSettingsStore((s) => s.themeMode)
  const setThemeMode = useSettingsStore((s) => s.setThemeMode)
  const fontSize = useSettingsStore((s) => s.fontSize)
  const setFontSize = useSettingsStore((s) => s.setFontSize)
  const eyeCareEnabled = useSettingsStore((s) => s.eyeCareEnabled)
  const setEyeCareEnabled = useSettingsStore((s) => s.setEyeCareEnabled)
  const eyeCareColors = useSettingsStore((s) => s.eyeCareColors)
  const eyeCareIntensity = useSettingsStore((s) => s.eyeCareIntensity)
  const setEyeCareColors = useSettingsStore((s) => s.setEyeCareColors)
  const setEyeCareIntensity = useSettingsStore((s) => s.setEyeCareIntensity)
  const glassEnabled = useSettingsStore((s) => s.glassEnabled)
  const setGlassEnabled = useSettingsStore((s) => s.setGlassEnabled)
  const glassOpacity = useSettingsStore((s) => s.glassOpacity)
  const setGlassOpacity = useSettingsStore((s) => s.setGlassOpacity)

  const [showEyeCareDetail, setShowEyeCareDetail] = useState(false)

  return (
    <div style={{ minHeight: '100%', background: 'var(--bg-primary)', padding: '0 0 100px' }}>
      <div style={{
        display: 'flex', alignItems: 'center', gap: 12,
        padding: '16px 20px', position: 'sticky', top: 0,
        background: 'var(--bg-overlay)', backdropFilter: 'blur(20px)',
        zIndex: 10, borderBottom: '1px solid rgba(255,255,255,0.06)',
      }}>
        <button onClick={onBack} style={{
          background: 'none', border: 'none', color: 'var(--accent)',
          fontSize: 20, cursor: 'pointer', padding: 4,
          display: 'flex', alignItems: 'center',
        }}>←</button>
        <span style={{ fontSize: 18, fontWeight: 600, color: 'var(--text-primary)' }}>{t('settings.basic.pageTitle')}</span>
      </div>

      <Section title="主题模式" icon={<Palette size={18} />}>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(5, 1fr)', gap: 8 }}>
          {THEME_OPTIONS.map(({ key, icon: Icon, labelKey }) => (
            <button key={key} onClick={() => setThemeMode(key)} style={{
              display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 6,
              padding: '14px 4px', borderRadius: 14,
              background: themeMode === key ? 'rgba(233,69,96,0.15)' : 'rgba(255,255,255,0.04)',
              border: themeMode === key ? '1.5px solid rgba(233,69,96,0.5)' : '1.5px solid transparent',
              color: themeMode === key ? 'var(--accent)' : 'var(--gray-300)',
              cursor: 'pointer', transition: 'all 0.25s cubic-bezier(0.4,0,0.2,1)',
            }}>
              <Icon size={22} />
              <span style={{ fontSize: 11, fontWeight: themeMode === key ? 600 : 400 }}>{t(labelKey)}</span>
            </button>
          ))}
        </div>
      </Section>

      <Section title={t('settings.basic.fontSize')} icon={<Type size={18} />}> 
        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          <span style={{ color: 'var(--gray-400)', fontSize: 12, minWidth: 20 }}>A</span>
          <div style={{ flex: 1 }}>
            <input type="range" min={0} max={FONT_SIZES.length - 1}
              value={FONT_SIZES.indexOf(fontSize)}
              onChange={(e) => setFontSize(FONT_SIZES[Number(e.target.value)])}
              style={{ width: '100%', accentColor: 'var(--accent)', height: 4, touchAction: 'pan-y' }} />
            <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: 4, padding: '0 2px' }}>
              {FONT_SIZES.map((s) => (
                <div key={s} style={{
                  width: s === fontSize ? 8 : 4, height: s === fontSize ? 8 : 4,
                  borderRadius: '50%', transition: 'all 0.2s',
                  background: s === fontSize ? 'var(--accent)' : 'rgba(255,255,255,0.15)',
                }} />
              ))}
            </div>
          </div>
          <span style={{ color: 'var(--gray-300)', fontSize: 20, fontWeight: 600, minWidth: 30, textAlign: 'right' }}>{fontSize}</span>
        </div>
      </Section>

      <Section title={t('settings.basic.eyeCare')} icon={<Eye size={18} />}> 
        <ToggleRow label={t('settings.basic.eyeCareToggle')} desc={t('settings.basic.eyeCareDesc')}
          checked={eyeCareEnabled} onChange={setEyeCareEnabled} />
        {eyeCareEnabled && (
          <div style={{ marginTop: 12 }}>
            <button onClick={() => setShowEyeCareDetail(!showEyeCareDetail)} style={{
              width: '100%', padding: '10px 14px',
              background: 'rgba(255,255,255,0.04)', border: '1px solid rgba(255,255,255,0.08)',
              borderRadius: 10, color: 'var(--gray-200)', fontSize: 13,
              cursor: 'pointer', textAlign: 'left',
              display: 'flex', justifyContent: 'space-between', alignItems: 'center',
            }}>
              <span>{t('settings.basic.customMapping')}</span>
              <span style={{ transform: showEyeCareDetail ? 'rotate(90deg)' : 'rotate(0)', transition: 'transform 0.2s' }}>›</span>
            </button>
          <EyeCareColorMapper
            mappings={eyeCareColors}
            intensity={eyeCareIntensity}
            onMappingsChange={setEyeCareColors}
            onIntensityChange={setEyeCareIntensity}
          />
          </div>
        )}
      </Section>

      <Section title={t('settings.basic.glassTitle')} icon={<Droplets size={18} />}> 
        <ToggleRow label={t('settings.basic.glassLabel')} desc={t('settings.basic.glassDesc')}
          checked={glassEnabled} onChange={setGlassEnabled} />
      </Section>
    </div>
  )
}

function Section({ title, icon, children }: { title: string; icon?: React.ReactNode; children: React.ReactNode }) {
  return (
    <div style={{ margin: '16px 16px 0', background: 'rgba(255,255,255,0.03)', borderRadius: 16, padding: '18px 16px', border: '1px solid rgba(255,255,255,0.05)' }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 14, color: 'var(--text-primary)', fontSize: 14, fontWeight: 600 }}>
        {icon}<span>{title}</span>
      </div>
      {children}
    </div>
  )
}

function ToggleRow({ label, desc, checked, onChange }: { label: string; desc?: string; checked: boolean; onChange: (v: boolean) => void }) {
  return (
    <div onClick={() => onChange(!checked)} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '10px 0', cursor: 'pointer' }}>
      <div style={{ flex: 1 }}>
        <div style={{ color: 'var(--gray-100)', fontSize: 14 }}>{label}</div>
        {desc && <div style={{ color: 'var(--gray-400)', fontSize: 12, marginTop: 2 }}>{desc}</div>}
      </div>
      <div style={{ width: 46, height: 26, borderRadius: 13, background: checked ? 'var(--accent)' : 'rgba(255,255,255,0.1)', position: 'relative', transition: 'background 0.25s', flexShrink: 0, marginLeft: 12 }}>
        <div style={{ width: 22, height: 22, borderRadius: 11, background: 'var(--text-primary)', position: 'absolute', top: 2, left: checked ? 22 : 2, transition: 'left 0.25s cubic-bezier(0.4,0,0.2,1)', boxShadow: '0 1px 3px rgba(0,0,0,0.3)' }} />
      </div>
    </div>
  )
}
/div>
        {desc && <div style={{ color: 'var(--gray-400)', fontSize: 12, marginTop: 2 }}>{desc}</div>}
      </div>
      <div style={{ width: 46, height: 26, borderRadius: 13, background: checked ? 'var(--accent)' : 'rgba(255,255,255,0.1)', position: 'relative', transition: 'background 0.25s', flexShrink: 0, marginLeft: 12 }}>
        <div style={{ width: 22, height: 22, borderRadius: 11, background: 'var(--text-primary)', position: 'absolute', top: 2, left: checked ? 22 : 2, transition: 'left 0.25s cubic-bezier(0.4,0,0.2,1)', boxShadow: '0 1px 3px rgba(0,0,0,0.3)' }} />
      </div>
    </div>
  )
}
