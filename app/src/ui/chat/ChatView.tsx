import { bridge } from '../../bridge'
import { useState, useRef, useEffect, useCallback } from 'react'
import { useChatStore, useSettingsStore } from '../stores'
import type { Message, ToolCall } from '../types'
import { Plus, Search, Database, Send, Copy, RefreshCw, Loader2, AlertCircle, ChevronDown, ChevronRight, Square, Wrench, CheckCircle2, XCircle } from 'lucide-react'
import ReactMarkdown from 'react-markdown'
import { t, onLangChange } from '../i18n'

function genId() { return Date.now().toString(36) + Math.random().toString(36).slice(2, 8) }

/* ─── 思考折叠块 ─── */
function ThinkingBlock({ content, showThinking }: { content: string; showThinking: boolean }) {
  const [collapsed, setCollapsed] = useState(true)
  if (!showThinking) return null
  return (
    <div className="my-1 rounded-lg" style={{ background: '#1a1a3a', border: '1px solid #2a2a5a' }}>
      <button onClick={() => setCollapsed(!collapsed)} className="flex items-center gap-1 w-full px-3 py-1.5 text-left" style={{ fontSize: 12, color: '#8888aa' }}>
        {collapsed ? <ChevronRight size={14} /> : <ChevronDown size={14} />}
        <span>💭 思考过程</span>
        <span style={{ color: '#555', marginLeft: 'auto', fontSize: 11 }}>{content.length} 字</span>
      </button>
      {!collapsed && (
        <div className="px-3 pb-2" style={{ fontSize: 13, color: '#aaaacc', whiteSpace: 'pre-wrap', borderTop: '1px solid #2a2a5a', paddingTop: 8 }}>
          {content}
        </div>
      )}
    </div>
  )
}

/* ─── 工具调用卡片 ─── */
function ToolCallCard({ tc, showToolCalls }: { tc: ToolCall; showToolCalls: boolean }) {
  const [expanded, setExpanded] = useState(false)
  if (!showToolCalls) return null
  const statusIcon = tc.status === 'running' ? <Loader2 size={12} className="animate-spin" style={{ color: '#4a9eff' }} />
    : tc.status === 'done' ? <CheckCircle2 size={12} style={{ color: '#4caf50' }} />
    : <XCircle size={12} style={{ color: '#e94560' }} />
  return (
    <div className="my-1 rounded-lg" style={{ background: '#1a1a3a', border: '1px solid #2a2a5a' }}>
      <button onClick={() => setExpanded(!expanded)} className="flex items-center gap-2 w-full px-3 py-1.5 text-left" style={{ fontSize: 12, color: '#8888aa' }}>
        <Wrench size={12} style={{ color: '#4a9eff' }} />
        {statusIcon}
        <span style={{ fontFamily: 'monospace' }}>{tc.tool}</span>
        <span style={{ color: '#555', marginLeft: 'auto', fontSize: 11 }}>{expanded ? '▾' : '▸'}</span>
      </button>
      {expanded && (
        <div className="px-3 pb-2" style={{ fontSize: 12, borderTop: '1px solid #2a2a5a', paddingTop: 8 }}>
          <div style={{ color: '#6666aa', marginBottom: 4 }}>输入：</div>
          <pre style={{ color: '#aaaacc', whiteSpace: 'pre-wrap', margin: 0, fontFamily: 'monospace', fontSize: 11 }}>{JSON.stringify(tc.input, null, 2)}</pre>
          {tc.output && (<>
            <div style={{ color: '#6666aa', margin: '8px 0 4px' }}>输出：</div>
            <pre style={{ color: '#aaaacc', whiteSpace: 'pre-wrap', margin: 0, fontFamily: 'monospace', fontSize: 11, maxHeight: 200, overflow: 'auto' }}>{tc.output}</pre>
          </>)}
        </div>
      )}
    </div>
  )
}

/* ─── 消息操作栏 ─── */
function MessageActions({ msg, onCopy, onRetry }: { msg: Message; onCopy: () => void; onRetry?: () => void }) {
  const [show, setShow] = useState(false)
  return (
    <div className="flex items-center gap-1 mt-1" style={{ opacity: show ? 1 : 0, transition: 'opacity 0.15s' }}
      onMouseEnter={() => setShow(true)} onMouseLeave={() => setShow(false)}>
      <button onClick={onCopy} className="p-1 rounded hover:bg-white/5" title="复制">
        <Copy size={12} style={{ color: '#666' }} />
      </button>
      {onRetry && msg.role === 'assistant' && (
        <button onClick={onRetry} className="p-1 rounded hover:bg-white/5" title="重新生成">
          <RefreshCw size={12} style={{ color: '#666' }} />
        </button>
      )}
    </div>
  )
}

/* ─── 主组件 ─── */
export function ChatView({ onNavigate }: { onNavigate?: (v: any) => void }) {
  const [input, setInput] = useState('')
  const [, forceUpdate] = useState(0)
  const endRef = useRef<HTMLDivElement>(null)
  const messages = useChatStore((s) => s.messages)
  const streaming = useChatStore((s) => s.streaming)
  const error = useChatStore((s) => s.error)
  const addMessage = useChatStore((s) => s.addMessage)
  const updateMessage = useChatStore((s) => s.updateMessage)
  const clearMessages = useChatStore((s) => s.clearMessages)
  const setStreaming = useChatStore((s) => s.setStreaming)
  const setError = useChatStore((s) => s.setError)
  const persona = useSettingsStore((s) => s.config.persona)
  const showThinking = useSettingsStore((s) => s.config.showThinking)
  const showToolCalls = useSettingsStore((s) => s.config.showToolCalls)

  useEffect(() => { onLangChange(() => forceUpdate((n) => n + 1)) }, [])
  useEffect(() => { endRef.current?.scrollIntoView({ behavior: 'smooth' }) }, [messages, streaming])

  const send = useCallback(async (text?: string) => {
    const content = (text ?? input).trim()
    if (!content || streaming) return
    if (!text) setInput('')
    addMessage({ id: genId(), role: 'user', content, timestamp: Date.now() })
    setStreaming(true)
    setError(null)
    const cfg = useSettingsStore.getState().config
    const assistantId = genId()
    let accumulated = ''
    let currentThinking = ''
    let toolCalls: ToolCall[] = []
    try {
      for await (const evt of bridge.chat(content, cfg)) {
        if (evt.type === 'text_delta') {
          accumulated += evt.data
          addMessage({ id: assistantId, role: 'assistant', content: accumulated, timestamp: Date.now(), thinking: currentThinking ? [{ id: 't', content: currentThinking, collapsed: true }] : undefined, toolCalls: toolCalls.length ? toolCalls : undefined }, true)
        } else if (evt.type === 'thinking') {
          currentThinking += evt.data
          addMessage({ id: assistantId, role: 'assistant', content: accumulated, timestamp: Date.now(), thinking: [{ id: 't', content: currentThinking, collapsed: true }], toolCalls: toolCalls.length ? toolCalls : undefined }, true)
        } else if (evt.type === 'tool_use') {
          const tc: ToolCall = { id: genId(), tool: evt.data.tool, input: evt.data.input, status: 'running' }
          toolCalls = [...toolCalls, tc]
          addMessage({ id: assistantId, role: 'assistant', content: accumulated, timestamp: Date.now(), thinking: currentThinking ? [{ id: 't', content: currentThinking, collapsed: true }] : undefined, toolCalls }, true)
        } else if (evt.type === 'tool_result') {
          toolCalls = toolCalls.map(tc => tc.tool === evt.data.tool && tc.status === 'running' ? { ...tc, output: evt.data.output, status: 'done' as const } : tc)
          addMessage({ id: assistantId, role: 'assistant', content: accumulated, timestamp: Date.now(), toolCalls }, true)
        } else if (evt.type === 'error') {
          setError(evt.data)
        }
      }
      if (!accumulated && !toolCalls.length) addMessage({ id: assistantId, role: 'assistant', content: '(无回复)', timestamp: Date.now() })
    } catch (err: any) {
      setError(err.message || '发送失败')
    } finally {
      setStreaming(false)
    }
  }, [input, streaming, addMessage, updateMessage, setStreaming, setError])

  const handleCopy = (content: string) => {
    navigator.clipboard?.writeText(content).catch(() => {})
  }

  const handleRetry = (msg: Message) => {
    const idx = messages.findIndex(m => m.id === msg.id)
    if (idx < 0) return
    const userMsg = messages.slice(0, idx).reverse().find(m => m.role === 'user')
    if (userMsg) send(userMsg.content)
  }

  const handleStop = () => {
    bridge.abort()
    setStreaming(false)
  }

  return (
    <div className="h-full flex flex-col" style={{ background: '#0f0f23' }}>
      {/* ─── 顶栏 ─── */}
      <div className="shrink-0 flex items-center justify-between" style={{ padding: '8px 16px', borderBottom: '1px solid #1a1a3a' }}>
        <div className="flex items-center gap-2">
          <div className="flex items-center justify-center rounded-full" style={{ width: 32, height: 32, background: '#e9456020', color: '#e94560', fontSize: 14, fontWeight: 600 }}>{persona.name[0]}</div>
          <div>
            <div style={{ fontSize: 14, fontWeight: 600 }}>{persona.name}</div>
            <div style={{ fontSize: 11, color: '#666' }}>{persona.tags?.join(' · ') || t('chat.aiAssistant')}</div>
          </div>
        </div>
        <div className="flex items-center gap-1">
          <button className="p-2 rounded-lg hover:bg-white/5" title={t('chat.search')}><Search size={18} style={{ color: '#666' }} /></button>
          <button className="p-2 rounded-lg hover:bg-white/5" title={t('settings.memory')}><Database size={18} style={{ color: '#666' }} /></button>
          <button className="p-2 rounded-lg hover:bg-white/5" title={t('chat.newChat')} onClick={clearMessages}><Plus size={18} style={{ color: '#666' }} /></button>
        </div>
      </div>

      {/* ─── 消息列表 ─── */}
      <div className="flex-1 overflow-y-auto" style={{ padding: "16px", overscrollBehavior: "contain" }}>
        {messages.length === 0 ? (
          <div className="flex flex-col items-center justify-center h-full" style={{ color: '#444' }}>
            <div style={{ fontSize: 48, marginBottom: 16 }}>✦</div>
            <div style={{ fontSize: 18, fontWeight: 600, marginBottom: 8 }}>{t('chat.startTitle')}</div>
            <div style={{ fontSize: 13 }}>{t('chat.startHint')}</div>
          </div>
        ) : (
          <div className="flex flex-col gap-3">
            {messages.map((msg) => (
              <div key={msg.id} className="flex flex-col" style={{ alignItems: msg.role === 'user' ? 'flex-end' : 'flex-start' }}>
                {/* 思考块 */}
                {msg.thinking?.map(th => (
                  <ThinkingBlock key={th.id} content={th.content} showThinking={showThinking} />
                ))}
                {/* 工具调用 */}
                {msg.toolCalls?.map(tc => (
                  <ToolCallCard key={tc.id} tc={tc} showToolCalls={showToolCalls} />
                ))}
                {/* 消息气泡 */}
                <div className="rounded-2xl" style={{
                  maxWidth: '80%', padding: '10px 14px',
                  background: msg.role === 'user' ? '#e94560' : '#1a1a3a',
                  color: msg.role === 'user' ? '#fff' : '#ddd',
                  borderTopRightRadius: msg.role === 'user' ? 4 : undefined,
                  borderTopLeftRadius: msg.role !== 'user' ? 4 : undefined,
                }}>
                  {msg.role === 'user' ? (
                    <div style={{ whiteSpace: 'pre-wrap' }}>{msg.content}</div>
                  ) : (
                    <div style={{ fontSize: 14, lineHeight: 1.6 }}>
                      <ReactMarkdown>{msg.content || ' '}</ReactMarkdown>
                    </div>
                  )}
                </div>
                {/* 错误信息 */}
                {msg.error && (
                  <div className="flex items-center gap-1 mt-1 px-2 py-1 rounded" style={{ background: '#e9456020', color: '#e94560', fontSize: 12 }}>
                    <AlertCircle size={12} /> {msg.error}
                  </div>
                )}
                {/* 操作栏 */}
                <MessageActions msg={msg} onCopy={() => handleCopy(msg.content)} onRetry={msg.role === 'assistant' ? () => handleRetry(msg) : undefined} />
              </div>
            ))}
            {/* 流式加载指示 */}
            {streaming && (
              <div className="flex items-center gap-2" style={{ color: '#666', fontSize: 13 }}>
                <Loader2 size={14} className="animate-spin" />
                <span>{t('chat.thinking')}</span>
              </div>
            )}
            {/* 全局错误 */}
            {error && !messages.some(m => m.error) && (
              <div className="flex items-center gap-2 px-3 py-2 rounded-lg" style={{ background: '#e9456020', color: '#e94560', fontSize: 13 }}>
                <AlertCircle size={14} />
                <span>{error}</span>
                <button onClick={() => { setError(null); const last = messages[messages.length - 1]; if (last?.role === 'user') send(last.content) }}
                  className="ml-auto px-2 py-0.5 rounded text-xs" style={{ background: '#e9456030', color: '#e94560' }}>重试</button>
              </div>
            )}
            <div ref={endRef} />
          </div>
        )}
      </div>

      {/* ─── 输入栏 ─── */}
      <div className="shrink-0" style={{ padding: '12px 16px', borderTop: '1px solid #1a1a3a', background: '#0f0f23' }}>
        <div className="flex items-end gap-2">
          <textarea
            className="flex-1 resize-none rounded-xl"
            style={{ background: '#1a1a3a', color: '#ddd', padding: '10px 14px', fontSize: 14, border: '1px solid #2a2a5a', outline: 'none', maxHeight: 120, minHeight: 40 }}
            rows={1}
            placeholder={t('chat.placeholder')}
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={(e) => { if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); send() } }}
          />
          {streaming ? (
            <button onClick={handleStop}
              className="flex items-center justify-center rounded-xl" style={{ width: 40, height: 40, background: '#e94560', color: '#fff' }}>
              <Square size={16} fill="#fff" />
            </button>
          ) : (
            <button onClick={() => send()}
              className="flex items-center justify-center rounded-xl" style={{ width: 40, height: 40, background: input.trim() ? '#e94560' : '#2a2a5a', color: input.trim() ? '#fff' : '#666', cursor: input.trim() ? 'pointer' : 'default' }}>
              <Send size={18} />
            </button>
          )}
        </div>
      </div>
    </div>
  )
}
