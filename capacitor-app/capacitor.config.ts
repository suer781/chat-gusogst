import type { CapacitorConfig } from '@capacitor/cli'

const config: CapacitorConfig = {
  appId: 'com.gusogst.chat',
  appName: 'chat-gusogst',
  webDir: 'dist',
  server: {
    androidScheme: 'https'
  }
}

export default config