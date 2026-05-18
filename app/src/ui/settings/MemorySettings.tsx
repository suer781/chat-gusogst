import { useSettingsStore } from '../stores'
import { Brain, Trash2, HardDrive } from 'lucide-react'

export function MemorySettings({ onBack }: { onBack: () => void }) {
  const memoryEnabled = useSettingsStore((s) => s.memoryEnabled)
  const setMemoryEnabled = useSettingsStore((s) => s.setMemoryEnabled)

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
        <span style={{ fontSize: "var(--text-xl)", fontWeight: 600, color: 'var(--text-primary)' }}>记忆</span>
      </div>

      {/* Toggle */}
      <div style={{
        margin: '16px 16px 0', background: 'rgba(255,255,255,0.03)', borderRadius: "var(--radius-lg)",
        padding: '18px 16px', border: '1px solid rgba(255,255,255,0.05)',
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 14, color: 'var(--text-primary)', fontSize: "var(--text-base)", fontWeight: 600 }}>
          <Brain size={18} />
          <span>记忆系统</span>
        </div>
        <div onClick={() => setMemoryEnabled(!memoryEnabled)} style={{
          display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '10px 0', cursor: 'pointer',
        }}>
          <div>
            <div style={{ color: 'var(--gray-50)', fontSize: "var(--text-base)" }}>启用记忆</div>
            <div style={{ color: 'var(--gray-400)', fontSize: "var(--text-sm)", marginTop: 2 }}>AI 会记住对话中的重要信息</div>
          </div>
          <div style={{
            width: 46, height: 26, borderRadius: "var(--radius-md)", flexShrink: 0, marginLeft: 12,
            background: memoryEnabled ? 'var(--yellow)' : 'rgba(255,255,255,0.1)',
            position: 'relative', transition: 'background 0.25s',
          }}>
            <div style={{
              width: 22, height: 22, borderRadius: 11, background: 'var(--text-primary)',
              position: 'absolute', top: 2, left: memoryEnabled ? 22 : 2,
              transition: 'left 0.25s cubic-bezier(0.4,0,0.2,1)',
              boxShadow: '0 1px 3px rgba(0, 0, 0, 0.3)',
            }} />
          </div>
        </div>
      </div>

      {/* Storage Info */}
      <div style={{
        margin: '12px 16px 0', background: 'rgba(255,255,255,0.03)', borderRadius: "var(--radius-lg)",
        padding: '18px 16px', border: '1px solid rgba(255,255,255,0.05)',
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 14, color: 'var(--text-primary)', fontSize: "var(--text-base)", fontWeight: 600 }}>
          <HardDrive size={18} />
          <span>存储</span>
        </div>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 }}>
          <span style={{ color: 'var(--gray-300)', fontSize: "var(--text-base)" }}>记忆条目</span>
          <span style={{ color: 'var(--yellow)', fontSize: "var(--text-base)", fontWeight: 600 }}>--</span>
        </div>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 }}>
          <span style={{ color: 'var(--gray-300)', fontSize: "var(--text-base)" }}>存储空间</span>
          <span style={{ color: 'var(--yellow)', fontSize: "var(--text-base)", fontWeight: 600 }}>--</span>
        </div>
        <button style={{
          marginTop: 8, padding: '10px 0', width: '100%', borderRadius: "var(--radius-md)",
          background: 'rgba(233,69,96,0.08)', border: '1px solid rgba(233,69,96,0.2)',
          color: 'var(--accent)', fontSize: "var(--text-base)", cursor: 'pointer',
          display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 6,
        }}>
          <Trash2 size={14} />
          清除所有记忆
        </button>
      </div>
    </div>
  )
}
