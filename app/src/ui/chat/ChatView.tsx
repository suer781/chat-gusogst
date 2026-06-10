import { bridge } from '../../bridge'
import { useState, useRef, useEffect, useCallback } from 'react'
import { useChatStore, useSettingsStore } from '../stores'
import type { UIMessage as Message, UIToolCall as ToolCall } from '../types'
import { Plus, Search, Database, Send, Copy, RefreshCw, Loader2, AlertCircle, ChevronDown, ChevronRight, Square, Wrench, CheckCircle2, XCircle } from 'lucide-react'
import LazyMarkdown from './LazyMarkdown'
import { t, onLangChange } from '../i18n'
import { success as hapticSuccess, light as hapticLight, medium as hapticMedium, heavy as hapticHeavy, sendPulse, unfold as hapticUnfold, error as hapticError, glassTap, glassPress } from '../haptics'

function genId() { return Date.now().toString(36) + Math.random().toString(36).slice(2, 8) }

/* ─── 思考折叠块 ─── */
function ThinkingBlock({ content, showThinking }: { content: string; showThinking: boolean }) {
  const [collapsed, setCollapsed] = useState(true)
  if (!showThinking) return null
  return (
    <div className="my-1 rounded-lg" style={{ background: 'var(--bg-tertiary)', border: '1px solid var(--border)' }}>
      <button onClick={() => { hapticUnfold(); setCollapsed(!collapsed) }} className="flex items-center gap-1 w-full px-3 py-1.5 text-left" style={{ fontSize: "var(--text-sm)", color: 'var(--gray-300)' }}>
        {collapsed ? <ChevronRight size={14} /> : <ChevronDown size={14} />}
        <span>💭 思考过程</span>
        <span style={{ color: 'var(--gray-400)', marginLeft: 'auto', fontSize: "var(--text-xs)" }}>{content.length} 字</span>
      </button>
      {!collapsed && (
        <div className="px-3 pb-2" style={{ fontSize: "var(--text-base)", color: 'var(--text-primary)', whiteSpace: 'pre-wrap', borderTop: '1px solid var(--border)', paddingTop: "var(--space-2)" }}>
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
  const statusIcon = tc.status === 'running' ? <Loader2 size={12} className="animate-spin" style={{ color: 'var(--blue)' }} />
    : tc.status === 'done' ? <CheckCircle2 size={12} style={{ color: 'var(--success)' }} />
    : <XCircle size={12} style={{ color: 'var(--accent)' }} />
  return (
    <div className="my-1 rounded-lg" style={{ background: 'var(--bg-tertiary)', border: '1px solid var(--border)' }}>
      <button onClick={() => { hapticUnfold(); setExpanded(!expanded) }} className="flex items-center gap-2 w-full px-3 py-1.5 text-left" style={{ fontSize: "var(--text-sm)", color: 'var(--gray-300)' }}>
        <Wrench size={12} style={{ color: 'var(--blue)' }} />
        {statusIcon}
        <span style={{ fontFamily: 'monospace' }}>{tc.tool}</span>
        <span style={{ color: 'var(--gray-400)', marginLeft: 'auto', fontSize: "var(--text-xs)" }}>{expanded ? '▾' : '▸'}</span>
      </button>
      {expanded && (
        <div className="px-3 pb-2" style={{ fontSize: "var(--text-sm)", borderTop: '1px solid var(--border)', paddingTop: "var(--space-2)" }}>
          <div style={{ color: 'var(--gray-300)', marginBottom: 4 }}>输入：</div>
          <pre style={{ color: 'var(--gray-100)', whiteSpace: 'pre-wrap', margin: 0, fontFamily: 'monospace', fontSize: "var(--text-xs)" }}>{JSON.stringify(tc.input, null, 2)}</pre>
          {tc.output && (<>
            <div style={{ color: 'var(--gray-300)', margin: '8px 0 4px' }}>输出：</div>
            <pre style={{ color: 'var(--gray-100)', whiteSpace: 'pre-wrap', margin: 0, fontFamily: 'monospace', fontSize: "var(--text-xs)", maxHeight: 200, overflow: 'auto' }}>{tc.output}</pre>
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
      <button onClick={() => { glassTap(); onCopy() }} className="p-1 rounded hover:bg-white/5" title="复制">
        <Copy size={12} style={{ color: 'var(--text-secondary)' }} />
      </button>
      {onRetry && msg.role === 'assistant' && (
        <button onClick={() => { hapticMedium(); onRetry?.() }} className="p-1 rounded hover:bg-white/5" title="重新生成">
          <RefreshCw size={12} style={{ color: 'var(--text-secondary)' }} />
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
  const persona = useSettingsStore((s) => s.persona)
  const showThinking = useSettingsStore((s) => s.showThinking)
  const showToolCalls = useSettingsStore((s) => s.showToolCalls)
  const showMemoryHints = useSettingsStore((s) => s.showMemoryHints)

  useEffect(() => { onLangChange(() => forceUpdate((n) => n + 1)) }, [])
  useEffect(() => { endRef.current?.scrollIntoView({ behavior: 'smooth' }) }, [messages, streaming])

  // ── RAf 节流：流式输出时，每帧最多 flush 一次 ──
  const rafId = useRef<number>(0)

  const send = useCallback(async (text?: string) => {
    const content = (text ?? input).trim()
    if (!content || streaming) return
    if (!text) setInput('')
    addMessage({ id: genId(), role: 'user', content, timestamp: Date.now() })
    setStreaming(true)
    setError(null)
    const cfg = useSettingsStore.getState()
    const assistantId = genId()
    let accumulated = ''
    let currentThinking = ''
    let toolCalls: ToolCall[] = []

    // RAf 批量 flush —— 同一帧内多次 delta 只最后提交一次
    const flushAccumulated = () => {
      addMessage({
        id: assistantId, role: 'assistant',
        content: accumulated, timestamp: Date.now(),
        thinking: currentThinking ? [{ id: 't', content: currentThinking, collapsed: true }] : undefined,
        toolCalls: toolCalls.length ? toolCalls : undefined,
      }, true)
      rafId.current = 0
    }

    try {
      for await (const evt of bridge.chat(content, cfg)) {
        if (evt.type === 'text_delta') {
          accumulated += evt.data
        } else if (evt.type === 'thinking') {
          currentThinking += evt.data
        } else if (evt.type === 'tool_use') {
          const tc: ToolCall = { id: genId(), tool: evt.data.tool, input: evt.data.input, status: 'running', agentEventId: evt.data.id }
          toolCalls = [...toolCalls, tc]
        } else if (evt.type === 'tool_result') {
          toolCalls = toolCalls.map(tc => {
            const match = evt.data.id ? tc.agentEventId === evt.data.id : (tc.tool === evt.data.tool && tc.status === 'running');
            return match ? { ...tc, output: evt.data.output, status: (evt.data.isError ? 'error' : 'done') as ToolCall['status'] } : tc;
          })
          // 工具结果立即提交（需要反馈状态变化）
          addMessage({ id: assistantId, role: 'assistant', content: accumulated, timestamp: Date.now(), toolCalls }, true)
        } else if (evt.type === 'error') {
          setError(evt.data)
        }

        // text_delta / thinking：用 RAf 批量提交，避免每字符渲染一次
        if (evt.type === 'text_delta' || evt.type === 'thinking') {
          if (!rafId.current) {
            rafId.current = requestAnimationFrame(flushAccumulated)
          }
        }
      }
      // 流结束：强制 flush
      if (rafId.current) { cancelAnimationFrame(rafId.current); rafId.current = 0 }
      addMessage({
        id: assistantId, role: 'assistant',
        content: accumulated, timestamp: Date.now(),
        thinking: currentThinking ? [{ id: 't', content: currentThinking, collapsed: true }] : undefined,
        toolCalls: toolCalls.length ? toolCalls : undefined,
      }, true)
      if (!accumulated && !toolCalls.length) addMessage({ id: assistantId, role: 'assistant', content: '(无回复)', timestamp: Date.now() })
      hapticSuccess()
    } catch (err: any) {
      if (rafId.current) { cancelAnimationFrame(rafId.current); rafId.current = 0 }
      setError(err.message || '发送失败')
    } finally {
      setStreaming(false)
    }
  }, [input, streaming, addMessage, updateMessage, setStreaming, setError])

  const handleCopy = (content: string) => {
    glassTap()
    navigator.clipboard?.writeText(content).catch(() => {})
  }

  const handleRetry = (msg: Message) => {
    hapticMedium()
    const idx = messages.findIndex(m => m.id === msg.id)
    if (idx < 0) return
    const userMsg = messages.slice(0, idx).reverse().find(m => m.role === 'user')
    if (userMsg) send(userMsg.content)
  }

  const handleStop = () => {
    hapticHeavy()
    bridge.abort()
    setStreaming(false)
  }

  return (
    <div className="h-full flex flex-col">
      {/* ─── 顶栏 ─── */}
      <div className="shrink-0 flex items-center justify-between" style={{ padding: 'var(--space-2) var(--space-4)', borderBottom: '1px solid var(--divider)' }}>
        <div className="flex items-center gap-2">
          <div className="flex items-center justify-center rounded-full" style={{ width: 32, height: 32, background: 'var(--accent-soft)', color: 'var(--accent)', fontSize: "var(--text-base)", fontWeight: 600 }}>{persona.name[0]}</div>
          <div>
            <div style={{ fontSize: "var(--text-base)", fontWeight: 600 }}>{persona.name}</div>
            <div style={{ fontSize: "var(--text-xs)", color: 'var(--text-secondary)' }}>{persona.tags?.join(' · ') || t('chat.aiAssistant')}</div>
          </div>
        </div>
        <div className="flex items-center gap-1">
          <button className="p-2 rounded-lg hover:bg-white/5" title={t('chat.newChat')} onClick={() => { glassTap(); clearMessages() }}><Plus size={18} style={{ color: 'var(--text-secondary)' }} /></button>
        </div>
      </div>

      {/* ─── 消息列表 ─── */}
      <div className="flex-1 overflow-y-auto" style={{ padding: "16px", overscrollBehavior: "contain", willChange: streaming ? 'contents' : 'auto' }}>
        {messages.length === 0 ? (
          <div className="flex flex-col items-center justify-center h-full" style={{ color: 'var(--text-secondary)' }}>
            <div style={{ fontSize: "var(--text-6xl)", marginBottom: "var(--space-4)" }}>✦</div>
            <div style={{ fontSize: "var(--text-xl)", fontWeight: 600, marginBottom: "var(--space-2)" }}>{t('chat.startTitle')}</div>
            <div style={{ fontSize: "var(--text-base)" }}>{t('chat.startHint')}</div>
          </div>
        ) : (
          <div className="flex flex-col gap-3">
            {messages.map((msg) => (
              <div key={msg.id} className="flex flex-col"
                style={{
                  alignItems: msg.role === 'user' ? 'flex-end' : 'flex-start',
                  contentVisibility: 'auto',
                  containIntrinsicSize: 'auto 80px',
                }}
              >
                {/* 思考块 */}
                {msg.thinking?.map((th: any) => (
                  <ThinkingBlock key={th.id} content={th.content} showThinking={showThinking} />
                ))}
                {/* 工具调用 */}
                {msg.toolCalls?.map((tc: any) => (
                  <ToolCallCard key={tc.id} tc={tc} showToolCalls={showToolCalls} />
                ))}
                {/* 消息气泡 */}
                <div className="rounded-2xl" style={{
                  maxWidth: '80%', padding: 'var(--space-3) var(--space-4)',
                  background: msg.role === 'user' ? 'var(--accent)' : 'var(--bg-tertiary)',
                  color: msg.role === 'user' ? 'var(--text-primary)' : 'var(--gray-100)',
                  borderTopRightRadius: msg.role === 'user' ? 4 : undefined,
                  borderTopLeftRadius: msg.role !== 'user' ? 4 : undefined,
                }}>
                  {msg.role === 'user' ? (
                    <div style={{ whiteSpace: 'pre-wrap' }}>{msg.content}</div>
                  ) : (
                    <div style={{ fontSize: "var(--text-base)", lineHeight: 1.6 }}>
                      <LazyMarkdown content={msg.content || ' '} isStreaming={streaming && msg.id === messages[messages.length - 1]?.id} />
                    </div>
                  )}
                </div>
                {/* 错误信息 */}
                {msg.error && (
                  <div className="flex items-center gap-1 mt-1 px-2 py-1 rounded" style={{ background: 'var(--accent-soft)', color: 'var(--accent)', fontSize: "var(--text-sm)" }}>
                    <AlertCircle size={12} /> {msg.error}
                  </div>
                )}
                {/* 操作栏 */}
                <MessageActions msg={msg} onCopy={() => handleCopy(msg.content)} onRetry={msg.role === 'assistant' ? () => handleRetry(msg) : undefined} />
              </div>
            ))}
            {/* 流式加载指示 */}
            {streaming && (
              <div className="flex items-center gap-2" style={{ color: 'var(--text-secondary)', fontSize: "var(--text-base)" }}>
                <Loader2 size={14} className="animate-spin" />
                <span>{t('chat.thinking')}</span>
              </div>
            )}
            {/* 全局错误 */}
            {error && !messages.some(m => m.error) && (
              <div className="flex items-center gap-2 px-3 py-2 rounded-lg" style={{ background: 'var(--accent-soft)', color: 'var(--accent)', fontSize: "var(--text-base)" }}>
                <AlertCircle size={14} />
                <span>{error}</span>
                <button onClick={() => { hapticMedium(); setError(null); const last = messages[messages.length - 1]; if (last?.role === 'user') send(last.content) }}
                  className="ml-auto px-2 py-0.5 rounded text-xs" style={{ background: 'rgba(233, 69, 96, 0.2)', color: 'var(--accent)' }}>重试</button>
              </div>
            )}
            <div ref={endRef} />
          </div>
        )}
      </div>

      {/* ─── 输入栏 ─── */}
      <div className="shrink-0" style={{ padding: 'var(--space-3) var(--space-4)', borderTop: '1px solid var(--divider)', background: 'var(--bg-primary)' }}>
        <div className="flex items-end gap-2">
          <textarea
            className="flex-1 resize-none rounded-xl"
            style={{ background: 'var(--bg-tertiary)', color: 'var(--text-primary)', padding: 'var(--space-3) var(--space-4)', fontSize: "var(--text-base)", border: '1px solid var(--border)', outline: 'none', maxHeight: 120, minHeight: 40 }}
            rows={1}
            placeholder={t('chat.placeholder')}
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={(e) => { if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); sendPulse(); send() } }}
          />
          {streaming ? (
            <button onClick={handleStop}
              className="flex items-center justify-center rounded-xl" style={{ width: 40, height: 40, background: 'var(--accent)', color: 'var(--text-primary)' }}>
              <Square size={16} fill="var(--text-primary)" />
            </button>
          ) : (
            <button onClick={() => { sendPulse(); send() }}
              className="flex items-center justify-center rounded-xl" style={{ width: 40, height: 40, background: input.trim() ? 'var(--accent)' : 'var(--border)', color: input.trim() ? 'var(--text-primary)' : 'var(--gray-400)', cursor: input.trim() ? 'pointer' : 'default' }}>
              <Send size={18} />
            </button>
          )}
        </div>
      </div>
    </div>
  )
}
