// 平台连接引导 - 数据来源: hermes-backend/gateway/platforms/
// 参考: hermes-backend/hermes_cli/platforms.py (PLATFORMS 注册表)

export interface ConnectStep {
  step: number
  action: 'open_url' | 'wait_input' | 'wait_confirm' | 'wait_test'
  url?: string
  hint?: string
  timeoutSec?: number
}

export interface ConnectFlow {
  platform: string
  displayName: string
  icon: string
  steps: ConnectStep[]
}

// 基于 hermes-backend/gateway/platforms/ 的真实平台配置
const PLATFORM_PRESETS: ConnectFlow[] = [
  {
    platform: 'telegram',
    displayName: 'Telegram',
    icon: '📱',
    steps: [
      { step: 1, action: 'open_url', url: 'https://t.me/BotFather' },
      { step: 2, action: 'wait_input', hint: '向 BotFather 发送 /newbot，将获得的 Bot Token 发过来' },
      { step: 3, action: 'wait_confirm', hint: '配置 Telegram 连接中' },
      { step: 4, action: 'wait_test', hint: '在 Telegram 给机器人发条消息测试', timeoutSec: 60 },
    ],
  },
  {
    platform: 'discord',
    displayName: 'Discord',
    icon: '💬',
    steps: [
      { step: 1, action: 'open_url', url: 'https://discord.com/developers/applications' },
      { step: 2, action: 'wait_input', hint: '创建应用 → Bot → Reset Token，将 Bot Token 发过来' },
      { step: 3, action: 'wait_confirm', hint: '配置 Discord 连接中' },
      { step: 4, action: 'wait_test', hint: '在 Discord 服务器发条消息测试', timeoutSec: 60 },
    ],
  },
  {
    platform: 'qqbot',
    displayName: 'QQ Bot',
    icon: '🐧',
    steps: [
      { step: 1, action: 'open_url', url: 'https://q.qq.com' },
      { step: 2, action: 'wait_input', hint: '创建机器人后，将 App ID 和 Client Secret 发过来' },
      { step: 3, action: 'wait_confirm', hint: '配置 QQ Bot 连接中' },
      { step: 4, action: 'wait_test', hint: '在 QQ 给机器人发条消息测试', timeoutSec: 60 },
    ],
  },
  {
    platform: 'weixin',
    displayName: '微信',
    icon: '💬',
    steps: [
      { step: 1, action: 'wait_confirm', hint: '生成微信绑定二维码' },
      { step: 2, action: 'wait_confirm', hint: '用微信扫码（60s 有效，超时重新生成）' },
      { step: 3, action: 'wait_confirm', hint: '扫码成功，确认连接' },
      { step: 4, action: 'wait_test', hint: '在微信发条消息测试', timeoutSec: 60 },
    ],
  },
  {
    platform: 'slack',
    displayName: 'Slack',
    icon: '💼',
    steps: [
      { step: 1, action: 'open_url', url: 'https://api.slack.com/apps' },
      { step: 2, action: 'wait_input', hint: '创建应用 → OAuth & Permissions → 将 Bot Token (xoxb-...) 发过来' },
      { step: 3, action: 'wait_input', hint: 'Basic Information → App-Level Tokens → 创建 token，将 App Token (xapp-...) 发过来' },
      { step: 4, action: 'wait_confirm', hint: '配置 Slack 连接中' },
      { step: 5, action: 'wait_test', hint: '在 Slack 发条消息测试', timeoutSec: 60 },
    ],
  },
  {
    platform: 'feishu',
    displayName: '飞书',
    icon: '🪽',
    steps: [
      { step: 1, action: 'open_url', url: 'https://open.feishu.cn' },
      { step: 2, action: 'wait_input', hint: '创建应用后，将 App ID 和 App Secret 发过来' },
      { step: 3, action: 'wait_confirm', hint: '配置飞书连接中' },
      { step: 4, action: 'wait_test', hint: '在飞书发条消息测试', timeoutSec: 60 },
    ],
  },
  {
    platform: 'dingtalk',
    displayName: '钉钉',
    icon: '📌',
    steps: [
      { step: 1, action: 'open_url', url: 'https://open-dev.dingtalk.com' },
      { step: 2, action: 'wait_input', hint: '创建企业内部应用后，将 AppKey 和 AppSecret 发过来' },
      { step: 3, action: 'wait_confirm', hint: '配置钉钉连接中' },
      { step: 4, action: 'wait_test', hint: '在钉钉发条消息测试', timeoutSec: 60 },
    ],
  },
  {
    platform: 'whatsapp',
    displayName: 'WhatsApp',
    icon: '📱',
    steps: [
      { step: 1, action: 'wait_confirm', hint: '启动 WhatsApp 桥接服务' },
      { step: 2, action: 'wait_confirm', hint: '扫描 WhatsApp Web 二维码' },
      { step: 3, action: 'wait_confirm', hint: '连接成功' },
      { step: 4, action: 'wait_test', hint: '在 WhatsApp 发条消息测试', timeoutSec: 60 },
    ],
  },
  {
    platform: 'signal',
    displayName: 'Signal',
    icon: '📡',
    steps: [
      { step: 1, action: 'wait_input', hint: '提供 Signal HTTP 服务地址 (SIGNAL_HTTP_URL)' },
      { step: 2, action: 'wait_input', hint: '提供 Signal 账号号码 (SIGNAL_ACCOUNT)' },
      { step: 3, action: 'wait_confirm', hint: '配置 Signal 连接中' },
      { step: 4, action: 'wait_test', hint: '在 Signal 发条消息测试', timeoutSec: 60 },
    ],
  },
  {
    platform: 'matrix',
    displayName: 'Matrix',
    icon: '💬',
    steps: [
      { step: 1, action: 'wait_input', hint: '提供 Matrix 家服务器地址 (homeserver URL)' },
      { step: 2, action: 'wait_input', hint: '提供访问令牌 (Access Token) 或用户名+密码' },
      { step: 3, action: 'wait_confirm', hint: '配置 Matrix 连接中' },
      { step: 4, action: 'wait_test', hint: '在 Matrix 房间发条消息测试', timeoutSec: 60 },
    ],
  },
  {
    platform: 'wecom',
    displayName: '企业微信',
    icon: '💬',
    steps: [
      { step: 1, action: 'wait_input', hint: '提供企业微信 Bot ID (WECOM_BOT_ID)' },
      { step: 2, action: 'wait_input', hint: '提供企业微信密钥 (WECOM_SECRET)' },
      { step: 3, action: 'wait_confirm', hint: '配置企业微信连接中' },
      { step: 4, action: 'wait_test', hint: '在企业微信发条消息测试', timeoutSec: 60 },
    ],
  },
  {
    platform: 'email',
    displayName: 'Email',
    icon: '📧',
    steps: [
      { step: 1, action: 'wait_input', hint: '提供 IMAP 服务器地址和端口' },
      { step: 2, action: 'wait_input', hint: '提供 SMTP 服务器地址和端口' },
      { step: 3, action: 'wait_input', hint: '提供邮箱地址和密码' },
      { step: 4, action: 'wait_confirm', hint: '配置 Email 连接中' },
      { step: 5, action: 'wait_test', hint: '发封测试邮件', timeoutSec: 60 },
    ],
  },
  {
    platform: 'homeassistant',
    displayName: 'Home Assistant',
    icon: '🏠',
    steps: [
      { step: 1, action: 'wait_input', hint: '提供 Home Assistant 地址 (如 http://homeassistant.local:8123)' },
      { step: 2, action: 'wait_input', hint: '提供长期访问令牌 (Long-Lived Access Token)' },
      { step: 3, action: 'wait_confirm', hint: '配置 Home Assistant 连接中' },
      { step: 4, action: 'wait_test', hint: '尝试控制一个设备测试', timeoutSec: 60 },
    ],
  },
  {
    platform: 'mattermost',
    displayName: 'Mattermost',
    icon: '💬',
    steps: [
      { step: 1, action: 'wait_input', hint: '提供 Mattermost 服务器地址' },
      { step: 2, action: 'wait_input', hint: '提供 Bot Access Token' },
      { step: 3, action: 'wait_confirm', hint: '配置 Mattermost 连接中' },
      { step: 4, action: 'wait_test', hint: '在 Mattermost 发条消息测试', timeoutSec: 60 },
    ],
  },
  {
    platform: 'bluebubbles',
    displayName: 'BlueBubbles',
    icon: '💙',
    steps: [
      { step: 1, action: 'wait_input', hint: '提供 BlueBubbles 服务器地址 (BLUEBUBBLES_SERVER_URL)' },
      { step: 2, action: 'wait_input', hint: '提供密码 (BLUEBUBBLES_PASSWORD)' },
      { step: 3, action: 'wait_confirm', hint: '配置 BlueBubbles 连接中' },
      { step: 4, action: 'wait_test', hint: '发条 iMessage 测试', timeoutSec: 60 },
    ],
  },
  {
    platform: 'yuanbao',
    displayName: '腾讯元宝',
    icon: '🤖',
    steps: [
      { step: 1, action: 'wait_input', hint: '提供元宝平台凭证' },
      { step: 2, action: 'wait_confirm', hint: '配置元宝连接中' },
      { step: 3, action: 'wait_test', hint: '发条消息测试', timeoutSec: 60 },
    ],
  },
]

export function getConnectablePlatforms(bridge?: { getHermesClient: () => any }): ConnectFlow[] {
  return PLATFORM_PRESETS
}

export function matchPlatform(input: string): ConnectFlow | null {
  const lower = input.toLowerCase()
  return PLATFORM_PRESETS.find(p =>
    p.platform.includes(lower) ||
    p.displayName.toLowerCase().includes(lower) ||
    (lower.includes('qq') && p.platform === 'qqbot') ||
    (lower.includes('微信') && !lower.includes('企业') && p.platform === 'weixin') ||
    (lower.includes('企业微信') && p.platform === 'wecom') ||
    (lower.includes('钉钉') && p.platform === 'dingtalk') ||
    (lower.includes('飞书') && p.platform === 'feishu') ||
    (lower.includes('home') && p.platform === 'homeassistant') ||
    (lower.includes('元宝') && p.platform === 'yuanbao')
  ) || null
}
