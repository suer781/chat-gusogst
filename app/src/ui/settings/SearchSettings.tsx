import { useSettingsStore } from '../stores'
import { t } from '../i18n'
import { Search, Key, Globe } from 'lucide-react'
import { light as hapticLight, medium as hapticMedium, glassTap } from '../haptics'

const ENGINES = [
  { id: 'duckduckgo', label: 'DuckDuckGo', desc: t('search.duckduckgo.desc'), free: true },
  { id: 'tavily',     label: 'Tavily',     desc: t('search.tavily.desc'), free: false },
  { id: 'serpapi',     label: 'SerpAPI',    desc: t('search.serpapi.desc'), free: false },
  { id: 'bing',        label: 'Bing',       desc: t('search.bing.desc'), free: false },
]

export function SearchSettings({ onBack }: { onBack: () => void }) {
  const searchEnabled = useSettingsStore((s) => s.searchEnabled)
  const setSearchEnabled = useSettingsStore((s) => s.setSearchEnabled)

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
        <span style={{ fontSize: "var(--text-xl)", fontWeight: 600, color: 'var(--text-primary)' }}>搜索</span>
      </div>

      {/* Toggle */}
      <div style={{
        margin: '16px 16px 0', background: 'rgba(255,255,255,0.03)', borderRadius: "var(--radius-lg)",
        padding: '18px 16px', border: '1px solid rgba(255,255,255,0.05)',
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 14, color: 'var(--text-primary)', fontSize: "var(--text-base)", fontWeight: 600 }}>
          <Search size={18} />
          <span>联网搜索</span>
        </div>
        <div onClick={() => { hapticMedium(); setSearchEnabled(!searchEnabled) }} style={{
          display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '10px 0', cursor: 'pointer',
        }}>
          <div>
            <div style={{ color: 'var(--gray-50)', fontSize: "var(--text-base)" }}>启用搜索</div>
            <div style={{ color: 'var(--gray-400)', fontSize: "var(--text-sm)", marginTop: 2 }}>AI 可以搜索互联网获取最新信息</div>
          </div>
          <div style={{
            width: 46, height: 26, borderRadius: "var(--radius-md)", flexShrink: 0, marginLeft: 12,
            background: searchEnabled ? 'var(--warning)' : 'rgba(255,255,255,0.1)',
            position: 'relative', transition: 'background 0.25s',
          }}>
            <div style={{
              width: 22, height: 22, borderRadius: 11, background: 'var(--text-primary)',
              position: 'absolute', top: 2, left: searchEnabled ? 22 : 2,
              transition: 'left 0.25s cubic-bezier(0.4,0,0.2,1)',
              boxShadow: '0 1px 3px rgba(0, 0, 0, 0.3)',
            }} />
          </div>
        </div>
      </div>

      {/* Engine Selection */}
      {searchEnabled && (
        <div style={{
          margin: '12px 16px 0', background: 'rgba(255,255,255,0.03)', borderRadius: "var(--radius-lg)",
          padding: '18px 16px', border: '1px solid rgba(255,255,255,0.05)',
        }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 14, color: 'var(--text-primary)', fontSize: "var(--text-base)", fontWeight: 600 }}>
            <Globe size={18} />
            <span>搜索引擎</span>
          </div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
            {ENGINES.map((e) => (
              <div key={e.id} style={{
                padding: '12px 14px', borderRadius: "var(--radius-md)",
                background: 'rgba(255,255,255,0.04)',
                border: '1px solid var(--divider)',
                display: 'flex', justifyContent: 'space-between', alignItems: 'center',
                cursor: 'pointer',
              }}>
                <div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                    <span style={{ color: 'var(--text-primary)', fontSize: "var(--text-base)" }}>{e.label}</span>
                    {e.free && <span style={{
                      padding: '1px 6px', borderRadius: 4, fontSize: 10,
                      background: 'rgba(0,184,148,0.15)', color: 'var(--teal)',
                    }}>免费</span>}
                  </div>
                  <div style={{ color: 'var(--gray-400)', fontSize: "var(--text-xs)", marginTop: 2 }}>{e.desc}</div>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* API Key */}
      {searchEnabled && (
        <div style={{
          margin: '12px 16px 0', background: 'rgba(255,255,255,0.03)', borderRadius: "var(--radius-lg)",
          padding: '18px 16px', border: '1px solid rgba(255,255,255,0.05)',
        }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 14, color: 'var(--text-primary)', fontSize: "var(--text-base)", fontWeight: 600 }}>
            <Key size={18} />
            <span>API Key</span>
          </div>
          <input type="password" placeholder="输入搜索引擎 API Key（可选）" style={{
            width: '100%', padding: '10px 14px', borderRadius: "var(--radius-md)",
            background: 'var(--divider)', border: '1px solid var(--border-color)',
            color: 'var(--gray-50)', fontSize: "var(--text-base)", outline: 'none', boxSizing: 'border-box',
          }} />
        </div>
      )}
    </div>
  )
}
