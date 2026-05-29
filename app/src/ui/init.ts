import { StatusBar, Style } from '@capacitor/status-bar'
import { SafeArea } from 'capacitor-plugin-safe-area'
import { SplashScreen } from '@capacitor/splash-screen'
import { Capacitor } from '@capacitor/core'

export async function initApp() {
  if (!Capacitor.isNativePlatform()) return

  try {
    await StatusBar.setStyle({ style: Style.Dark })
    await StatusBar.setBackgroundColor({ color: '#0f0f23' })
  } catch (e) {
    console.warn('StatusBar init failed:', e)
  }

  try {
    const { insets } = await SafeArea.getSafeAreaInsets()
    const root = document.documentElement
    root.style.setProperty('--sat', insets.top + 'px')
    root.style.setProperty('--sab', insets.bottom + 'px')
    root.style.setProperty('--sal', insets.left + 'px')
    root.style.setProperty('--sar', insets.right + 'px')
  } catch (e) {
    console.warn('SafeArea init failed:', e)
  }

  // Hide splash AFTER app is painted — seamless handoff
  try {
    await SplashScreen.hide({ fadeOutDuration: 0 })
  } catch (e) {
    console.warn('SplashScreen hide failed:', e)
  }
}
