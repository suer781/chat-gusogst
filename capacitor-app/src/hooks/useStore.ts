import { useState, useEffect, useCallback } from 'react'
import type { ModelConfig, Persona, Message } from '../lib/types'
import { DEFAULT_MODEL, PRESETS } from '../lib/types'

const KEY_MODEL = 'gusogst_model'
const KEY_PERSONA = 'gusogst_persona'
const KEY_MESSAGES = 'gusogst_messages'
const KEY_CUSTOM_PERSONAS = 'gusogst_custom_personas'

function load<T>(key: string, fallback: T): T {
  try { const v = localStorage.getItem(key); return v ? JSON.parse(v) : fallback } catch { return fallback }
}
function save(key: string, value: unknown) { localStorage.setItem(key, JSON.stringify(value)) }

export function useModel() {
  const [model, setModel] = useState<ModelConfig>(() => load(KEY_MODEL, DEFAULT_MODEL))
  const saveModel = useCallback((m: ModelConfig) => { setModel(m); save(KEY_MODEL, m) }, [])
  return { model, saveModel }
}

export function usePersona() {
  const [persona, setPersona] = useState<Persona>(() => load(KEY_PERSONA, PRESETS[0]))
  const savePersona = useCallback((p: Persona) => { setPersona(p); save(KEY_PERSONA, p) }, [])
  return { persona, savePersona }
}

// 多人设管理：预设 + 自定义
export function usePersonas() {
  const [customPersonas, setCustomPersonas] = useState<Persona[]>(() => load(KEY_CUSTOM_PERSONAS, []))
  
  const allPersonas = [...PRESETS, ...customPersonas]
  
  const addPersona = useCallback((p: Persona) => {
    setCustomPersonas(prev => {
      const next = [...prev, p]
      save(KEY_CUSTOM_PERSONAS, next)
      return next
    })
  }, [])
  
  const updatePersona = useCallback((id: string, updates: Partial<Persona>) => {
    setCustomPersonas(prev => {
      const next = prev.map(p => p.id === id ? { ...p, ...updates } : p)
      save(KEY_CUSTOM_PERSONAS, next)
      return next
    })
  }, [])
  
  const deletePersona = useCallback((id: string) => {
    setCustomPersonas(prev => {
      const next = prev.filter(p => p.id !== id)
      save(KEY_CUSTOM_PERSONAS, next)
      return next
    })
  }, [])
  
  return { customPersonas, allPersonas, addPersona, updatePersona, deletePersona }
}

export function useMessages() {
  const [messages, setMessages] = useState<Message[]>(() => load(KEY_MESSAGES, []))
  useEffect(() => { save(KEY_MESSAGES, messages) }, [messages])
  const clearMessages = useCallback(() => { setMessages([]); localStorage.removeItem(KEY_MESSAGES) }, [])
  return { messages, setMessages, clearMessages }
}
