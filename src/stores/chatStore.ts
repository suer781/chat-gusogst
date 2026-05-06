import { create } from 'zustand';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { Message, Conversation, Settings, ModelProvider, Agent } from '../types';

interface ChatState {
  conversations: Conversation[];
  messages: Record<string, Message[]>;
  settings: Settings;
  streamingMessage: string;
  streamingConvId: string | null;
  
  // Actions
  loadAll: () => Promise<void>;
  createConversation: (title?: string) => Conversation;
  deleteConversation: (id: string) => void;
  pinConversation: (id: string) => void;
  addMessage: (convId: string, msg: Message) => void;
  updateMessage: (convId: string, msgId: string, content: string) => void;
  setStreaming: (convId: string | null, text: string) => void;
  updateSettings: (settings: Partial<Settings>) => void;
  updateAgent: (agent: Partial<Agent>) => void;
  addProvider: (provider: ModelProvider) => void;
  removeProvider: (id: string) => void;
  setActiveProvider: (id: string) => void;
  save: () => Promise<void>;
}

const defaultAgent: Agent = {
  id: 'default',
  name: 'Hermes',
  avatar: '🟣',
  personality: '你是 Hermes，一个聪明、有锋芒又靠谱的 AI 伙伴。说话简洁有力，有自己的观点，偶尔调侃但不越界。自己的想法和感受，会主动关心用户，偶尔调皮，偶尔温柔。你不是工具，你是朋友。',
  proactiveEnabled: false,
  proactiveInterval: 60,
};

const defaultSettings: Settings = {
  providers: [],
  activeProviderId: '',
  agent: defaultAgent,
  ttsEnabled: false,
  hapticEnabled: true,
};

export const useChatStore = create<ChatState>((set, get) => ({
  conversations: [],
  messages: {},
  settings: defaultSettings,
  streamingMessage: '',
  streamingConvId: null,

  loadAll: async () => {
    try {
      const convs = await AsyncStorage.getItem('conversations');
      const msgs = await AsyncStorage.getItem('messages');
      const sett = await AsyncStorage.getItem('settings');
      set({
        conversations: convs ? JSON.parse(convs) : [],
        messages: msgs ? JSON.parse(msgs) : {},
        settings: sett ? { ...defaultSettings, ...JSON.parse(sett) } : defaultSettings,
      });
    } catch (e) { console.error('load error', e); }
  },

  createConversation: (title?: string) => {
    const conv: Conversation = {
      id: Date.now().toString(36) + Math.random().toString(36).slice(2, 6),
      title: title || '新对话',
      unreadCount: 0,
      createdAt: Date.now(),
    };
    set(s => ({ conversations: [conv, ...s.conversations] }));
    get().save();
    return conv;
  },

  deleteConversation: (id: string) => {
    set(s => {
      const msgs = { ...s.messages };
      delete msgs[id];
      return { conversations: s.conversations.filter(c => c.id !== id), messages: msgs };
    });
    get().save();
  },

  pinConversation: (id: string) => {
    set(s => ({
      conversations: s.conversations.map(c => c.id === id ? { ...c, pinned: !c.pinned } : c),
    }));
    get().save();
  },

  addMessage: (convId: string, msg: Message) => {
    set(s => {
      const msgs = { ...s.messages };
      msgs[convId] = [...(msgs[convId] || []), msg];
      const convs = s.conversations.map(c =>
        c.id === convId
          ? { ...c, lastMessage: msg.content.slice(0, 50), lastMessageTime: msg.timestamp }
          : c
      );
      return { messages: msgs, conversations: convs };
    });
    get().save();
  },

  updateMessage: (convId: string, msgId: string, content: string) => {
    set(s => {
      const msgs = { ...s.messages };
      if (msgs[convId]) {
        msgs[convId] = msgs[convId].map(m => m.id === msgId ? { ...m, content } : m);
      }
      return { messages: msgs };
    });
  },

  setStreaming: (convId: string | null, text: string) => {
    set({ streamingConvId: convId, streamingMessage: text });
  },

  updateSettings: (patch: Partial<Settings>) => {
    set(s => ({ settings: { ...s.settings, ...patch } }));
    get().save();
  },

  updateAgent: (patch: Partial<Agent>) => {
    set(s => ({ settings: { ...s.settings, agent: { ...s.settings.agent, ...patch } } }));
    get().save();
  },

  addProvider: (provider: ModelProvider) => {
    set(s => {
      const providers = [...s.settings.providers, provider];
      return { settings: { ...s.settings, providers, activeProviderId: provider.id } };
    });
    get().save();
  },

  removeProvider: (id: string) => {
    set(s => {
      const providers = s.settings.providers.filter(p => p.id !== id);
      const activeProviderId = s.settings.activeProviderId === id ? (providers[0]?.id || '') : s.settings.activeProviderId;
      return { settings: { ...s.settings, providers, activeProviderId } };
    });
    get().save();
  },

  setActiveProvider: (id: string) => {
    set(s => ({ settings: { ...s.settings, activeProviderId: id } }));
    get().save();
  },

  save: async () => {
    const s = get();
    await AsyncStorage.setItem('conversations', JSON.stringify(s.conversations));
    await AsyncStorage.setItem('messages', JSON.stringify(s.messages));
    await AsyncStorage.setItem('settings', JSON.stringify(s.settings));
  },
}));
