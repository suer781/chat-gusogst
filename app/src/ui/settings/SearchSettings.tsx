import { useSettingsStore } from '../stores'
import { Search, Key, Globe } from 'lucide-react'

const ENGINES = [
  { id: 'duckduckgo', label: 'DuckDuckGo', desc: '免费，无需 API Key', free: true },
  { id: 'tavily',     label: 'Tavily',     desc: 'AI 优化搜索，需要 API Key', free: false },
  { id: 'serpapi',     label: 'SerpAPI',    desc: 'Google 搜索结果，需要 API Key', free: false },
  { id: 'bing',        label: 'Bing',       desc: '微软搜索，需要 API Key', free: false },
]

export function SearchSettings({ onBack }: { onBack: () => void }) {
  const searchEnabled = useSettingsStore((s) => s.searchEnabled)
  const setSearchEnabled = useSettingsStore((s) => s.setSearchEnabled)

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
        <span style={{ fontSize: 18, fontWeight: 600, color: '#fff' }}>搜索</span>
      </div>

      {/* Toggle */}
      <div style={{
        margin: '16px 16px 0', background: 'rgba(255,255,255,0.03)', borderRadius: 16,
        padding: '18px 16px', border: '1px solid rgba(255,255,255,0.05)',
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 14, color: '#fff', fontSize: 14, fontWeight: 600 }}>
          <Search size={18} />
          <span>联网搜索</span>
        </div>
        <div onClick={() => setSearchEnabled(!searchEnabled)} style={{
          display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '10px 0', cursor: 'pointer',
        }}>
          <div>
            <div style={{ color: '#eee', fontSize: 14 }}>启用搜索</div>
            <div style={{ color: '#666', fontSize: 12, marginTop: 2 }}>AI 可以搜索互联网获取最新信息</div>
          </div>
          <div style={{
            width: 46, height: 26, borderRadius: 13, flexShrink: 0, marginLeft: 12,
            background: searchEnabled ? '#E17055' : 'rgba(255,255,255,0.1)',
            position: 'relative', transition: 'background 0.25s',
          }}>
            <div style={{
              width: 22, height: 22, borderRadius: 11, background: '#fff',
              position: 'absolute', top: 2, left: searchEnabled ? 22 : 2,
              transition: 'left 0.25s cubic-bezier(0.4,0,0.2,1)',
              boxShadow: '0 1px 3px rgba(0,0,0,0.3)',
            }} />
          </div>
        </div>
      </div>

      {/* Engine Selection */}
      {searchEnabled && (
        <div style={{
          margin: '12px 16px 0', background: 'rgba(255,255,255,0.03)', borderRadius: 16,
          padding: '18px 16px', border: '1px solid rgba(255,255,255,0.05)',
        }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 14, color: '#fff', fontSize: 14, fontWeight: 600 }}>
            <Globe size={18} />
            <span>搜索引擎</span>
          </div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
            {ENGINES.map((e) => (
              <div key={e.id} style={{
                padding: '12px 14px', borderRadius: 12,
                background: 'rgba(255,255,255,0.04)',
                border: '1px solid rgba(255,255,255,0.06)',
                display: 'flex', justifyContent: 'space-between', alignItems: 'center',
                cursor: 'pointer',
              }}>
                <div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                    <span style={{ color: '#fff', fontSize: 14 }}>{e.label}</span>
                    {e.free && <span style={{
                      padding: '1px 6px', borderRadius: 4, fontSize: 10,
                      background: 'rgba(0,184,148,0.15)', color: '#00B894',
                    }}>免费</span>}
                  </div>
                  <div style={{ color: '#666', fontSize: 11, marginTop: 2 }}>{e.desc}</div>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* API Key */}
      {searchEnabled && (
        <div style={{
          margin: '12px 16px 0', background: 'rgba(255,255,255,0.03)', borderRadius: 16,
          padding: '18px 16px', border: '1px solid rgba(255,255,255,0.05)',
        }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 14, color: '#fff', fontSize: 14, fontWeight: 600 }}>
            <Key size={18} />
            <span>API Key</span>
          </div>
          <input type="password" placeholder="输入搜索引擎 API Key（可选）" style={{
            width: '100%', padding: '10px 14px', borderRadius: 10,
            background: 'rgba(255,255,255,0.06)', border: '1px solid rgba(255,255,255,0.08)',
            color: '#eee', fontSize: 13, outline: 'none', boxSizing: 'border-box',
          }} />
        </div>
      )}
    </div>
  )
}
