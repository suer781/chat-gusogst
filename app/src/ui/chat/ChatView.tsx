import { useState, useRef, useEffect } from 'react'
import { useChatStore, useSettingsStore } from '../stores'
import type { Message } from '../types'
import { Plus, Search, Database, Send, Copy, Loader2, AlertCircle } from 'lucide-react'
import ReactMarkdown from 'react-markdown'
import { t, onLangChange } from '../i18n'

function genId() { return Date.now().toString(36) + Math.random().toString(36).slice(2, 8) }

export function ChatView({ onNavigate }: { onNavigate?: (v: any) => void }) {
  const [input, setInput] = useState('')
  const [, forceUpdate] = useState(0)
  const endRef = useRef<HTMLDivElement>(null)
  const messages = useChatStore((s) => s.messages)
  const streaming = useChatStore((s) => s.streaming)
  const error = useChatStore((s) => s.error)
  const addMessage = useChatStore((s) => s.addMessage)
  const clearMessages = useChatStore((s) => s.clearMessages)
  const setStreaming = useChatStore((s) => s.setStreaming)
  const setError = useChatStore((s) => s.setError)
  const persona = useSettingsStore((s) => s.config.persona)

  useEffect(() => onLangChange(() => forceUpdate((n) => n + 1)), [])
  useEffect(() => { endRef.current?.scrollIntoView({ behavior: 'smooth' }) }, [messages])

  const send = async () => {
    const text = input.trim()
    if (!text || streaming) return
    addMessage({ id: genId(), role: 'user', content: text, timestamp: Date.now() })
    setInput('')
    setStreaming(true)
    setError(null)
    setTimeout(() => {
      addMessage({ id: genId(), role: 'assistant', content: t('chat.noBackend'), timestamp: Date.now() })
      setStreaming(false)
    }, 500)
  }

  return (
    <div className="h-full flex flex-col" style={{ background: '#0f0f23' }}>
      <div className="shrink-0 flex items-center justify-between" style={{ padding: '8px 16px', borderBottom: '1px solid #1a1a3a' }}>
        <div className="flex items-center gap-2">
          <div className="flex items-center justify-center rounded-full" style={{ width: 32, height: 32, background: '#e9456020', color: '#e94560', fontSize: 14, fontWeight: 600 }}>{persona.name[0]}</div>
          <div>
            <div style={{ fontSize: 14, fontWeight: 600 }}>{persona.name}</div>
            <div style={{ fontSize: 11, color: '#666688' }}>{persona.tags?.join(', ') || t('chat.aiAssistant')}</div>
          </div>
        </div>
        <div className="flex items-center gap-1">
          <button style={{ color: '#666688', background: 'none', border: 'none', cursor: 'pointer', padding: 6, borderRadius: 6 }} title={t('chat.search')}><Search size={18} /></button>
          <button style={{ color: '#666688', background: 'none', border: 'none', cursor: 'pointer', padding: 6, borderRadius: 6 }} title={t('settings.memory')}><Database size={18} /></button>
          <button onClick={clearMessages} style={{ color: '#666688', background: 'none', border: 'none', cursor: 'pointer', padding: 6, borderRadius: 6 }} title={t('chat.newChat')}><Plus size={18} /></button>
        </div>
      </div>

      <div className="flex-1 overflow-y-auto" style={{ padding: 16 }}>
        {messages.length === 0 && (
          <div className="flex flex-col items-center justify-center h-full" style={{ color: '#4a4a6a' }}>
            <div style={{ fontSize: 48, marginBottom: 16 }}>✦</div>
            <div style={{ fontSize: 16, fontWeight: 500 }}>{t('chat.startTitle')}</div>
            <div style={{ fontSize: 13, marginTop: 4 }}>{t('chat.startHint')}</div>
          </div>
        )}
        {messages.map((msg) => (
          <div key={msg.id} className="flex flex-col" style={{ alignItems: msg.role === 'user' ? 'flex-end' : 'flex-start', marginBottom: 12 }}>
            <div style={{ maxWidth: '85%', padding: '10px 14px', borderRadius: 16, borderTopRightRadius: msg.role === 'user' ? 4 : 16, borderTopLeftRadius: msg.role === 'user' ? 16 : 4, background: msg.role === 'user' ? '#e94560' : '#1a1a3a', color: msg.role === 'user' ? '#fff' : '#e0e0e0', fontSize: 14, lineHeight: 1.6, wordBreak: 'break-word' }}>
              {msg.role === 'user' ? <span style={{ whiteSpace: 'pre-wrap' }}>{msg.content}</span> : <div className="prose-chat"><ReactMarkdown>{msg.content}</ReactMarkdown></div>}
            </div>
          </div>
        ))}
        {streaming && <div className="flex items-center gap-2" style={{ padding: '8px 0', color: '#8888aa' }}><Loader2 size={16} className="animate-spin" /><span style={{ fontSize: 13 }}>{t('chat.thinking')}</span></div>}
        {error && <div className="flex items-center gap-2" style={{ padding: '8px 0', color: '#e94560' }}><AlertCircle size={16} /><span style={{ fontSize: 13 }}>{error}</span></div>}
        <div ref={endRef} />
      </div>

      <div className="shrink-0" style={{ padding: '12px 16px', borderTop: '1px solid #1a1a3a', background: '#0a0a1a' }}>
        <div className="flex items-end gap-2">
          <textarea value={input} onChange={(e) => setInput(e.target.value)} onKeyDown={(e) => { if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); send() } }} placeholder={t('chat.placeholder')} rows={1} className="flex-1 resize-none outline-none" style={{ background: '#1a1a3a', border: '1px solid #2a2a4a', borderRadius: 12, padding: '10px 14px', fontSize: 14, color: '#e0e0e0', maxHeight: 120, lineHeight: 1.5 }} />
          <button onClick={send} disabled={!input.trim() || streaming} className="flex items-center justify-center shrink-0" style={{ width: 40, height: 40, background: input.trim() ? '#e94560' : '#2a2a4a', border: 'none', borderRadius: 12, cursor: input.trim() ? 'pointer' : 'default', color: input.trim() ? '#fff' : '#666688' }}><Send size={18} /></button>
        </div>
      </div>
    </div>
  )
}
