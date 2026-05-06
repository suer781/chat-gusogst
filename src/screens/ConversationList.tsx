import React, { useEffect, useState } from 'react';
import { View, Text, FlatList, Pressable, StyleSheet, StatusBar } from 'react-native';
import { useRouter } from 'expo-router';
import { colors } from '../theme/colors';
import Avatar from '../components/Avatar';
import { useChatStore } from '../stores/chatStore';
import { dailyFortune } from '../services/charms';
import { Conversation } from '../types';

function timeAgo(ts: number): string {
  const d = Date.now() - ts;
  if (d < 60000) return '刚刚';
  if (d < 3600000) return Math.floor(d / 60000) + '分钟前';
  if (d < 86400000) return Math.floor(d / 3600000) + '小时前';
  return Math.floor(d / 86400000) + '天前';
}

export default function ConversationList() {
  const router = useRouter();
  const { conversations, settings, loadAll, createConversation, deleteConversation, pinConversation } = useChatStore();
  const [fortune] = useState(() => dailyFortune());

  useEffect(() => { loadAll(); }, []);

  const sorted = [...conversations].sort((a, b) => {
    if (a.pinned && !b.pinned) return -1;
    if (!a.pinned && b.pinned) return 1;
    return (b.lastMessageTime || b.createdAt) - (a.lastMessageTime || a.createdAt);
  });

  const handleNew = () => {
    const c = createConversation();
    router.push('/chat/' + c.id);
  };

  const renderItem = ({ item }: { item: Conversation }) => (
    <Pressable style={styles.item} onPress={() => router.push('/chat/' + item.id)} onLongPress={() => pinConversation(item.id)}>
      <Avatar emoji={settings.agent.avatar} size={52} online />
      <View style={styles.itemBody}>
        <View style={styles.itemRow}>
          <Text style={styles.itemTitle} numberOfLines={1}>{item.title}</Text>
          <Text style={styles.itemTime}>{item.lastMessageTime ? timeAgo(item.lastMessageTime) : ''}</Text>
        </View>
        <Text style={styles.itemPreview} numberOfLines={1}>{item.lastMessage || '开始对话...'}</Text>
      </View>
      {item.pinned && <Text style={styles.pin}>📌</Text>}
    </Pressable>
  );

  return (
    <View style={styles.container}>
      <StatusBar barStyle="dark-content" backgroundColor={colors.bg} />
      <View style={styles.header}>
        <View>
          <Text style={styles.headerTitle}>{settings.agent.name}</Text>
          <Text style={styles.fortune}>{fortune}</Text>
        </View>
        <Pressable style={styles.settingsBtn} onPress={() => router.push('/settings')}>
          <Text style={{ fontSize: 22 }}>⚙️</Text>
        </Pressable>
      </View>
      {sorted.length === 0 ? (
        <View style={styles.empty}>
          <Text style={styles.emptyEmoji}>💜</Text>
          <Text style={styles.emptyText}>你好，我是 {settings.agent.name}</Text>
          <Text style={styles.emptyHint}>点击下方按钮开始聊天吧</Text>
        </View>
      ) : (
        <FlatList data={sorted} keyExtractor={i => i.id} renderItem={renderItem} contentContainerStyle={{ paddingHorizontal: 16 }} />
      )}
      <Pressable style={styles.fab} onPress={handleNew}><Text style={styles.fabText}>＋</Text></Pressable>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: colors.bg },
  header: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'flex-end', paddingHorizontal: 20, paddingTop: 60, paddingBottom: 16 },
  headerTitle: { fontSize: 28, fontWeight: '700', color: colors.text },
  fortune: { fontSize: 13, color: colors.textSecondary, marginTop: 4 },
  settingsBtn: { padding: 8 },
  item: { flexDirection: 'row', alignItems: 'center', paddingVertical: 14, borderBottomWidth: 1, borderBottomColor: colors.borderLight },
  itemBody: { flex: 1, marginLeft: 12 },
  itemRow: { flexDirection: 'row', justifyContent: 'space-between' },
  itemTitle: { fontSize: 16, fontWeight: '600', color: colors.text, flex: 1 },
  itemTime: { fontSize: 12, color: colors.textMuted, marginLeft: 8 },
  itemPreview: { fontSize: 14, color: colors.textSecondary, marginTop: 4 },
  pin: { fontSize: 14, marginLeft: 8 },
  empty: { flex: 1, justifyContent: 'center', alignItems: 'center' },
  emptyEmoji: { fontSize: 56, marginBottom: 16 },
  emptyText: { fontSize: 20, color: colors.text, fontWeight: '600' },
  emptyHint: { fontSize: 14, color: colors.textMuted, marginTop: 6 },
  fab: { position: 'absolute', bottom: 28, right: 24, width: 56, height: 56, borderRadius: 28, backgroundColor: colors.primary, alignItems: 'center', justifyContent: 'center', elevation: 4, shadowColor: colors.primary, shadowOffset: { width: 0, height: 4 }, shadowOpacity: 0.3, shadowRadius: 8 },
  fabText: { fontSize: 28, color: '#FFF', marginTop: -2 },
});
