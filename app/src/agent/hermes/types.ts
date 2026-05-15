/**
 * Hermes 平台类型定义
 */

export interface PlatformInfo {
  name: string
  displayName: string
  enabled: boolean
  connected: boolean
  status: 'active' | 'inactive' | 'error' | 'disconnected'
  error?: string
  type: 'im' | 'email' | 'voice' | 'smart_home' | 'other'
  icon: string
}

export interface PlatformMessage {
  platform: string
  conversationId: string
  sender: string
  content: string
  type: 'text' | 'image' | 'audio' | 'file'
  timestamp: number
}

export const PLATFORM_PRESETS: Record<string, Omit<PlatformInfo, 'enabled' | 'connected' | 'status'>> = {
  weixin: { name: 'weixin', displayName: '微信', type: 'im', icon: '💬' },
  wecom: { name: 'wecom', displayName: '企业微信', type: 'im', icon: '🏢' },
  qq: { name: 'qq', displayName: 'QQ', type: 'im', icon: '🐧' },
  feishu: { name: 'feishu', displayName: '飞书', type: 'im', icon: '🐦' },
  dingtalk: { name: 'dingtalk', displayName: '钉钉', type: 'im', icon: '📌' },
  telegram: { name: 'telegram', displayName: 'Telegram', type: 'im', icon: '✈️' },
  discord: { name: 'discord', displayName: 'Discord', type: 'im', icon: '🎮' },
  slack: { name: 'slack', displayName: 'Slack', type: 'im', icon: '💼' },
  whatsapp: { name: 'whatsapp', displayName: 'WhatsApp', type: 'im', icon: '📱' },
  signal: { name: 'signal', displayName: 'Signal', type: 'im', icon: '🔒' },
  matrix: { name: 'matrix', displayName: 'Matrix', type: 'im', icon: '🔗' },
  email: { name: 'email', displayName: '邮件', type: 'email', icon: '📧' },
  sms: { name: 'sms', displayName: '短信', type: 'im', icon: '💬' },
  homeassistant: { name: 'homeassistant', displayName: 'Home Assistant', type: 'smart_home', icon: '🏠' },
}
