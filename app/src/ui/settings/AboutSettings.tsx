import { Info, Github, MessageSquare, Heart, ExternalLink, Shield, Code } from 'lucide-react'
import { light as hapticLight, glassTap } from '../haptics'

const VERSION = '0.1.0-dev'
const BUILD = '2026.05.17'

export function AboutSettings({ onBack }: { onBack: () => void }) {
  return (
    <div className="flex-1 flex flex-col overflow-y-auto" style={{ minHeight: 0, background: 'var(--bg-primary)' }}>
      <div style={{
        display: 'flex', alignItems: 'center', gap: 12, padding: '16px 20px', position: 'sticky', top: 0,
        background: 'var(--bg-overlay)', backdropFilter: 'blur(20px)', zIndex: 10,
        borderBottom: '1px solid var(--divider)',
      }}>
        <button onClick={() => { glassTap(); onBack() }} style={{
          background: 'none', border: 'none', color: 'var(--accent)', fontSize: "var(--text-2xl)", cursor: 'pointer', padding: 4,
          display: 'flex', alignItems: 'center',
        }}>{'<-'}</button>
        <span style={{ fontSize: "var(--text-xl)", fontWeight: 600, color: 'var(--text-primary)' }}>关于</span>
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
          background: 'linear-gradient(135deg, var(--accent), var(--purple))',
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          fontSize: 32, boxShadow: '0 8px 24px var(--accent-glow)',
        }}>
          💬
        </div>
        <div style={{ color: 'var(--text-primary)', fontSize: 22, fontWeight: 700, marginBottom: 4 }}>
          Chat Gusogst
        </div>
        <div style={{ color: 'var(--gray-400)', fontSize: "var(--text-base)" }}>
          v{VERSION} · {BUILD}
        </div>
        <div style={{ color: 'var(--gray-400)', fontSize: "var(--text-sm)", marginTop: 8 }}>
          AI 虚拟恋人 · 融合 Agent 能力
        </div>
      </div>

      {/* Links */}
      <div style={{
        margin: '16px 16px 0', background: 'rgba(255,255,255,0.03)', borderRadius: "var(--radius-lg)",
        padding: '8px 0', border: '1px solid rgba(255,255,255,0.05)',
      }}>
        <LinkItem icon={<Github size={18} />} label="GitHub 仓库"
          desc="查看源码、提交 Issue" color="var(--gray-400)" />
        <LinkItem icon={<MessageSquare size={18} />} label="反馈与建议"
          desc="帮助我们做得更好" color="var(--warning)" />
        <LinkItem icon={<Code size={18} />} label="开源许可"
          desc="MIT License" color="var(--purple)" />
        <LinkItem icon={<Shield size={18} />} label="隐私政策"
          desc="我们如何保护你的数据" color="var(--teal)" />
      </div>

      {/* Tech Stack */}
      <div style={{
        margin: '16px 16px 0', background: 'rgba(255,255,255,0.03)', borderRadius: "var(--radius-lg)",
        padding: '18px 16px', border: '1px solid rgba(255,255,255,0.05)',
      }}>
        <div style={{ color: 'var(--text-primary)', fontSize: "var(--text-base)", fontWeight: 600, marginBottom: 12 }}>
          技术栈
        </div>
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
          {['React', 'TypeScript', 'Capacitor', 'Python', 'Hermes Agent', 'Zustand'].map((t) => (
            <span key={t} style={{
              padding: '4px 10px', borderRadius: 6, fontSize: "var(--text-xs)",
              background: 'rgba(255,255,255,0.05)', color: 'var(--gray-400)',
            }}>{t}</span>
          ))}
        </div>
      </div>

      {/* Footer */}
      <div style={{
        margin: '24px 16px 0', padding: '16px', textAlign: 'center',
      }}>
        <div style={{ color: 'var(--gray-500)', fontSize: "var(--text-sm)", display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 4 }}>
          Made with <Heart size={12} color="var(--accent)" fill="var(--accent)" /> by suer781
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
        width: 36, height: 36, borderRadius: "var(--radius-md)",
        background: `${color}15`, display: 'flex', alignItems: 'center', justifyContent: 'center',
        flexShrink: 0, color: color,
      }}>{icon}</div>
      <div style={{ flex: 1 }}>
        <div style={{ color: 'var(--gray-50)', fontSize: "var(--text-base)" }}>{label}</div>
        <div style={{ color: 'var(--gray-400)', fontSize: "var(--text-xs)", marginTop: 1 }}>{desc}</div>
      </div>
      <ExternalLink size={14} color="var(--gray-500)" />
    </div>
  )
}
