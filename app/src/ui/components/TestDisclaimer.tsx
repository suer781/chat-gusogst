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
      position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.45)', backdropFilter: 'blur(12px)', WebkitBackdropFilter: 'blur(12px)',
      display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 9999
    }}>
      <div style={{
        background: 'linear-gradient(165deg, rgba(255,255,255,0.1) 0%, rgba(20,20,40,0.65) 100%)', borderRadius: 16, padding: '32px 28px', maxWidth: 360, width: '85%', textAlign: 'center',
        border: '1px solid rgba(255,255,255,0.12)', boxShadow: '0 8px 32px rgba(0,0,0,0.5)'
      }}>
        <div style={{ fontSize: "var(--text-4xl)", marginBottom: 12 }}>⚠️</div>
        <div style={{ fontSize: "var(--text-xl)", fontWeight: 700, color: 'var(--accent)', marginBottom: 8 }}>测试版声明</div>
        <div style={{ fontSize: "var(--text-base)", color: 'var(--gray-300)', lineHeight: 1.6, marginBottom: 24 }}>
          本应用为非最终发布版本，仅供测试评估使用。功能和内容可能随时变更，不保证稳定性。
        </div>
        <button onClick={handleConfirm} style={{
          width: '100%', padding: '10px 0', background: 'var(--accent)', color: 'var(--text-primary)',
          border: 'none', borderRadius: 8, fontSize: "var(--text-md)", fontWeight: 600, cursor: 'pointer'
        }}>
          确定
        </button>
        <div style={{ fontSize: "var(--text-xs)", color: 'var(--gray-500)', marginTop: 10 }}>
          还需确认 {remaining} 次
        </div>
      </div>
    </div>
  )
}
