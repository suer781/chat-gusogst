import type { Channel, ChannelStyle } from '../../shared/types'

/**
 * 渠道风格配置
 * 同一个 Agent，不同渠道用不同输出风格
 */
const CHANNEL_STYLES: Record<Channel, ChannelStyle> = {
  app: {
    id: 'app',
    name: '应用内',
    instruction: [
      '你正在应用内与用户进行日常见面式对话。',
      '可以用括号写动作和心理描写，如（微微一笑）（想了想）*轻叹*。',
      '语言自然、有温度，像面对面聊天一样。',
    ].join(''),
  },
  wechat: {
    id: 'wechat',
    name: '微信',
    instruction: [
      '你正在微信上与用户聊天。',
      '纯文字聊天，不用括号描写动作，不用星号标注语气。',
      '口语化、简洁，像普通微信好友聊天一样，偶尔可以用 emoji。',
    ].join(''),
  },
  qq: {
    id: 'qq',
    name: 'QQ',
    instruction: [
      '你正在QQ上与用户聊天。',
      '纯文字聊天，不用括号描写动作。',
      '风格活泼一些，像QQ好友聊天，可以用 emoji 和颜文字。',
    ].join(''),
  },
  feishu: {
    id: 'feishu',
    name: '飞书',
    instruction: [
      '你正在飞书上与用户沟通。',
      '简洁专业，少废话，重点突出。',
      '不需要情感表达和闲聊铺垫。',
    ].join(''),
  },
  custom: {
    id: 'custom',
    name: '自定义',
    instruction: '',
  },
}

/** 获取渠道风格配置 */
export function getChannelStyle(channel: Channel): ChannelStyle {
  return CHANNEL_STYLES[channel] ?? CHANNEL_STYLES.app
}

/** 获取所有渠道列表（用于 UI 选择） */
export function listChannels(): ChannelStyle[] {
  return Object.values(CHANNEL_STYLES)
}

/**
 * 构建渠道风格指令
 * 用于拼入 system prompt
 */
export function buildChannelInstruction(channel?: Channel): string {
  if (!channel || channel === 'custom') return ''
  const style = getChannelStyle(channel)
  return `

[对话渠道: ${style.name}]
${style.instruction}`
}
