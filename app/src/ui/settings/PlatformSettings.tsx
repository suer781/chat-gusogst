import { useState } from 'react'
import { Link2, Wifi, WifiOff, Loader2 } from 'lucide-react'

type PlatformStatus = 'disconnected' | 'connecting' | 'connected'

const PLATFORMS = [
  { id: 'telegram',  name: 'Telegram',  icon: '📱', color: '#0088cc' },
  { id: 'discord',   name: 'Discord',   icon: '💬', color: '#5865F2' },
  { id: 'qqbot',     name: 'QQ Bot',    icon: '🐧', color: '#12B7F5' },
  { id: 'weixin',    name: '微信',       icon: '💚', color: '#07C160' },
  { id: 'slack',     name: 'Slack',     icon: '💼', color: '#4A154B' },
  { id: 'whatsapp',  name: 'WhatsApp',  icon: '📞', color: '#25D366' },
  { id: 'twitter',   name: 'Twitter/X', icon: '🐦', color: '#1DA1F2' },
  { id: 'email',     name: 'Email',     icon: '📧', color: '#EA4335' },
  { id: 'line',      name: 'LINE',      icon: '🟢', color: '#00B900' },
  { id: 'irc',       name: 'IRC',       icon: '💻', color: '#CCCCCC' },
  { id: 'matrix',    name: 'Matrix',    icon: '🔗', color: '#0DBD8B' },
  { id: 'teams',     name: 'Teams',     icon: '🏢', color: '#6264A7' },
  { id: 'wechat',    name: '企业微信',   icon: '🏢', color: '#1AAD19' },
  { id: 'dingtalk',  name: '钉钉',       icon: '📌', color: '#0082EF' },
  { id: 'feishu',    name: '飞书',       icon: '🪶', color: '#3370FF' },
  { id: 'signal',    name: 'Signal',    icon: '🔒', color: '#3A76F0' },
]

export function PlatformSettings({ onBack }: { onBack: () => void }) {
  const [statuses, setStatuses] = useState<Record<string, PlatformStatus>>({})

  const handleConnect = (id: string) => {
    setStatuses((s) => ({ ...s, [id]: 'connecting' }))
    // TODO: integrate with connector.ts flow
    setTimeout(() => setStatuses((s) => ({ ...s, [id]: 'disconnected' })), 2000)
  }

  return (
    <div style={{ minHeight: '100%', background: 'var(--bg-primary)', padding: '0 0 100px' }}>
      <div style={{
        display: 'flex', alignItems: 'center', gap: 12, padding: '16px 20px', position: 'sticky', top: 0,
        background: 'var(--bg-overlay)', backdropFilter: 'blur(20px)', zIndex: 10,
        borderBottom: '1px solid var(--divider)',
      }}>
        <button onClick={onBack} style={{
          background: 'none', border: 'none', color: 'var(--accent)', fontSize: "var(--text-2xl)", cursor: 'pointer', padding: 4,
          display: 'flex', alignItems: 'center',
        }}>{'<-'}</button>
        <span style={{ fontSize: "var(--text-xl)", fontWeight: 600, color: 'var(--text-primary)' }}>平台连接</span>
      </div>

      <div style={{ padding: '12px 16px', display: 'flex', flexDirection: 'column', gap: 8 }}>
        {PLATFORMS.map((p, i) => {
          const status = statuses[p.id] || 'disconnected'
          return (
            <div key={p.id} style={{
              display: 'flex', alignItems: 'center', gap: 12, padding: '14px 16px',
              background: 'rgba(255,255,255,0.03)', borderRadius: "var(--radius-md)",
              border: '1px solid rgba(255,255,255,0.05)',
              animation: `fadeInUp 0.3s ${i * 0.03}s both`,
            }}>
              <div style={{
                width: 40, height: 40, borderRadius: "var(--radius-md)",
                background: `${p.color}15`, display: 'flex', alignItems: 'center', justifyContent: 'center',
                fontSize: "var(--text-2xl)", flexShrink: 0,
              }}>{p.icon}</div>
              <div style={{ flex: 1 }}>
                <div style={{ color: 'var(--text-primary)', fontSize: "var(--text-base)", fontWeight: 500 }}>{p.name}</div>
                <div style={{
                  color: status === 'connected' ? '#00B894' : status === 'connecting' ? 'var(--yellow)' : 'var(--gray-400)',
                  fontSize: "var(--text-xs)", marginTop: 2,
                }}>
                  {status === 'connected' ? '已连接' : status === 'connecting' ? '连接中...' : '未连接'}
                </div>
              </div>
              <button onClick={() => handleConnect(p.id)} disabled={status === 'connecting'} style={{
                padding: '6px 14px', borderRadius: "var(--radius-sm)", border: 'none', cursor: 'pointer',
                background: status === 'connected' ? 'rgba(0,184,148,0.15)' : 'var(--accent-soft)',
                color: status === 'connected' ? '#00B894' : 'var(--accent)',
                fontSize: "var(--text-sm)", fontWeight: 500,
                display: 'flex', alignItems: 'center', gap: 4,
              }}>
                {status === 'connecting' && <Loader2 size={12} className="animate-spin" />}
                {status === 'connected' ? '断开' : '连接'}
              </button>
            </div>
          )
        })}
      </div>

      <style>{`
        @keyframes fadeInUp {
          from { opacity: 0; transform: translateY(8px); }
          to   { opacity: 1; transform: translateY(0); }
        }
        .animate-spin { animation: spin 1s linear infinite; }
        @keyframes spin { from { transform: rotate(0); } to { transform: rotate(360deg); } }
      `}</style>
    </div>
  )
}
