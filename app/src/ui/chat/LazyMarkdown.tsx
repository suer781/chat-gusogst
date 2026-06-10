/**
 * LazyMarkdown — 懒渲染 markdown，解决长句子卡顿
 *
 * 策略：
 *   1. 短内容（< 2000 字符）直接渲染 —— 开销可忽略
 *   2. 中等内容（2000-8000 字符）用 IntersectionObserver 只在
 *      消息进入视口时渲染，离开视口时用占位高度代替
 *   3. 长内容（> 8000 字符）分段渲染 —— 可见段先渲染，
 *      不可见段用空 div 占位（content-visibility: auto）
 *   4. 流式输出中的最新消息永远直接渲染（用户正在看）
 *
 * 原理：
 *   content-visibility: auto → 浏览器对不可见元素跳过 layout/paint
 *   这比 React 虚拟化更轻量，不引入额外 JS 计算
 */

import { useRef, useState, useEffect, useMemo } from 'react'
import ReactMarkdown from 'react-markdown'

interface Props {
  content: string
  isStreaming?: boolean   // 流式输出中——跳过懒加载，直接渲染
}

// ── 分段策略 ──
const CHUNK_THRESHOLD = 2000   // 低于此行不拆分
const BLOCK_THRESHOLD = 8000   // 高于此行做 content-visibility 分段

/**
 * 把长字符串切成 N 段，每段尽量在自然边界（换行/句末）断开
 */
function splitContent(content: string, chunkSize = 4000): string[] {
  if (content.length <= chunkSize) return [content]
  const chunks: string[] = []
  let remaining = content
  while (remaining.length > 0) {
    if (remaining.length <= chunkSize) {
      chunks.push(remaining)
      break
    }
    // 在 chunkSize 范围内找最近的 \n\n（段落边界）
    let cut = chunkSize
    const searchRange = remaining.slice(chunkSize - 400, chunkSize + 400)
    const paraBreak = searchRange.lastIndexOf('\n\n')
    if (paraBreak !== -1) {
      cut = chunkSize - 400 + paraBreak
    } else {
      // 退而找 \n
      const lineBreak = searchRange.lastIndexOf('\n')
      if (lineBreak !== -1) cut = chunkSize - 400 + lineBreak
      else {
        // 找句号
        const dot = searchRange.lastIndexOf('。')
        if (dot !== -1) cut = chunkSize - 400 + dot + 1
      }
    }
    chunks.push(remaining.slice(0, cut))
    remaining = remaining.slice(cut)
  }
  return chunks
}

// ── 单个内容块，有/无懒加载 ──
function MarkdownChunk({ content, lazy }: { content: string; lazy: boolean }) {
  const ref = useRef<HTMLDivElement>(null)
  const [visible, setVisible] = useState(!lazy)

  useEffect(() => {
    if (!lazy || !ref.current) return
    const el = ref.current
    const obs = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting) {
          setVisible(true)
          obs.disconnect()
        }
      },
      { rootMargin: '300px 0px 300px 0px' }  // 提前 300px 开始渲染
    )
    obs.observe(el)
    return () => obs.disconnect()
  }, [lazy])

  if (!visible) {
    // 占位：估算高度 = 每行约 40 字符 × 行高 1.6 × 字号
    const estimatedLines = Math.ceil(content.length / 50)
    const estimatedHeight = estimatedLines * 24
    return <div ref={ref} style={{ minHeight: estimatedHeight, contain: 'strict' }} aria-hidden="true" />
  }

  return (
    <div ref={lazy ? ref : undefined}>
      <ReactMarkdown>{content}</ReactMarkdown>
    </div>
  )
}

export default function LazyMarkdown({ content, isStreaming }: Props) {
  const safeContent = content || ' '

  // 流式输出中：永远直接渲染（用户正在读），只做分段
  if (isStreaming) {
    const chunks = splitContent(safeContent, 4000)
    return (
      <>
        {chunks.map((chunk, i) => (
          <MarkdownChunk key={i} content={chunk} lazy={false} />
        ))}
      </>
    )
  }

  // 短内容：直接全量渲染
  if (safeContent.length <= CHUNK_THRESHOLD) {
    return <ReactMarkdown>{safeContent}</ReactMarkdown>
  }

  // 中等内容：单个 chunk，IntersectionObserver 懒加载
  if (safeContent.length <= BLOCK_THRESHOLD) {
    return <MarkdownChunk content={safeContent} lazy={true} />
  }

  // 长内容：分段 + 每段懒加载 + content-visibility 自动跳过离屏渲染
  const chunks = splitContent(safeContent, 4000)
  return (
    <>
      {chunks.map((chunk, i) => (
        <div key={i} style={{ contentVisibility: 'auto', containIntrinsicSize: 'auto 300px' }}>
          <MarkdownChunk content={chunk} lazy={i > 0} />
        </div>
      ))}
    </>
  )
}