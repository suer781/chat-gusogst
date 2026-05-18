import { type EyeCareMapping, genMappingId, DEFAULT_EYE_CARE_MAPPINGS } from '../stores'

interface EyeCareColorMapperProps {
  mappings: EyeCareMapping[]
  intensity: number
  onMappingsChange: (m: EyeCareMapping[]) => void
  onIntensityChange: (v: number) => void
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
            {/* 源色 */}
            <div style={{ position: 'relative', width: 32, height: 32, borderRadius: 8, border: '1px solid var(--border-color, #555)', overflow: 'hidden', flexShrink: 0 }}>
              <div style={{ width: '100%', height: '100%', background: m.sourceColor }} />
              <input
                type="color" value={m.sourceColor}
                onChange={e => update(m.id, { sourceColor: e.target.value })}
                style={{ position: 'absolute', inset: 0, opacity: 0, cursor: 'pointer', width: '100%', height: '100%' }}
              />
            </div>
            <span style={{ fontSize: 16, opacity: 0.5 }}>→</span>
            {/* 目标色 */}
            <div style={{ position: 'relative', width: 32, height: 32, borderRadius: 8, border: '1px solid var(--border-color, #555)', overflow: 'hidden', flexShrink: 0 }}>
              <div style={{ width: '100%', height: '100%', background: m.targetColor }} />
              <input
                type="color" value={m.targetColor}
                onChange={e => update(m.id, { targetColor: e.target.value })}
                style={{ position: 'absolute', inset: 0, opacity: 0, cursor: 'pointer', width: '100%', height: '100%' }}
              />
            </div>
            {/* 标签 */}
            <input
              type="text" value={m.label || ''} placeholder="自定义标签"
              onChange={e => update(m.id, { label: e.target.value })}
              style={{ flex: 1, minWidth: 0, fontSize: 13, background: 'transparent', border: '1px solid var(--border-color, #555)', borderRadius: 6, padding: '4px 8px', color: 'inherit' }}
            />
            {/* 删除 */}
            <button
              onClick={() => remove(m.id)}
              style={{ width: 28, height: 28, border: 'none', borderRadius: 6, background: 'rgba(255,60,60,0.15)', color: 'var(--danger)', fontSize: 16, cursor: 'pointer', flexShrink: 0, display: 'flex', alignItems: 'center', justifyContent: 'center' }}
            >×</button>
          </div>
        ))}
      </div>

      {/* 添加按钮 */}
      <button
        onClick={add}
        style={{ width: '100%', marginTop: 8, padding: '10px', border: '1px dashed var(--border-color, #555)', borderRadius: 8, background: 'transparent', color: 'inherit', opacity: 0.7, fontSize: 13, cursor: 'pointer' }}
      >
        + 添加颜色映射
      </button>

      {/* 恢复默认 */}
      <button
        onClick={() => onMappingsChange(DEFAULT_EYE_CARE_MAPPINGS)}
        style={{ width: '100%', marginTop: 6, padding: '8px', border: 'none', borderRadius: 8, background: 'transparent', color: 'inherit', opacity: 0.5, fontSize: 12, cursor: 'pointer' }}
      >
        恢复默认
      </button>
    </div>
  )
}
