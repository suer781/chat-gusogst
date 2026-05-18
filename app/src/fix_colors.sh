#!/bin/bash
set -e
cd ~/project/github.com/chat-gusogst/app/src/ui
sed -i "s/background: '#0f0f23'/background: 'var(--bg-primary)'/g" chat/ChatView.tsx persona/PersonaView.tsx providers/ProviderView.tsx
sed -i "s/backgroundColor: '#0f0f23'/backgroundColor: 'var(--bg-primary)'/g" chat/ChatView.tsx persona/PersonaView.tsx providers/ProviderView.tsx
sed -i "s/,'#0f0f23'/,'var(--bg-primary)'/g" chat/ChatView.tsx
sed -i "s/background: '#1a1a2e'/background: 'var(--bg-secondary)'/g" chat/ChatView.tsx providers/ProviderView.tsx settings/SettingsView.tsx
sed -i "s/backgroundColor: '#1a1a2e'/backgroundColor: 'var(--bg-secondary)'/g" chat/ChatView.tsx providers/ProviderView.tsx settings/SettingsView.tsx
sed -i "s/backgroundColor: '#1a1a3a'/backgroundColor: 'var(--bg-secondary)'/g" chat/ChatView.tsx persona/PersonaView.tsx
sed -i "s/,'#1a1a3a'/,'var(--bg-secondary)'/g" chat/ChatView.tsx persona/PersonaView.tsx
sed -i "s/,'#1a1a2e'/,'var(--bg-secondary)'/g" chat/ChatView.tsx providers/ProviderView.tsx
sed -i "s/backgroundColor: '#16213e'/backgroundColor: 'var(--bg-card)'/g" chat/ChatView.tsx providers/ProviderView.tsx
sed -i "s/,'#16213e'/,'var(--bg-card)'/g" chat/ChatView.tsx
sed -i "s/backgroundColor: '#0a0a23'/backgroundColor: 'var(--bg-secondary)'/g" App.tsx
sed -i "s/,'#0a0a23'/,'var(--bg-secondary)'/g" App.tsx
sed -i "s/backgroundColor: '#222222'/backgroundColor: 'var(--bg-card)'/g" chat/ChatView.tsx
sed -i "s/,'#222222'/,'var(--bg-card)'/g" chat/ChatView.tsx
sed -i "s/color: '#e0e0e0'/color: 'var(--text-primary)'/g" chat/ChatView.tsx persona/PersonaView.tsx providers/ProviderView.tsx
sed -i "s/,'#e0e0e0'/,'var(--text-primary)'/g" App.tsx
sed -i "s/color: '#666'/color: 'var(--text-secondary)'/g" chat/ChatView.tsx persona/PersonaView.tsx
sed -i "s/,'#666'/,'var(--text-secondary)'/g" chat/ChatView.tsx persona/PersonaView.tsx App.tsx
sed -i "s/color: '#888'/color: 'var(--text-secondary)'/g" chat/ChatView.tsx providers/ProviderView.tsx
sed -i "s/,'#888'/,'var(--text-secondary)'/g" chat/ChatView.tsx providers/ProviderView.tsx
sed -i "s/color: '#999'/color: 'var(--text-secondary)'/g" chat/ChatView.tsx providers/ProviderView.tsx
sed -i "s/,'#999'/,'var(--text-secondary)'/g" chat/ChatView.tsx providers/ProviderView.tsx
sed -i "s/borderColor: 'rgba(255,255,255,0.1)'/borderColor: 'var(--border-color)'/g" chat/ChatView.tsx persona/PersonaView.tsx
echo DONE
