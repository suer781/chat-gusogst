import { identifyPlatform } from './platform_keywords'
import type { PlatformInfo } from './types'
import { PLATFORM_PRESETS } from './types'
import { getConnectablePlatforms } from './connector'
import { getChaquopyClient } from './chaquopy'

export function createPlatformConnectTool() {
  return {
    name: 'platform_connect',
    description: '连接社交平台，当用户想在某个平台联系你时调用',
    parameters: {
      type: 'object' as const,
      properties: {
        platform: { type: 'string', description: '平台标识' },
        action: { type: 'string', enum: ['connect', 'disconnect', 'status'], default: 'connect' },
      },
    },
    execute: async (args: Record<string, unknown>) => {
      const chaquopy = getChaquopyClient()
      const action = String(args.action || 'connect')
      const platform = args.platform as string | undefined
      if (!platform) {
        const platforms = getConnectablePlatforms()
        return JSON.stringify({
          status: 'need_platform',
          message: '请告诉我要连接哪个平台',
          available: platforms.map(p => ({ id: p.platform, name: p.displayName, icon: p.icon })),
        })
      }
      const preset = PLATFORM_PRESETS[platform]
      if (!preset) return JSON.stringify({ status: 'error', message: '不支持平台：' + platform })
      try {
        switch (action) {
          case 'connect': {
            const result = await chaquopy.connectPlatform(platform)
            return JSON.stringify({ status: 'connected', platform: preset.displayName, icon: preset.icon, detail: result })
          }
          case 'disconnect': {
            await chaquopy.disconnectPlatform(platform)
            return JSON.stringify({ status: 'disconnected', platform: preset.displayName })
          }
          case 'status': {
            const allStatus = await chaquopy.getPlatformStatus()
            return JSON.stringify({ status: allStatus[platform] ? 'connected' : 'disconnected', platform: preset.displayName })
          }
          default:
            return JSON.stringify({ status: 'error', message: '未知操作：' + action })
        }
      } catch (e: any) {
        return JSON.stringify({ status: 'error', message: e.message })
      }
    }
  }
}

export function registerPlatformConnectTools(registry: { register: Function }) {
  const tool = createPlatformConnectTool()
  registry.register(tool.name, tool.description, tool.parameters, tool.execute)
}

export { getConnectablePlatforms, matchPlatform } from './connector'
export { PLATFORM_PRESETS } from './types'
export type { PlatformInfo } from './types'
