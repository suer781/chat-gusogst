import { identifyPlatform } from "./platform_keywords"
import type { ToolDefinition } from '../../shared/types'
import type { PlatformInfo } from './types'
import { PLATFORM_PRESETS } from './types'
import { getConnectablePlatforms } from './connector'
import { getChaquopyClient } from './chaquopy'

export function createPlatformConnectTool(): ToolDefinition {
  return {
    type: 'function',
    function: {
      name: 'platform_connect',
      description: '连接社交平台（QQ/微信/Telegram等），当用户想在某个平台联系你时调用',
      parameters: {
        type: 'object',
        properties: {
          platform: { type: 'string', description: '平台标识' },
          action: { type: 'string', enum: ['connect', 'disconnect', 'status'], default: 'connect' },
        },
      },
    },
  }
}

export async function executePlatformConnect(args: { platform?: string; action?: string }) {
  const chaquopy = getChaquopyClient()
  const action = args.action || 'connect'
  const platform = args.platform

  if (!platform) {
    const platforms = getConnectablePlatforms()
    return {
      status: 'need_platform',
      message: '请告诉我要连接哪个平台～',
      available: platforms.map(p => ({ id: p.platform, name: p.displayName, icon: p.icon })),
    }
  }

  const preset = PLATFORM_PRESETS[platform]
  if (!preset) return { status: 'error', message: '不支持平台：' + platform }

  try {
    switch (action) {
      case 'connect': {
        const result = await chaquopy.connectPlatform(platform)
        return { status: 'connected', platform: preset.displayName, icon: preset.icon, detail: result }
      }
      case 'disconnect': {
        await chaquopy.disconnectPlatform(platform)
        return { status: 'disconnected', platform: preset.displayName }
      }
      case 'status': {
        const allStatus = await chaquopy.getPlatformStatus()
        return { status: allStatus[platform] ? 'connected' : 'disconnected', platform: preset.displayName }
      }
      default:
        return { status: 'error', message: '未知操作：' + action }
    }
  } catch (e: any) {
    return { status: 'error', message: e.message }
  }
}

export { getConnectablePlatforms, matchPlatform } from './connector'
export { PLATFORM_PRESETS } from './types'
export type { PlatformInfo } from './types'
