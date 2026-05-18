import { useState, useRef, useEffect } from 'react'
import { type EyeCareMapping, genMappingId, DEFAULT_EYE_CARE_MAPPINGS } from '../stores'

interface EyeCareColorMapperProps {
  mappings: EyeCareMapping[]
  intensity: number
  onMappingsChange: (m: EyeCareMapping[]) => void
  onIntensityChange: (v: number) => void
}

/* 内置颜色选择器，替代 <input type="color"> 的原生弹窗 */
const PRESET_COLORS = [
  '#FFFFFF', '#F5F0E8', '#E8D5B7', '#D4E157', '#AED581',
  '#81C784', '#4DB6AC', '#4FC3F7', '#64B5F6', '#7986CB',
  '#9575CD', '#BA68C8', '#F06292', '#E57373', '#FF8A65',
  '#FFB74D', '#FFD54F', '#A1887F', '#90A4AE', '#000000',
]

function ColorPicker({ value, onChange, onClose }: { value: string; onChange: (c: string) => void; onClose: () => void }) {
  const [hex, setHex] = useState(value)
  const ref = useRef<HTMLDivElement>(null)

  useEffect(() => {
    const handler = (e: MouseEvent) => {
      if (ref.current && !ref.current.contains(e.target as Node)) onClose()
    }
    document.addEventListener('mousedown', handler)
    return () => document.removeEventListener('mousedown', handler)
  }, [onClose])

  return (
    <div ref={ref} className="color-picker-popup" style={{
      position: 'absolute', top: '100%', left: 0, marginTop: 4, zIndex: 100,
      background: 'var(--bg-elevated, #2a2a2a)', border: '1px solid var(--border-color, #444)',
      borderRadius: 12, padding: 12, boxShadow: '0 8px 24px rgba(0,0,0,0.4)',
      minWidth: 200,
    }}>
      {/* 预设色块 */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(5, 1fr)', gap: 6, marginBottom: 10 }}>
        {PRESET_COLORS.map(c => (
          <button key={c} onClick={() => { onChange(c); setHex(c); onClose() }} style={{
            width: 28, height: 28, borderRadius: 6, background: c, border: c === value ? '2px solid var(--accent, #e94560)' : '1px solid var(--border-color, #555)',
            cursor: 'pointer', padding: 0, transition: 'transform 0.15s',
          }} />
        ))}
      </div>
      {/* Hex 输入 */}
      <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
        <span style={{ fontSize: 12, opacity: 0.6 }}>#</span>
        <input
          type="text" maxLength={6} value={hex.replace('#', '')}
          onChange={e => {
            const v = e.target.value.replace(/[^0-9a-fA-F]/g, '')
            setHex('#' + v)
            if (v.length === 6) onChange('#' + v)
          }}
          style={{
            flex: 1, background: 'var(--bg-surface, #333)', border: '1px solid var(--border-color, #555)',
            borderRadius: 6, padding: '4px 8px', color: 'var(--text-primary)', fontSize: 13, outline: 'none',
          }}
        />
      </div>
    </div>
  )
}

function ColorSwatch({ color, onChange }: { color: string; onChange: (c: string) => void }) {
  const [open, setOpen] = useState(false)
  return (
    <div style={{ position: 'relative', width: 32, height: 32, flexShrink: 0 }}>
      <button onClick={() => setOpen(!open)} style={{
        width: 32, height: 32, borderRadius: 8, background: color,
        border: '1px solid var(--border-color, #555)', cursor: 'pointer', padding: 0,
      }} />
      {open && <ColorPicker value={color} onChange={onChange} onClose={() => setOpen(false)} />}
    </div>
  )
}

export function EyeCareColorMapper({ mappings, intensity, onMappingsChange, onIntensityChange }: EyeCareColorMapperProps) {
  const update = (id: string, patch: Partial<EyeCareMapping>) => {
    onMappingsChange(mappings.map(m => m.id === id ? { ...m, ...patch } : m))
  }
  const remove = (id: string) => {
    onMappingsChange(mappings.filter(m => m.id !== id))
  }
  const add = () => {
    onMappingsChange([
      ...mappings,
      { id: genMappingId(), sourceColor: '#FFFFFF', targetColor: '#F5F0E8', label: '' }
    ])
  }

  return (
    <div style={{ marginTop: 12 }}>
      {/* 强度滑块 */}
      <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 12 }}>
        <span style={{ fontSize: 13, opacity: 0.7, minWidth: 36 }}>强度</span>
        <input
          type="range" min={0} max={100} value={intensity}
          onChange={e => onIntensityChange(Number(e.target.value))}
          style={{ flex: 1 }}
        />
        <span style={{ fontSize: 13, opacity: 0.5, minWidth: 32 }}>{intensity}%</span>
      </div>

      {/* 映射列表 */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
        {mappings.map(m => (
          <div key={m.id} style={{ display: 'flex', alignItems: 'center', gap: 8, padding: '6px 0' }}>
            <ColorSwatch color={m.sourceColor} onChange={c => update(m.id, { sourceColor: c })} />
            <span style={{ fontSize: 16, opacity: 0.5 }}>→</span>
            <ColorSwatch color={m.targetColor} onChange={c => update(m.id, { targetColor: c })} />
            <input
              type="text" placeholder="标签" value={m.label || ''}
              onChange={e => update(m.id, { label: e.target.value })}
              style={{
                flex: 1, background: 'var(--bg-surface, #333)', border: '1px solid var(--border-color, #555)',
                borderRadius: 6, padding: '4px 8px', color: 'var(--text-primary)', fontSize: 13, outline: 'none',
              }}
            />
            <button onClick={() => remove(m.id)} style={{ color: 'var(--error, #ef4444)', fontSize: 16, padding: '4px 8px' }}>×</button>
          </div>
        ))}
      </div>

      {/* 添加按钮 */}
      <button onClick={add} style={{
        marginTop: 8, padding: '6px 12px', borderRadius: 8,
        background: 'var(--bg-surface, #333)', border: '1px dashed var(--border-color, #555)',
        color: 'var(--text-secondary)', fontSize: 13, cursor: 'pointer', width: '100%',
      }}>+ 添加颜色映射</button>
    </div>
  )
}
