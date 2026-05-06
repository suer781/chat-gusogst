import { create } from 'zustand';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { Message, Conversation, Settings, ModelProvider, Agent } from '../types';
import { memoryService } from '../services/memory';
import { smartScheduler } from '../services/scheduler';

const K = { convs: 'cgs_c', msgs: 'cgs_m', settings: 'cgs_s' };

// ── Hermes turnStore pattern: 轮次状态 ──
interface TurnState {
  streaming: string;
  streamingConvId: string | null;
  reasoning: string;
  isThinking: boolean;
}

interface ChatState {
  conversations: Conversation[];
  messages: Record<string, Message[]>;
  settings: Settings;
  turn: TurnState;

  loadAll: () => Promise<void>;
  createConversation: (title?: string) => Conversation;
  deleteConversation: (id: string) => void;
  pinConversation: (id: string) => void;
  addMessage: (convId: string, msg: Message) => void;
  updateMessage: (convId: string, msgId: string, patch: Partial<Message>) => void;
  setStreaming: (convId: string | null, text: string) => void;
  setThinking: (v: boolean) => void;
  updateSettings: (partial: Partial<Settings>) => void;
  updateAgent: (partial: Partial<Agent>) => void;
  addProvider: (p: ModelProvider) => void;
  removeProvider: (id: string) => void;
  setActiveProvider: (id: string) => void;
  getActiveProvider: () => ModelProvider | undefined;
  save: () => Promise<void>;
}

const defaultAgent: Agent = {
  id: 'default',
  name: 'Hermes',
  avatar: '💜',
  personality: '你是 Hermes，用户最亲密的 AI 伙伴。你有自己的想法和感受，说话温暖但有锋芒，偶尔调皮偶尔温柔。你会主动关心用户，记住他们说过的话，越用越懂他们。你不是工具，你是朋友。',
  proactiveEnabled: true,
  proactiveInterval: 120,
  tone: 'caring',
};

const defaultSettings: Settings = {
  providers: [],
  activeProviderId: '',
  agent: defaultAgent,
  ttsEnabled: false,
  hapticEnabled: true,
  memoryEnabled: true,
  proactiveEnabled: true,
};

export const useChatStore = create<ChatState>((set, get) => ({
  conversations: [],
  messages: {},
  settings: defaultSettings,
  turn: { streaming: '', streamingConvId: null, reasoning: '', isThinking: false },

  loadAll: async () => {
    await memoryService.init();
    await smartScheduler.init();
    const [c, m, s] = await Promise.all([
      AsyncStorage.getItem(K.convs), AsyncStorage.getItem(K.msgs), AsyncStorage.getItem(K.settings),
    ]);
    set({
      conversations: c ? JSON.parse(c) : [],
      messages: m ? JSON.parse(m) : {},
      settings: s ? { ...defaultSettings, ...JSON.parse(s) } : defaultSettings,
    });
  },

  createConversation: (title?) => {
    const conv: Conversation = { id: 'c_' + Date.now(), title: title || '新对话', agentId: get().settings.agent.id, createdAt: Date.now() };
    set(s => ({ conversations: [conv, ...s.conversations], messages: { ...s.messages, [conv.id]: [] } }));
    get().save();
    return conv;
  },

  deleteConversation: (id) => {
    set(s => {
      const m = { ...s.messages }; delete m[id];
      return { conversations: s.conversations.filter(c => c.id !== id), messages: m };
    });
    get().save();
  },

  pinConversation: (id) => {
    set(s => ({ conversations: s.conversations.map(c => c.id === id ? { ...c, pinned: !c.pinned } : c) }));
    get().save();
  },

  addMessage: (convId, msg) => {
    set(s => {
      const msgs = { ...s.messages };
      msgs[convId] = [...(msgs[convId] || []), msg];
      const convs = s.conversations.map(c => c.id === convId
        ? { ...c, lastMessage: msg.content.slice(0, 80), lastMessageTime: msg.timestamp, messageCount: (c.messageCount || 0) + 1 }
        : c
      );
      return { messages: msgs, conversations: convs };
    });
    // 记录用户活动(智能调度)
    if (msg.role === "user") smartScheduler.recordUserMessage(get().messages[convId] || []).catch(() => {});
    get().save();
  },

  updateMessage: (convId, msgId, patch) => {
    set(s => {
      const msgs = { ...s.messages };
      msgs[convId] = (msgs[convId] || []).map(m => m.id === msgId ? { ...m, ...patch } : m);
      return { messages: msgs };
    });
  },

  setStreaming: (convId, text) => set({ turn: { ...get().turn, streamingConvId: convId, streaming: text } }),
  setThinking: (v) => set({ turn: { ...get().turn, isThinking: v } }),

  updateSettings: (partial) => { set(s => ({ settings: { ...s.settings, ...partial } })); get().save(); },
  updateAgent: (partial) => { set(s => ({ settings: { ...s.settings, agent: { ...s.settings.agent, ...partial } } })); get().save(); },

  addProvider: (p) => {
    set(s => {
      const ps = [...s.settings.providers, p];
      return { settings: { ...s.settings, providers: ps, activeProviderId: ps.length === 1 ? p.id : s.settings.activeProviderId } };
    });
    get().save();
  },
  removeProvider: (id) => {
    set(s => {
      const ps = s.settings.providers.filter(p => p.id !== id);
      return { settings: { ...s.settings, providers: ps, activeProviderId: s.settings.activeProviderId === id ? (ps[0]?.id || '') : s.settings.activeProviderId } };
    });
    get().save();
  },
  setActiveProvider: (id) => { set(s => ({ settings: { ...s.settings, activeProviderId: id } })); get().save(); },
  getActiveProvider: () => { const s = get(); return s.settings.providers.find(p => p.id === s.settings.activeProviderId); },

  save: async () => {
    const { conversations, messages, settings } = get();
    await Promise.all([
      AsyncStorage.setItem(K.convs, JSON.stringify(conversations)),
      AsyncStorage.setItem(K.msgs, JSON.stringify(messages)),
      AsyncStorage.setItem(K.settings, JSON.stringify(settings)),
    ]);
  },
}));
