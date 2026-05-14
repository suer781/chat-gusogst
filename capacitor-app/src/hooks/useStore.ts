import { useState, useEffect, useCallback } from 'react'
import type { ModelConfig, Persona, Message } from '../lib/types'
import { DEFAULT_MODEL, PRESETS } from '../lib/types'

const KEY_MODEL = 'gusogst_model'
const KEY_PERSONA = 'gusogst_persona'
const KEY_MESSAGES = 'gusogst_messages'

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

export function useMessages() {
  const [messages, setMessages] = useState<Message[]>(() => load(KEY_MESSAGES, []))
  useEffect(() => { save(KEY_MESSAGES, messages) }, [messages])
  const clearMessages = useCallback(() => { setMessages([]); localStorage.removeItem(KEY_MESSAGES) }, [])
  return { messages, setMessages, clearMessages }
}