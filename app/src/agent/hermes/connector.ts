import { identifyPlatform } from "./platform_keywords"
/**
 * HermesConnector — platform binding data provider
 *
 * Only returns raw step data. NO fixed dialogue.
 * The LLM generates all user-facing text in persona's voice.
 *
 * Tool output example:
 * {
 *   platform: 'qq',
 *   displayName: 'QQ',
 *   icon: '🐧',
 *   steps: [
 *     { step: 1, action: 'open_url', url: 'https://q.qq.com', hint: '...' },
 *     { step: 2, action: 'wait_input', hint: '...' },
 *   ]
 * }
 */

export interface ConnectStep {
  step: number
  /** what the user needs to do */
  action: 'open_url' | 'wait_input' | 'wait_confirm' | 'wait_test'
  /** platform URL if applicable */
  url?: string
  /** factual hint — what to look for, not how to say it */
  hint: string
  timeoutSec?: number
}

export interface ConnectFlow {
  platform: string
  displayName: string
  icon: string
  steps: ConnectStep[]
}

export const CONNECT_FLOWS: Record<string, ConnectFlow> = {
  qq: {
    platform: 'qq',
    displayName: 'QQ',
    icon: '\ud83d\udc27',
    steps: [
      { step: 1, action: 'open_url', url: 'https://q.qq.com', hint: 'QQ 开放平台，申请机器人' },
      { step: 2, action: 'wait_input', hint: '登录后点「创建机器人」，把页面上的指令复制发过来' },
      { step: 3, action: 'wait_confirm', hint: '收到指令后自动配置中' },
      { step: 4, action: 'wait_test', hint: '在 QQ 里发一句话确认连接', timeoutSec: 60 },
    ],
  },

  weixin: {
    platform: 'weixin',
    displayName: '微信',
    icon: '\ud83d\udcac',
    steps: [
      { step: 1, action: 'wait_confirm', hint: '生成微信绑定二维码中' },
      { step: 2, action: 'wait_confirm', hint: '用微信扫码（一分钟有效，超时重新生成）', timeoutSec: 60 },
      { step: 3, action: 'wait_confirm', hint: '扫码成功，确认连接' },
      { step: 4, action: 'wait_test', hint: '在微信里发一句话确认连接', timeoutSec: 60 },
    ],
  },

  telegram: {
    platform: 'telegram',
    displayName: 'Telegram',
    icon: '\u2708\ufe0f',
    steps: [
      { step: 1, action: 'open_url', hint: 'Telegram 搜索 @BotFather，发 /newbot' },
      { step: 2, action: 'wait_input', hint: '把 BotFather 给的 API Token 发过来' },
      { step: 3, action: 'wait_confirm', hint: '配置中' },
      { step: 4, action: 'wait_test', hint: '在 Telegram 里给机器人发句话', timeoutSec: 60 },
    ],
  },

  feishu: {
    platform: 'feishu',
    displayName: '飞书',
    icon: '\ud83d\udc26',
    steps: [
      { step: 1, action: 'open_url', url: 'https://open.feishu.cn', hint: '飞书开放平台，创建应用' },
      { step: 2, action: 'wait_input', hint: '把 App ID 和 App Secret 发过来' },
      { step: 3, action: 'wait_confirm', hint: '配置飞书连接中' },
      { step: 4, action: 'wait_test', hint: '在飞书里发句话确认', timeoutSec: 60 },
    ],
  },

  discord: {
    platform: 'discord',
    displayName: 'Discord',
    icon: '\ud83c\udfae',
    steps: [
      { step: 1, action: 'open_url', url: 'https://discord.com/developers/applications', hint: 'Discord Developer Portal，创建应用' },
      { step: 2, action: 'wait_input', hint: 'Bot 页面点 Reset Token，把 Token 发过来' },
      { step: 3, action: 'wait_confirm', hint: '配置中' },
      { step: 4, action: 'wait_test', hint: '在 Discord 服务器里发句话', timeoutSec: 60 },
    ],
  },

  dingtalk: {
    platform: 'dingtalk',
    displayName: '钉钉',
    icon: '\ud83d\udccc',
    steps: [
      { step: 1, action: 'open_url', url: 'https://open-dev.dingtalk.com', hint: '钉钉开放平台，创建企业内部应用' },
      { step: 2, action: 'wait_input', hint: '把 AppKey 和 AppSecret 发过来' },
      { step: 3, action: 'wait_confirm', hint: '配置中' },
      { step: 4, action: 'wait_test', hint: '在钉钉里发句话确认', timeoutSec: 60 },
    ],
  },
}

export function getConnectablePlatforms(): ConnectFlow[] {
  return Object.values(CONNECT_FLOWS)
}

export function matchPlatform(userInput: string): ConnectFlow | null {
  const platform = identifyPlatform(userInput)
  if (!platform) return null
  // wechat/wecom -> weixin (CONNECT_FLOWS key is 'weixin')
  const aliasMap: Record<string, string> = { wechat: 'weixin', wecom: 'weixin' }
  const flowKey = aliasMap[platform] ?? platform
  return CONNECT_FLOWS[flowKey] ?? null
}

