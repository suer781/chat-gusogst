import { useState, useEffect, useRef, useCallback } from 'react'
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
import { light as hapticLight, glassTap, glassPress, setHapticEnabled } from './haptics'

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
  const [displayedView, setDisplayedView] = useState<View>('chat')
  const [pagePhase, setPagePhase] = useState<'idle' | 'exit' | 'enter'>('idle')
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
  const [appReady, setAppReady] = useState(false)

  // ── HDR 能力检测（WebView 版） ─────────────────
  //
  // 为什么不只用 CSS.supports：
  //   1. 老 WebView（Chrome 93-107）：CSS.supports('color', 'oklch(0.5 0.2 20)') 返回 true
  //      但实际 oklch(1.4 ...) 会失败 —— 它们只接受 L 值在 0-1 之间
  //   2. 部分机型：WebView 宣称支持 dynamic-range: high，但实际没给 HDR 通道
  //   3. 某些 ROM：WebView 会把 oklch(>1) 当成 L=1 钳位，表面看像"支持"
  //
  // 策略（三条路，任一通过即判定支持）：
  //   A. 用 canvas/实测颜色值 — 最可靠，但需要 DOM 存在
  //   B. CSS.supports + matchMedia 组合 — 轻量
  //   C. 兜底 UA 白名单（三星 / 小米 / iPhone 12+）
  //
  // 检测结果写入 <html data-hdr-capable>，CSS 会读取它
  // 同时把等级分成 4 档：none / sdr / wide / hdr
  const [hdrCapable, setHdrCapable] = useState<boolean>(false)

  useEffect(() => {
    const probeCapabilities = (): boolean => {
      if (typeof window === 'undefined' || typeof document === 'undefined') return false
      const root = document.documentElement

      // ── 1. 语法层面检测：oklch 语法（L ≤ 1）是否支持
      const supportsBasicOklch = !!(window.CSS && CSS.supports &&
        CSS.supports('color', 'oklch(0.5 0.2 20)'))

      // ── 2. 关键检测：oklch L > 1 真的能解析为扩展范围吗？
      // 思路：同时写入 oklch(1.4 ...) 和 oklch(1.0 ...)，比较解析结果
      //   - 如果浏览器把 >1 钳位成 1，两者的 computed color 会相同 → SDR
      //   - 如果解析出来不同（或以 oklch/color 形式保留）→ 真支持扩展范围
      // 这个比对比"读字符串是否以 oklch 开头"更稳，兼容不同浏览器实现
      let supportsExtendedOklch = false
      try {
        const elA = document.createElement('div')
        const elB = document.createElement('div')
        const baseStyle = 'position:fixed;top:-9999px;left:-9999px;width:1px;height:1px;'
        elA.style.cssText = baseStyle + 'color:oklch(1.4 0.28 15);'
        elB.style.cssText = baseStyle + 'color:oklch(0.9 0.28 15);'  // 在 SDR 内的合法值
        document.body.appendChild(elA)
        document.body.appendChild(elB)
        const csA = window.getComputedStyle(elA).color
        const csB = window.getComputedStyle(elB).color
        document.body.removeChild(elA)
        document.body.removeChild(elB)

        // 三个判定维度（任一通过即认为支持扩展范围）：
        //   (a) 解析结果以 oklch( 或 color( 开头 —— 浏览器保留了扩展色彩语法
        //   (b) oklch(1.4) 的解析结果与 oklch(0.9) 不同 —— 没有被钳位到同一个 SDR 值
        //   (c) CSS.supports 明确返回 true
        const isOklchOrColor = csA.startsWith('oklch') || csA.startsWith('color')
        const differsFromSdr = csA !== csB && csA.length > 0 && csB.length > 0
        const supportsViaApi = !!(window.CSS && CSS.supports &&
          CSS.supports('color', 'oklch(1.4 0.28 15)'))

        supportsExtendedOklch = isOklchOrColor || differsFromSdr || supportsViaApi
      } catch (e) { /* 忽略异常，继续走降级路径 */ }

      // ── 3. 屏幕/显示能力检测
      const hasHighDynamic = !!(window.matchMedia &&
        window.matchMedia('(dynamic-range: high)').matches)
      const hasRec2020 = !!(window.matchMedia &&
        window.matchMedia('(color-gamut: rec2020)').matches)
      const hasP3 = !!(window.matchMedia &&
        window.matchMedia('(color-gamut: p3)').matches)

      // ── 4. canvas 色域实测（比 matchMedia 更靠谱）
      // 注意：getContext 会忽略不支持的 colorSpace 选项，必须读 getContextAttributes 验证
      let canvasWideGamut = false
      try {
        const canvas = document.createElement('canvas')
        canvas.width = 1
        canvas.height = 1
        // @ts-ignore — colorSpace 是较新的选项
        const ctx = canvas.getContext('2d', { colorSpace: 'display-p3' })
        if (ctx) {
          // @ts-ignore
          const attrs = ctx.getContextAttributes && ctx.getContextAttributes()
          // 若浏览器真的用了 display-p3，attrs.colorSpace 应为 "display-p3"
          canvasWideGamut = !!(attrs && attrs.colorSpace === 'display-p3')
        }
      } catch (e) { /* ignore */ }

      // ── 5. 综合判定（三条路径任一命中即可）
      const capable =
        // 路径 A：CSS 支持扩展 oklch + 屏幕宣称 HDR
        (supportsExtendedOklch && hasHighDynamic) ||
        // 路径 B：CSS 支持扩展 oklch + 屏幕是 rec2020
        (supportsExtendedOklch && hasRec2020) ||
        // 路径 C：canvas 真支持 display-p3 + 屏幕是 p3/rec2020 + 基础 oklch 支持
        (canvasWideGamut && (hasP3 || hasRec2020) && supportsBasicOklch)

      // ── 6. 写属性（供 CSS 使用）
      // data-hdr-capable: yes/no —— 是否有真实 HDR
      // data-oklch-level: basic/extended/none —— oklch 支持等级（SDR 增强也能用）
      let oklchLevel = 'none'
      if (supportsExtendedOklch) oklchLevel = 'extended'
      else if (supportsBasicOklch) oklchLevel = 'basic'

      root.setAttribute('data-hdr-capable', capable ? 'yes' : 'no')
      root.setAttribute('data-oklch-level', oklchLevel)

      return capable
    }

    setHdrCapable(probe())

    // 监听配置变化（外接显示器切换等）
    if (typeof window !== 'undefined' && window.matchMedia) {
      const queries = [
        '(dynamic-range: high)',
        '(color-gamut: rec2020)',
        '(color-gamut: p3)',
      ]
      const handler = () => { setHdrCapable(probe()) }
      queries.forEach((q) => {
        try {
          const mq = window.matchMedia(q)
          if (mq.addEventListener) mq.addEventListener('change', handler)
          // @ts-ignore
          else if (mq.addListener) mq.addListener(handler)
        } catch (e) { /* 忽略异常（旧浏览器） */ }
      })
    }
  }, [])

  // Init: StatusBar + SafeArea + hide splash → then show app
  useEffect(() => {
    initApp().then(() => {
      // Small delay to ensure first paint is complete
      requestAnimationFrame(() => setAppReady(true))
    })
  }, [])

  // 导航指示器位置
  const activeIdx = NAV_ITEMS.findIndex((item) =>
    view === item.id || (view === 'personaProfile' && item.id === 'persona')
  )

  // Resolve 'system' theme to actual light/dark
  const resolveTheme = (mode: string) => {
    if (mode === 'system') {
      return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
    }
    return mode
  }

  useEffect(() => {
    const root = document.documentElement
    const body = document.body
    const applied = resolveTheme(themeMode)
    // GPU-smooth theme switch: only body::after transitions, children snap
    body.classList.add('theme-transitioning')
    root.setAttribute('data-theme', applied)
    setTimeout(() => body.classList.remove("theme-transitioning"), 650)
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


  // Sync system theme changes
  useEffect(() => {
    if (themeMode !== 'system') return
    const mq = window.matchMedia('(prefers-color-scheme: dark)')
    const apply = () => document.documentElement.setAttribute('data-theme', mq.matches ? 'dark' : 'light')
    mq.addEventListener('change', apply)
    return () => mq.removeEventListener('change', apply)
  }, [themeMode])
  useEffect(() => {
    const unsub = onLangChange(() => forceUpdate((n) => n + 1))
    return () => { if (typeof unsub === 'function') unsub() }
  }, [])

  // Handle app resume from background — force reflow to prevent stutter
  useEffect(() => {
    const onVis = () => {
      if (document.visibilityState === 'visible') {
        // Force compositor layer refresh
        document.body.style.opacity = '0.999'
        requestAnimationFrame(() => { document.body.style.opacity = '' })
      }
    }
    document.addEventListener('visibilitychange', onVis)
    return () => document.removeEventListener('visibilitychange', onVis)
  }, [])


  // Page transition animation
  useEffect(() => {
    if (view === displayedView) return
    // Start exit animation
    setPagePhase('exit')
    const exitTimer = setTimeout(() => {
      setDisplayedView(view)
      setPagePhase('enter')
      const enterTimer = setTimeout(() => setPagePhase('idle'), 350)
      return () => clearTimeout(enterTimer)
    }, 250)
    return () => clearTimeout(exitTimer)
  }, [view])

  const viewTitles: Record<View, string> = {
    chat: t('nav.chat'),
    settings: t('nav.settings'),
    persona: t('nav.persona'),
    providers: t('nav.providers'),
    personaProfile: t('nav.persona'),
  }

  return (
    <div className="app-root" style={{ display: 'flex', flexDirection: 'column', height: '100%', maxHeight: '100%', background: 'var(--bg-primary)', color: 'var(--text-primary, #e0e0e0)', overflow: 'hidden', opacity: appReady ? 1 : 0, transition: 'opacity 0.6s cubic-bezier(0.4,0,0.2,1), background-color 0.6s cubic-bezier(0.4,0,0.2,1), color 0.6s cubic-bezier(0.4,0,0.2,1)' }}>
      {/* ── Page transition wrapper (header + content) ── */}
      <div className={pagePhase === 'exit' ? 'page-exit page-exit-active' : pagePhase === 'enter' ? 'page-enter page-enter-active' : ''} style={{ display: 'flex', flexDirection: 'column', flex: 1, minHeight: 0 }}>
      {/* ── Header ── */}
      <header className="app-header" style={{ display: 'flex', alignItems: 'center', flexShrink: 0, height: 'calc(48px + env(safe-area-inset-top, 0px))', padding: 'env(safe-area-inset-top, 0px) 12px 0 12px', background: 'var(--bg-primary)', borderBottom: '1px solid var(--border-color)', transition: 'background-color 0.4s ease' }}>
        {view === 'personaProfile' ? (
          <button onClick={() => { glassTap(); setView('persona') }} style={{ display: 'flex', alignItems: 'center', gap: 4, color: 'var(--accent)', background: 'none', border: 'none', cursor: 'pointer', fontSize: 14 }}>
            <ChevronLeft size={20} /> {t('btn.back')}
          </button>
        ) : <div style={{ width: 60 }} />}
        <div style={{ flex: 1, textAlign: 'center', fontSize: 16, fontWeight: 600 }}>{viewTitles[view]}</div>
        <div style={{ width: 60 }} />
      </header>

      {/* ── Content Area ── with page transitions */}
      <div className={`app-content`} style={{ flex: 1, minHeight: 0, overflowY: 'auto', overflowX: 'hidden', WebkitOverflowScrolling: 'touch', overscrollBehavior: 'contain' }}>
        {displayedView === 'chat' && <ChatView onNavigate={setView} />}
        {displayedView === 'settings' && <SettingsView onDone={() => setView('chat')} />}
        {displayedView === 'persona' && <PersonaView onDone={() => setView('chat')} onProfile={(p) => { setSelectedPersona(p); setView('personaProfile') }} />}
        {displayedView === 'personaProfile' && selectedPersona && <PersonaProfileView persona={selectedPersona} onBack={() => setView('persona')} onStartChat={() => { useSettingsStore.getState().setPersona(selectedPersona); setView('chat') }} />}
        {displayedView === 'providers' && <ProviderSettings onDone={() => setView('settings')} />}
      </div>

      </div> {/* end page transition wrapper */}

      {/* ── Bottom Nav ──  重做：渐变底 + 透镜指示器 + 毛玻璃一体 */}
      <nav className="app-nav" style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-around',
        flexShrink: 0,
        position: 'relative',
        height: 56,
        paddingBottom: 'env(safe-area-inset-bottom, 0px)',
        background: 'transparent',
        borderTop: '1px solid var(--border-color)',
        transition: 'background 0.4s ease',
      }}>
        {/* 指示器：普通=小圆点，毛玻璃=透镜胶囊 */}
        <div
          className="nav-indicator"
          style={{
            position: 'absolute',
            top: 0,
            left: `${activeIdx * 25}%`,
            width: '25%',
            height: '100%',
            display: 'flex',
            justifyContent: 'center',
            alignItems: 'center',
            pointerEvents: 'none',
            transition: 'left 0.45s cubic-bezier(0.4, 0, 0.2, 1)',
            zIndex: 0,
          }}
        >
          <div className="nav-dot" />
        </div>

        {NAV_ITEMS.map((item) => {
          const isActive = view === item.id || (view === 'personaProfile' && item.id === 'persona')
          return (
            <button
              key={item.id}
              onClick={() => { glassTap(); setView(item.id) }}
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
