import { StatusBar, Style } from '@capacitor/status-bar'
import { SafeArea } from 'capacitor-plugin-safe-area'
import { SplashScreen } from '@capacitor/splash-screen'
import { Capacitor } from '@capacitor/core'

export async function initApp() {
  if (!Capacitor.isNativePlatform()) return

  try {
    await StatusBar.setStyle({ style: Style.Dark })
    await StatusBar.setBackgroundColor({ color: '#08080f' })
  } catch (e) {
    console.warn('StatusBar init failed:', e)
  }

  try {
    const { insets } = await SafeArea.getSafeAreaInsets()
    const root = document.documentElement
    root.style.setProperty('--safe-top', insets.top + 'px')
    root.style.setProperty('--safe-bottom', insets.bottom + 'px')
    root.style.setProperty('--safe-left', insets.left + 'px')
    root.style.setProperty('--safe-right', insets.right + 'px')
  } catch (e) {
    console.warn('SafeArea init failed:', e)
  }

  // Hide splash AFTER app is painted — seamless handoff
  try {
    await SplashScreen.hide({ fadeOutDuration: 300 })
  } catch (e) {
    console.warn('SplashScreen hide failed:', e)
  }
}
