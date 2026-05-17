import type { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  appId: 'com.gusogst.chat',
  appName: 'chat-gusogst',
  webDir: 'dist',
  plugins: {
    StatusBar: {
      overlaysWebView: true,
      backgroundColor: '#00000000',
      style: 'DARK',
    },
  },
};

export default config;
