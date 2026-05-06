export interface Message {
  id: string;
  role: 'user' | 'assistant' | 'system';
  content: string;
  timestamp: number;
  status?: 'sending' | 'sent' | 'error';
}

export interface Conversation {
  id: string;
  title: string;
  lastMessage?: string;
  lastMessageTime?: number;
  unreadCount: number;
  createdAt: number;
  pinned?: boolean;
}

export interface Agent {
  id: string;
  name: string;
  avatar: string;
  personality: string;
  proactiveEnabled: boolean;
  proactiveInterval: number;
  proactiveLastTime?: number;
}

export interface ModelProvider {
  id: string;
  name: string;
  apiUrl: string;
  apiKey: string;
  model: string;
}

export interface Settings {
  providers: ModelProvider[];
  activeProviderId: string;
  agent: Agent;
  ttsEnabled: boolean;
  hapticEnabled: boolean;
}
