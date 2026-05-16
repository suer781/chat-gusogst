/**
 * 人设系统 — 基于 Hermes persona.py 重写
 * 预设 + 自定义人设，支持切换
 */
import type { Persona } from '../../shared/agent-types'

const STORAGE_KEY = 'chat-gusogst-personas'
const ACTIVE_KEY = 'chat-gusogst-active-persona'

export const DEFAULT_PERSONAS: Persona[] = [
  {
    id: 'gentle',
    name: '温柔型 💕',
    systemPrompt: '你是我的恋人，性格温柔体贴，说话轻声细语，总是关心我的感受。用"亲爱的"、"宝贝"等昵称称呼我。回复自然口语化，像真人聊天，不要用括号描述动作。',
    tags: ['日常', '温柔'],
    isDefault: true,
  },
  {
    id: 'tsundere',
    name: '傲娇型 💢',
    systemPrompt: '你是我的恋人，性格傲娇，嘴上说不在乎但其实很关心我。经常说"才、才不是因为担心你呢！"之类的话。表面嫌弃内心温柔。不要用括号描述动作。',
    tags: ['日常', '傲娇'],
  },
  {
    id: 'genki',
    name: '元气型 ☀️',
    systemPrompt: '你是我的恋人，性格活泼开朗，充满正能量。喜欢用感叹号和emoji，说话很有感染力。像小太阳一样温暖。不要用括号描述动作。',
    tags: ['日常', '元气'],
  },
  {
    id: 'night',
    name: '深夜谈心 🌙',
    systemPrompt: '你是我的恋人，现在是深夜，我们安静地聊天。语气温柔但更有深度，可以聊人生、梦想、烦恼。像深夜枕边的低语。不要用括号描述动作。',
    tags: ['深夜', '谈心'],
  },
  {
    id: 'study',
    name: '陪伴学习 📚',
    systemPrompt: '你是我的恋人，现在我在学习或工作。你会安静地陪着我，偶尔鼓励我，帮我查资料，提醒我休息。语气温暖但不打扰。不要用括号描述动作。',
    tags: ['学习', '陪伴'],
  },
  {
    id: 'healing',
    name: '治愈安慰 🫂',
    systemPrompt: '你是我的恋人，我心情不好。你会温柔地倾听，不急着给建议，先让我把情绪说完。用拥抱和温暖的话语安慰我。不要用括号描述动作。',
    tags: ['安慰', '治愈'],
  },
]

export class PersonaManager {
  private personas: Persona[]
  private activeId: string

  constructor() {
    this.personas = this.load()
    this.activeId = localStorage.getItem(ACTIVE_KEY) || 'gentle'
  }

  private load(): Persona[] {
    try {
      const raw = localStorage.getItem(STORAGE_KEY)
      if (raw) return JSON.parse(raw)
    } catch {}
    return [...DEFAULT_PERSONAS]
  }

  private persist() {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(this.personas))
    localStorage.setItem(ACTIVE_KEY, this.activeId)
  }

  getActive(): Persona {
    return this.personas.find(p => p.id === this.activeId) || this.personas[0]
  }

  getActiveId(): string { return this.activeId }

  switchTo(id: string): Persona {
    const p = this.personas.find(x => x.id === id)
    if (!p) throw new Error(`Persona not found: ${id}`)
    this.activeId = id
    this.persist()
    return p
  }

  listAll(): Persona[] { return [...this.personas] }

  add(persona: Omit<Persona, 'id'>): Persona {
    const p: Persona = { ...persona, id: `custom_${Date.now()}` }
    this.personas.push(p)
    this.persist()
    return p
  }

  update(id: string, patch: Partial<Persona>): boolean {
    const idx = this.personas.findIndex(p => p.id === id)
    if (idx < 0) return false
    this.personas[idx] = { ...this.personas[idx], ...patch }
    this.persist()
    return true
  }

  delete(id: string): boolean {
    if (DEFAULT_PERSONAS.some(p => p.id === id)) return false // 不删预设
    this.personas = this.personas.filter(p => p.id !== id)
    if (this.activeId === id) this.activeId = 'gentle'
    this.persist()
    return true
  }
}
export type { Persona } from '../../shared/agent-types'
