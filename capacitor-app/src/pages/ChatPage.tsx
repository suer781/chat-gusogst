import { useState, useRef, useEffect, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import { chatStream } from '../lib/api'
import { useModel, usePersona, useMessages } from '../hooks/useStore'
import type { Message } from '../lib/types'
import './ChatPage.css'

export default function ChatPage() {
  const navigate = useNavigate()
  const { model } = useModel()
  const { persona } = usePersona()
  const { messages, setMessages, clearMessages } = useMessages()
  const [input, setInput] = useState('')
  const [streaming, setStreaming] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const listRef = useRef<HTMLDivElement>(null)
  const inputRef = useRef<HTMLTextAreaElement>(null)

  useEffect(() => {
    const el = listRef.current
    if (el) el.scrollTop = el.scrollHeight
  }, [messages])

  const send = useCallback(async () => {
    const text = input.trim()
    if (!text || streaming) return
    if (!model.apiKey) { setError('请先在设置中配置 API Key'); return }

    const userMsg: Message = { role: 'user', content: text, timestamp: Date.now() }
    const updated = [...messages, userMsg]
    setMessages(updated)
    setInput('')
    setStreaming(true)
    setError(null)

    let buffer = ''
    try {
      for await (const ev of chatStream(model, persona, updated)) {
        if (ev.token) { buffer += ev.token }
        else if (ev.error) { setError(ev.error); break }
      }
      if (buffer) {
        setMessages(prev => [...prev, { role: 'assistant', content: buffer, timestamp: Date.now() }])
      }
    } catch (e: unknown) {
      setError(String(e))
    } finally {
      setStreaming(false)
    }
  }, [input, streaming, model, persona, messages, setMessages])

  const handleKey = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); send() }
  }

  return (
    <div className="chat-page">
      <header className="chat-header">
        <div className="chat-persona" onClick={() => navigate('/persona')}>
          <span className="chat-persona-avatar">{persona.avatar}</span>
          <span className="chat-persona-name">{persona.name}</span>
          <span className="chat-persona-switch">▾</span>
        </div>
        <div className="chat-actions">
          <button className="chat-action-btn" onClick={() => navigate('/settings')} title="记忆与设置">🧠</button>
          <button className="chat-action-btn" onClick={clearMessages} title="清空对话">🗑️</button>
        </div>
      </header>

      <div className="chat-list" ref={listRef}>
        {messages.length === 0 && (
          <div className="chat-empty">
            <div className="empty-avatar">{persona.avatar}</div>
            <p>和 {persona.name} 说点什么吧~</p>
          </div>
        )}
        {messages.map((m, i) => (
          <div key={i} className={'bubble ' + m.role}>
            {m.content}
          </div>
        ))}
        {streaming && (
          <div className="bubble assistant streaming">
            <span className="dot" /><span className="dot" /><span className="dot" />
          </div>
        )}
      </div>

      {error && (
        <div className="error-bar">
          ⚠️ {error}
          <button onClick={() => setError(null)}>✕</button>
        </div>
      )}

      <div className="chat-input">
        <textarea
          ref={inputRef}
          value={input}
          onChange={e => setInput(e.target.value)}
          onKeyDown={handleKey}
          placeholder="输入消息..."
          rows={1}
        />
        <button className="send-btn" onClick={send} disabled={!input.trim() || streaming}>
          {streaming ? '⏳' : '➤'}
        </button>
      </div>
    </div>
  )
}
