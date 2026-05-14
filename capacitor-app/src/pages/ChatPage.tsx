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
      for await (const event of chatStream(model, persona, updated)) {
        if (event.type === 'token') buffer += event.content
        else if (event.type === 'error') { setError(event.message); break }
      }
    } catch (e: any) { setError(e.message) }

    if (buffer) {
      const reply: Message = { role: 'assistant', content: buffer, timestamp: Date.now() }
      setMessages([...updated, reply])
    }
    setStreaming(false)
  }, [input, streaming, model, persona, messages, setMessages])

  const handleKey = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); send() }
  }

  return (
    <div className="chat-page">
      <header className="chat-header">
        <span className="header-title">{persona.avatar} {persona.name}</span>
        <div className="header-actions">
          <button className="icon-btn" onClick={clearMessages} title="清空">🗑️</button>
          <button className="icon-btn" onClick={() => navigate('/settings')} title="设置">⚙️</button>
        </div>
      </header>

      <div className="chat-list" ref={listRef}>
        {messages.length === 0 && (
          <div className="chat-empty">
            <span className="empty-emoji">💕</span>
            <span className="empty-text">说点什么吧~</span>
          </div>
        )}
        {messages.map((msg, i) => (
          <div key={i} className={'bubble-row ' + msg.role}>
            <div className={'bubble ' + (msg.role === 'error' ? 'error' : msg.role)}>
              {msg.content}
            </div>
          </div>
        ))}
        {streaming && (
          <div className="bubble-row assistant">
            <div className="bubble assistant loading">
              <span className="dot" /><span className="dot" /><span className="dot" />
            </div>
          </div>
        )}
      </div>

      {error && (
        <div className="error-bar">
          <span>{error}</span>
          <button onClick={() => setError(null)}>✕</button>
        </div>
      )}

      <div className="chat-input">
        <textarea
          ref={inputRef}
          value={input}
          onChange={e => setInput(e.target.value)}
          onKeyDown={handleKey}
          placeholder="说点什么..."
          rows={1}
        />
        <button className="send-btn" onClick={send} disabled={!input.trim() || streaming}>
          ➤
        </button>
      </div>
    </div>
  )
}