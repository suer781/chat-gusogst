import type { ToolDefinition } from '../../shared/types'
import { matchPlatform, getConnectablePlatforms } from './connector'
import type { HermesBridge } from './bridge'

export function createPlatformConnectTool(bridge?: HermesBridge) {
  return {
    definition: {
      type: 'function' as const,
      function: {
        name: 'platform_connect',
        description: '引导用户连接社交平台（微信、QQ、Telegram、飞书、Discord、钉钉）。当用户说「加微信」「加QQ」「连接xx」时调用。返回步骤数据，用你的语气引导用户。',
        parameters: {
          type: 'object',
          properties: {
            platform: {
              type: 'string',
              description: '平台名：qq/weixin/telegram/feishu/discord/dingtalk，或用户原话',
            },
            action: {
              type: 'string',
              enum: ['get_steps', 'list_platforms', 'check_status'],
              description: 'get_steps=获取步骤, list_platforms=列出平台, check_status=查状态',
            },
          },
          required: ['action'],
        },
      },
    },

    handler: async (_name: string, args: any) => {
      const { action, platform } = args

      if (action === 'list_platforms') {
        return {
          platforms: getConnectablePlatforms().map(f => ({
            name: f.platform,
            displayName: f.displayName,
            icon: f.icon,
          })),
        }
      }

      if (action === 'check_status') {
        if (!bridge) return { error: 'Hermes not started' }
        try {
          const platforms = await bridge.getPlatforms()
          return {
            connected: platforms.filter(p => p.connected),
            available: platforms.filter(p => !p.connected),
          }
        } catch (e: any) {
          return { error: e.message }
        }
      }

      const flow = matchPlatform(platform ?? '')
      if (!flow) {
        return { supported: ['qq', 'weixin', 'telegram', 'feishu', 'discord', 'dingtalk'] }
      }
      return { platform: flow.displayName, icon: flow.icon, steps: flow.steps }
    },
  }
}
