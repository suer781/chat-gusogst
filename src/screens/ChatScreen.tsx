import React, { useRef, useEffect, useState, useCallback } from 'react';
import { View, Text, FlatList, Pressable, StyleSheet, StatusBar, KeyboardAvoidingView, Platform } from 'react-native';
import { useLocalSearchParams, useRouter, Stack } from 'expo-router';
import { colors } from '../theme/colors';
import Avatar from '../components/Avatar';
import ChatBubble from '../components/ChatBubble';
import MessageInput from '../components/MessageInput';
import { useChatStore } from '../stores/chatStore';
import { chatStream } from '../services/llm';
import { memoryService } from '../services/memory';
import { Message } from '../types';

export default function ChatScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const router = useRouter();
  const listRef = useRef<FlatList>(null);
  const abortRef = useRef<AbortController | null>(null);
  const [isStreaming, setIsStreaming] = useState(false);
  const [streamText, setStreamText] = useState('');

  const { messages, settings, addMessage, getActiveProvider } = useChatStore();
  const convMsgs = messages[id!] || [];

  const handleSend = useCallback(async (text: string) => {
    const provider = getActiveProvider();
    if (!provider) {
      addMessage(id!, { id: 'e_' + Date.now(), role: 'assistant', content: '请先在设置中配置模型 ⚙️', timestamp: Date.now(), status: 'error' });
      return;
    }

    addMessage(id!, { id: 'u_' + Date.now(), role: 'user', content: text, timestamp: Date.now() });

    // Hermes-style: 记忆注入
    let sys = settings.agent.personality;
    if (settings.memoryEnabled) {
      const mems = memoryService.searchMemories(text, 3);
      if (mems.length) sys += '

[记忆] ' + mems.map(m => m.summary).join('; ');
      const sum = memoryService.getSummary(id!);
      if (sum) sys += '

[会话] ' + sum;
    }

    const apiMsgs = [
      { role: 'system', content: sys },
      ...convMsgs.map(m => ({ role: m.role, content: m.content })),
      { role: 'user', content: text },
    ];

    setIsStreaming(true);
    setStreamText('');
    abortRef.current = new AbortController();
    const aid = 'a_' + Date.now();

    await chatStream(provider, apiMsgs, {
      onToken: t => setStreamText(p => p + t),
      onComplete: (full, tokens) => {
        setIsStreaming(false); setStreamText('');
        addMessage(id!, { id: aid, role: 'assistant', content: full, timestamp: Date.now(), status: 'sent', tokens });
      },
      onError: err => {
        setIsStreaming(false); setStreamText('');
        addMessage(id!, { id: aid, role: 'assistant', content: '出错了: ' + err, timestamp: Date.now(), status: 'error' });
      },
    }, abortRef.current.signal);
  }, [id, convMsgs, settings, getActiveProvider, addMessage]);

  const handleStop = () => {
    abortRef.current?.abort();
    setIsStreaming(false);
    if (streamText) {
      addMessage(id!, { id: 'a_' + Date.now(), role: 'assistant', content: streamText, timestamp: Date.now(), status: 'sent' });
      setStreamText('');
    }
  };

  useEffect(() => () => { abortRef.current?.abort(); }, []);

  const all = isStreaming && streamText
    ? [...convMsgs, { id: '_stream', role: 'assistant' as const, content: streamText, timestamp: Date.now() }]
    : convMsgs;

  return (
    <KeyboardAvoidingView style={{ flex: 1, backgroundColor: colors.bgChat }} behavior={Platform.OS === 'ios' ? 'padding' : undefined} keyboardVerticalOffset={90}>
      <StatusBar barStyle="dark-content" />
      <Stack.Screen options={{ headerShown: false }} />
      <View style={styles.topBar}>
        <Pressable onPress={() => router.back()} style={{ padding: 8 }}><Text style={{ fontSize: 28, color: colors.text }}>‹</Text></Pressable>
        <Avatar emoji={settings.agent.avatar} size={32} online />
        <Text style={styles.topTitle}>{settings.agent.name}</Text>
        <View style={{ flex: 1 }} />
        {isStreaming && (
          <Pressable onPress={handleStop} style={styles.stopBtn}><Text style={{ color: '#FFF', fontSize: 13 }}>停止</Text></Pressable>
        )}
      </View>
      <FlatList
        ref={listRef}
        data={all}
        keyExtractor={i => i.id}
        renderItem={({ item }) => <ChatBubble message={item} streaming={item.id === '_stream'} />}
        contentContainerStyle={{ paddingTop: 8, paddingBottom: 8 }}
        onContentSizeChange={() => listRef.current?.scrollToEnd()}
      />
      <MessageInput onSend={handleSend} disabled={isStreaming} />
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  topBar: { flexDirection: 'row', alignItems: 'center', paddingHorizontal: 12, paddingTop: 50, paddingBottom: 10, backgroundColor: colors.bgCard, borderBottomWidth: 1, borderBottomColor: colors.borderLight },
  topTitle: { fontSize: 17, fontWeight: '600', color: colors.text, marginLeft: 8 },
  stopBtn: { backgroundColor: colors.error, borderRadius: 12, paddingHorizontal: 12, paddingVertical: 4 },
});
