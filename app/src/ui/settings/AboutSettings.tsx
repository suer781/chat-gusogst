import { Info, Github, MessageSquare, Heart, ExternalLink, Shield, Code } from 'lucide-react'

const VERSION = '0.1.0-dev'
const BUILD = '2026.05.17'

export function AboutSettings({ onBack }: { onBack: () => void }) {
  return (
    <div style={{ minHeight: '100%', background: '#0f0f23', padding: '0 0 100px' }}>
      <div style={{
        display: 'flex', alignItems: 'center', gap: 12, padding: '16px 20px', position: 'sticky', top: 0,
        background: 'rgba(15,15,35,0.9)', backdropFilter: 'blur(20px)', zIndex: 10,
        borderBottom: '1px solid rgba(255,255,255,0.06)',
      }}>
        <button onClick={onBack} style={{
          background: 'none', border: 'none', color: '#e94560', fontSize: 20, cursor: 'pointer', padding: 4,
          display: 'flex', alignItems: 'center',
        }}>{'<-'}</button>
        <span style={{ fontSize: 18, fontWeight: 600, color: '#fff' }}>关于</span>
      </div>

      {/* Logo & Version */}
      <div style={{
        margin: '24px 16px 0', padding: '32px 20px',
        background: 'linear-gradient(135deg, rgba(233,69,96,0.08), rgba(108,92,231,0.08))',
        borderRadius: 20, border: '1px solid rgba(255,255,255,0.05)',
        textAlign: 'center',
      }}>
        <div style={{
          width: 72, height: 72, borderRadius: 20, margin: '0 auto 16px',
          background: 'linear-gradient(135deg, #e94560, #6C5CE7)',
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          fontSize: 32, boxShadow: '0 8px 24px rgba(233,69,96,0.2)',
        }}>
          💬
        </div>
        <div style={{ color: '#fff', fontSize: 22, fontWeight: 700, marginBottom: 4 }}>
          Chat Gusogst
        </div>
        <div style={{ color: '#666', fontSize: 13 }}>
          v{VERSION} · {BUILD}
        </div>
        <div style={{ color: '#555', fontSize: 12, marginTop: 8 }}>
          AI 虚拟恋人 · 融合 Agent 能力
        </div>
      </div>

      {/* Links */}
      <div style={{
        margin: '16px 16px 0', background: 'rgba(255,255,255,0.03)', borderRadius: 16,
        padding: '8px 0', border: '1px solid rgba(255,255,255,0.05)',
      }}>
        <LinkItem icon={<Github size={18} />} label="GitHub 仓库"
          desc="查看源码、提交 Issue" color="#636E72" />
        <LinkItem icon={<MessageSquare size={18} />} label="反馈与建议"
          desc="帮助我们做得更好" color="#E17055" />
        <LinkItem icon={<Code size={18} />} label="开源许可"
          desc="MIT License" color="#6C5CE7" />
        <LinkItem icon={<Shield size={18} />} label="隐私政策"
          desc="我们如何保护你的数据" color="#00B894" />
      </div>

      {/* Tech Stack */}
      <div style={{
        margin: '16px 16px 0', background: 'rgba(255,255,255,0.03)', borderRadius: 16,
        padding: '18px 16px', border: '1px solid rgba(255,255,255,0.05)',
      }}>
        <div style={{ color: '#fff', fontSize: 14, fontWeight: 600, marginBottom: 12 }}>
          技术栈
        </div>
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
          {['React', 'TypeScript', 'Capacitor', 'Python', 'Hermes Agent', 'Zustand'].map((t) => (
            <span key={t} style={{
              padding: '4px 10px', borderRadius: 6, fontSize: 11,
              background: 'rgba(255,255,255,0.05)', color: '#888',
            }}>{t}</span>
          ))}
        </div>
      </div>

      {/* Footer */}
      <div style={{
        margin: '24px 16px 0', padding: '16px', textAlign: 'center',
      }}>
        <div style={{ color: '#444', fontSize: 12, display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 4 }}>
          Made with <Heart size={12} color="#e94560" fill="#e94560" /> by suer781
        </div>
      </div>
    </div>
  )
}

function LinkItem({ icon, label, desc, color }: {
  icon: React.ReactNode; label: string; desc: string; color: string
}) {
  return (
    <div style={{
      display: 'flex', alignItems: 'center', gap: 12, padding: '14px 16px',
      cursor: 'pointer', transition: 'background 0.15s',
    }}>
      <div style={{
        width: 36, height: 36, borderRadius: 10,
        background: `${color}15`, display: 'flex', alignItems: 'center', justifyContent: 'center',
        flexShrink: 0, color: color,
      }}>{icon}</div>
      <div style={{ flex: 1 }}>
        <div style={{ color: '#eee', fontSize: 14 }}>{label}</div>
        <div style={{ color: '#666', fontSize: 11, marginTop: 1 }}>{desc}</div>
      </div>
      <ExternalLink size={14} color="#444" />
    </div>
  )
}
