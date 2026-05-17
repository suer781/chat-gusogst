import { useEffect, useState } from 'react'

const STORAGE_KEY = 'disclaimerConfirmCount'
const MAX_CONFIRM = 3

export default function TestDisclaimer() {
  const [show, setShow] = useState(false)

  useEffect(() => {
    const count = parseInt(localStorage.getItem(STORAGE_KEY) || '0', 10)
    if (count < MAX_CONFIRM) setShow(true)
  }, [])

  if (!show) return null

  const handleConfirm = () => {
    const count = parseInt(localStorage.getItem(STORAGE_KEY) || '0', 10) + 1
    localStorage.setItem(STORAGE_KEY, count.toString())
    setShow(false)
  }

  const remaining = MAX_CONFIRM - parseInt(localStorage.getItem(STORAGE_KEY) || '0', 10)

  return (
    <div style={{
      position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.75)',
      display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 9999
    }}>
      <div style={{
        background: '#1a1a3a', borderRadius: 12, padding: '32px 28px',
        maxWidth: 360, width: '85%', textAlign: 'center',
        border: '1px solid #2a2a4a', boxShadow: '0 8px 32px rgba(0,0,0,0.5)'
      }}>
        <div style={{ fontSize: 32, marginBottom: 12 }}>⚠️</div>
        <div style={{ fontSize: 18, fontWeight: 700, color: '#e94560', marginBottom: 8 }}>测试版声明</div>
        <div style={{ fontSize: 13, color: '#8888aa', lineHeight: 1.6, marginBottom: 24 }}>
          本应用为非最终发布版本，仅供测试评估使用。功能和内容可能随时变更，不保证稳定性。
        </div>
        <button onClick={handleConfirm} style={{
          width: '100%', padding: '10px 0', background: '#e94560', color: '#fff',
          border: 'none', borderRadius: 8, fontSize: 15, fontWeight: 600, cursor: 'pointer'
        }}>
          确定
        </button>
        <div style={{ fontSize: 11, color: '#555577', marginTop: 10 }}>
          还需确认 {remaining} 次
        </div>
      </div>
    </div>
  )
}
