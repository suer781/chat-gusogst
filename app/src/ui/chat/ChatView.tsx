import { useRef, useEffect, useState } from 'react'
import { useChatStore, useSettingsStore } from '../stores'
import { Settings, Smile, Send, Square, Trash2 } from 'lucide-react'
import ReactMarkdown from 'react-markdown'

export function ChatView({ onOpenSettings, onOpenPersona }: {
  onOpenSettings: () => void
  onOpenPersona: () => void
}) {
  const { messages, isStreaming, error, sendMessage, abort, clear } = useChatStore()
  const config = useSettingsStore(s => s.config)
  const [input, setInput] = useState('')
  const messagesEndRef = useRef<HTMLDivElement>(null)
  const inputRef = useRef<HTMLTextAreaElement>(null)

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages])

  const handleSend = () => {
    const text = input.trim()
    if (!text || isStreaming) return
    setInput('')
    sendMessage(text)
    inputRef.current?.focus()
  }

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      handleSend()
    }
  }

  return (
    <div className="chat-view">
      {/* 顶栏 */}
      <div className="chat-header">
        <div className="persona-badge" onClick={onOpenPersona}>
          <span className="persona-name">{config.persona.name}</span>
        </div>
        <div className="header-actions">
          <button onClick={clear} className="icon-btn" title="清空对话">
            <Trash2 size={18} />
          </button>
          <button onClick={onOpenSettings} className="icon-btn" title="设置">
            <Settings size={18} />
          </button>
        </div>
      </div>

      {/* 消息列表 */}
      <div className="chat-messages">
        {messages.length === 0 && (
          <div className="empty-state">
            <div className="empty-emoji">💕</div>
            <p>说点什么吧~</p>
          </div>
        )}
        {messages.map((msg, i) => (
          <div key={i} className={`message message-${msg.role}`}>
            <div className="message-bubble">
              {msg.role === 'assistant' ? (
                <ReactMarkdown>{msg.content ?? ''}</ReactMarkdown>
              ) : (
                <p>{msg.content}</p>
              )}
            </div>
            {msg.timestamp && (
              <div className="message-time">
                {new Date(msg.timestamp).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })}
              </div>
            )}
          </div>
        ))}
        {error && <div className="error-banner">⚠️ {error}</div>}
        <div ref={messagesEndRef} />
      </div>

      {/* 输入栏 */}
      <div className="chat-input">
        <textarea
          ref={inputRef}
          value={input}
          onChange={e => setInput(e.target.value)}
          onKeyDown={handleKeyDown}
          placeholder="说点什么..."
          rows={1}
          disabled={isStreaming}
        />
        <button
          onClick={isStreaming ? abort : handleSend}
          className={`send-btn ${isStreaming ? 'streaming' : ''}`}
          disabled={!input.trim() && !isStreaming}
        >
          {isStreaming ? <Square size={18} /> : <Send size={18} />}
        </button>
      </div>
    </div>
  )
}
