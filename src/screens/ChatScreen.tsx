import React, { useRef, useEffect, useState } from 'react';
import { View, Text, FlatList, Pressable, StyleSheet, StatusBar, KeyboardAvoidingView, Platform } from 'react-native';
import { useLocalSearchParams, useRouter } from 'expo-router';
import { colors } from '../theme/colors';
import Avatar from '../components/Avatar';
import ChatBubble from '../components/ChatBubble';
import MessageInput from '../components/MessageInput';
import { useChatStore } from '../stores/chatStore';
import { chatStream } from '../services/llm';
import { Message } from '../types';

export default function ChatScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const router = useRouter();
  const flatListRef = useRef<FlatList>(null);
  const [isStreaming, setIsStreaming] = useState(false);
  const [streamText, setStreamText] = useState('');

  const { messages, settings, addMessage, setStreaming } = useChatStore();
  const convMessages = messages[id!] || [];
  const provider = settings.providers.find(p => p.id === settings.activeProviderId);

  const handleSend = async (text: string) => {
    if (!provider) {
      addMessage(id!, {
        id: 'err-' + Date.now(),
        role: 'assistant',
        content: '请先在设置中配置模型提供方',
        timestamp: Date.now(),
      });
      return;
    }

    // 添加用户消息
    const userMsg: Message = {
      id: 'u-' + Date.now(),
      role: 'user',
      content: text,
      timestamp: Date.now(),
    };
    addMessage(id!, userMsg);

    // 构建上下文
    const apiMessages = [
      { role: 'system', content: settings.agent.personality },
      ...convMessages.map(m => ({ role: m.role, content: m.content })),
      { role: 'user', content: text },
    ];

    // 流式调用
    setIsStreaming(true);
    setStreamText('');

    const assistantId = 'a-' + Date.now();
    const assistantMsg: Message = {
      id: assistantId,
      role: 'assistant',
      content: '',
      timestamp: Date.now(),
      status: 'sending',
    };
    addMessage(id!, assistantMsg);

    let full = '';
    chatStream(provider, apiMessages, {
      onToken: (token) => {
        full += token;
        setStreamText(full);
      },
      onComplete: (fullText) => {
        useChatStore.getState().updateMessage(id!, assistantId, fullText);
        setIsStreaming(false);
        setStreamText('');
      },
      onError: (err) => {
        useChatStore.getState().updateMessage(id!, assistantId, '出错了: ' + err);
        setIsStreaming(false);
        setStreamText('');
      },
    });
  };

  // 显示的消息列表
  const displayMessages = isStreaming && convMessages.length > 0
    ? [
        ...convMessages.slice(0, -1),
        { ...convMessages[convMessages.length - 1], content: streamText || '...' },
      ]
    : convMessages;

  return (
    <KeyboardAvoidingView style={styles.container} behavior={Platform.OS === 'ios' ? 'padding' : undefined} keyboardVerticalOffset={0}>
      <StatusBar barStyle="dark-content" backgroundColor={colors.bg} />
      <View style={styles.header}>
        <Pressable onPress={() => router.back()} style={styles.backBtn}>
          <Text style={{ fontSize: 20, color: colors.accent }}>‹</Text>
        </Pressable>
        <Avatar emoji={settings.agent.avatar} size={32} />
        <View style={styles.headerInfo}>
          <Text style={styles.headerTitle}>{settings.agent.name}</Text>
          {isStreaming && <Text style={styles.typing}>正在输入...</Text>}
        </View>
      </View>

      <FlatList
        ref={flatListRef}
        data={displayMessages}
        keyExtractor={m => m.id}
        renderItem={({ item, index }) => (
          <ChatBubble
            message={item}
            agentEmoji={settings.agent.avatar}
            isLast={index === displayMessages.length - 1}
            streaming={isStreaming && index === displayMessages.length - 1}
          />
        )}
        contentContainerStyle={{ paddingVertical: 12 }}
        onContentSizeChange={() => flatListRef.current?.scrollToEnd({ animated: true })}
      />

      <MessageInput onSend={handleSend} disabled={isStreaming} />
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: colors.bg },
  header: {
    flexDirection: 'row', alignItems: 'center',
    paddingHorizontal: 12, paddingTop: 52, paddingBottom: 12,
    backgroundColor: colors.bg,
    borderBottomWidth: StyleSheet.hairlineWidth, borderBottomColor: colors.divider,
  },
  backBtn: { padding: 8, marginRight: 4 },
  headerInfo: { flex: 1, marginLeft: 10 },
  headerTitle: { fontSize: 17, fontWeight: '600', color: colors.textPrimary },
  typing: { fontSize: 12, color: colors.typing, marginTop: 2 },
});
