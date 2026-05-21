import type { CapacitorConfig } from '@capacitor/cli'

const config: CapacitorConfig = {
  appId: 'com.chatgusogst.app',
  appName: 'chat-gusogst',
  webDir: 'dist',
  server: {
    androidScheme: 'https',
  },
  plugins: {
    SplashScreen: {
      launchShowDuration: 0,
      backgroundColor: '#0f0f23',
      showSpinner: false,
      launchAutoHide: false,
    },
    Keyboard: {
      resize: 'none',
      resizeOnFullScreen: true,
    },
    StatusBar: {
      style: 'DARK',
      backgroundColor: '#0f0f23',
    },
    NavigationBar: {
      color: '#0a0a1a',
    },
  },
}
export default config
