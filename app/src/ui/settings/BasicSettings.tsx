import { useState, useEffect } from 'react'
import { useSettingsStore, DEFAULT_EYE_CARE_MAPPINGS, genMappingId } from '../stores'
import { EyeCareColorMapper } from './EyeCareColorMapper'
import { Sun, Moon, Monitor, Eye, Droplets, Type, Palette, Smartphone } from 'lucide-react'
import { t } from '../i18n'
import { medium as hapticMedium, light as hapticLight, glassTap, glassPress, selectionStart, selectionChangedThrottled, selectionEnd } from '../haptics'

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
  const glassTier = useSettingsStore((s) => s.glassTier)
  const setGlassTier = useSettingsStore((s) => s.setGlassTier)
  const performanceHint = useSettingsStore((s) => s.performanceHint)
  const hapticEnabled = useSettingsStore((s) => s.hapticEnabled)
  const setHapticEnabled = useSettingsStore((s) => s.setHapticEnabled)
  const hdrEnabled = useSettingsStore((s) => s.hdrEnabled)
  const setHdrEnabled = useSettingsStore((s) => s.setHdrEnabled)
  const glassOpacity = useSettingsStore((s) => s.glassOpacity)
  const setGlassOpacity = useSettingsStore((s) => s.setGlassOpacity)

  // 内联背景模糊级别（跟随当前生效的 tier）
  const headerBlur = glassEnabled && glassTier !== 'off'
    ? (glassTier === 'light' ? 'blur(8px)' : 'blur(16px)')
    : 'none'

  // 复用 App.tsx 已经写入 <html data-hdr-capable> 的检测结果
  const [hdrCapable, setHdrCapable] = useState<boolean>(() => {
    if (typeof document === 'undefined') return false
    return document.documentElement.getAttribute('data-hdr-capable') === 'yes'
  })

  const [showEyeCareDetail, setShowEyeCareDetail] = useState(false)

  // 如果 App.tsx 的检测还没完成，监听一下变化
  useEffect(() => {
    const update = () => {
      setHdrCapable(
        document.documentElement.getAttribute('data-hdr-capable') === 'yes'
      )
    }
    update()
    const id = setInterval(update, 500)
    setTimeout(() => clearInterval(id), 3000)
    return () => clearInterval(id)
  }, [])

  return (
    <div style={{ minHeight: '100%', background: 'var(--bg-primary)', padding: '0 0 100px' }}>
      <div style={{
        display: 'flex', alignItems: 'center', gap: 12,
        padding: '16px 20px', position: 'sticky', top: 0,
        background: 'var(--bg-overlay)', backdropFilter: headerBlur,
        zIndex: 10, borderBottom: '1px solid rgba(255,255,255,0.06)',
      }}>
        <button onClick={() => { glassTap(); onBack() }} style={{
          background: 'none', border: 'none', color: 'var(--accent)',
          fontSize: 20, cursor: 'pointer', padding: 4,
          display: 'flex', alignItems: 'center',
        }}>←</button>
        <span style={{ fontSize: 18, fontWeight: 600, color: 'var(--text-primary)' }}>{t('settings.basic.pageTitle')}</span>
      </div>

      <Section title="主题模式" icon={<Palette size={18} />}>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(5, 1fr)', gap: 8 }}>
          {THEME_OPTIONS.map(({ key, icon: Icon, labelKey }) => (
            <button key={key} onClick={() => { glassPress(); setThemeMode(key) }} style={{
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
              onPointerDown={() => selectionStart()}
              onChange={(e) => { selectionChangedThrottled(); setFontSize(FONT_SIZES[Number(e.target.value)]) }}
              onPointerUp={() => selectionEnd()}
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
            <button onClick={() => { glassTap(); setShowEyeCareDetail(!showEyeCareDetail) }} style={{
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

        {/* 毛玻璃性能分层选择器 */}
        {glassEnabled && (
          <div style={{ marginTop: 14 }}>
            <div style={{ color: 'var(--text-secondary)', fontSize: 12, marginBottom: 8, fontWeight: 600 }}>
              毛玻璃效果等级
            </div>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 7 }}>
              {([
                { key: 'auto' as const, label: '自动', desc: performanceHint || '检测中…', icon: '🔄' },
                { key: 'full' as const, label: '完整', desc: '最佳效果', icon: '✨' },
                { key: 'light' as const, label: '轻量', desc: '折中方案', icon: '🌤' },
                { key: 'off' as const, label: '关闭', desc: '最省资源', icon: '🔋' },
              ]).map(({ key, label, desc, icon }) => {
                const isActive = glassTier === key
                    // "关闭" 用绿色系，其他用品牌色系
                    const isOff = key === 'off'
                    return (
                      <button
                        key={key}
                        onClick={() => {
                          hapticMedium()
                          setGlassTier(key)
                        }}
                        style={{
                          padding: '10px 6px',
                          borderRadius: 12,
                          fontSize: 12,
                          fontWeight: isActive ? 700 : 500,
                          color: isActive
                            ? (isOff ? '#10b981' : 'var(--accent)')
                            : 'var(--text-secondary)',
                          background: isActive
                            ? (isOff
                              ? 'rgba(16,185,129,0.12)'
                              : 'rgba(233,69,96,0.12)')
                            : 'var(--bg-tertiary)',
                          border: isActive
                            ? (isOff
                              ? '1.5px solid rgba(16,185,129,0.45)'
                              : '1.5px solid rgba(233,69,96,0.45)')
                            : '1px solid var(--border)',
                      cursor: 'pointer',
                      display: 'flex',
                      flexDirection: 'column',
                      alignItems: 'center',
                      gap: 3,
                      transition: 'all 0.2s cubic-bezier(0.4, 0, 0.2, 1)',
                      lineHeight: 1.25,
                      position: 'relative',
                      overflow: 'hidden',
                    }}
                  >
                    {/* 选中指示条 */}
                    {isActive && (
                      <div style={{
                        position: 'absolute', top: 0, left: '50%', transform: 'translateX(-50%)',
                        width: 20, height: 2.5, borderRadius: 2,
                        background: isOff ? '#10b981' : 'var(--accent)',
                      }} />
                    )}
                    <span style={{ fontSize: 14 }}>{icon}</span>
                    <span>{label}</span>
                    <span style={{ fontSize: 9.5, opacity: isActive ? 0.85 : 0.55 }}>{desc}</span>
                  </button>
                )
              })}
            </div>
            {/* 自动模式下的性能检测结果 */}
            {performanceHint && glassTier === 'auto' && (
              <div style={{
                marginTop: 8,
                padding: '8px 12px',
                borderRadius: 10,
                background: 'rgba(233,69,96,0.06)',
                border: '1px solid rgba(233,69,96,0.15)',
                fontSize: 11,
                color: 'var(--text-secondary)',
                lineHeight: 1.45,
                display: 'flex',
                alignItems: 'center',
                gap: 6,
              }}>
                <span>📊</span>
                <span>自动检测：<strong style={{ color: 'var(--text-primary)' }}>{performanceHint}</strong></span>
              </div>
            )}
          </div>
        )}
      </Section>

      <Section title="触觉反馈" icon={<Smartphone size={18} />}>
        <ToggleRow label="震动反馈" desc="按钮点击时的线性马达触感反馈（需设备支持）"
          checked={hapticEnabled} onChange={setHapticEnabled} />
      </Section>

      <Section title="HDR 渲染" icon={<Sun size={18} />}>
        <ToggleRow
          label="HDR 玻璃质感"
          desc={describeHdrLevel(hdrCapable, readOklchLevel())}
          checked={hdrEnabled}
          onChange={setHdrEnabled}
        />
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

// ── 辅助函数：根据 JS 实测的能力等级返回设置页描述 ──
function readOklchLevel(): string {
  if (typeof document === 'undefined') return 'none'
  return document.documentElement.getAttribute('data-oklch-level') || 'none'
}

function describeHdrLevel(hdrCapable: boolean, oklchLevel: string): string {
  if (hdrCapable && oklchLevel === 'extended') {
    return '★ 您的设备支持真 HDR，将使用超亮高光色渲染（边缘会有明显发光）'
  }
  if (oklchLevel === 'extended' || oklchLevel === 'basic') {
    return '您的设备支持宽色域（Display P3），当前以高色准 SDR 增强效果渲染（屏幕峰值亮度受限）'
  }
  return '您的设备/WebView 不支持 oklch / 宽色域，当前以标准 rgba 颜色回退渲染'
}

function ToggleRow({ label, desc, checked, onChange }: { label: string; desc?: string; checked: boolean; onChange: (v: boolean) => void }) {
  return (
    <div onClick={() => { hapticMedium(); onChange(!checked) }} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '10px 0', cursor: 'pointer' }}>
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
