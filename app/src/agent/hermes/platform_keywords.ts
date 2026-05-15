/**
 * 平台关键词统一表
 * identifyPlatform 和 matchPlatform 共用，避免不同步
 */
export const PLATFORM_KEYWORDS: Record<string, string[]> = {
  qq:        ['QQ', 'qq', 'qqbot', 'QQBot'],
  wechat:    ['微信', 'wechat', 'WeChat'],
  wecom:     ['企业微信', 'wecom', 'WeCom'],
  telegram:  ['Telegram', 'telegram', '电报', '纸飞机', 'tg', 'TG'],
  discord:   ['Discord', 'discord', 'DC', 'dc'],
  whatsapp:  ['WhatsApp', 'whatsapp', 'WA'],
  slack:     ['Slack', 'slack'],
  feishu:    ['飞书', 'feishu', 'Feishu', 'Lark', 'lark'],
  dingtalk:  ['钉钉', 'dingtalk', 'DingTalk'],
  signal:    ['Signal', 'signal'],
  matrix:    ['Matrix', 'matrix'],
  imessage:  ['iMessage', 'imessage'],
}

/**
 * 从用户消息识别平台（不区分大小写）
 */
export function identifyPlatform(message: string): string | null {
  const lower = message.toLowerCase()
  for (const [platform, keywords] of Object.entries(PLATFORM_KEYWORDS)) {
    if (keywords.some(kw => lower.includes(kw.toLowerCase()))) {
      return platform
    }
  }
  return null
}
