import React, { useEffect } from 'react';
import { View, Text, FlatList, Pressable, StyleSheet, StatusBar } from 'react-native';
import { useRouter } from 'expo-router';
import { colors } from '../theme/colors';
import Avatar from '../components/Avatar';
import { useChatStore } from '../stores/chatStore';
import { Conversation } from '../types';

export default function ConversationList() {
  const router = useRouter();
  const { conversations, settings, loadAll, createConversation, deleteConversation, pinConversation } = useChatStore();

  useEffect(() => { loadAll(); }, []);

  const sorted = [...conversations].sort((a, b) => {
    if (a.pinned && !b.pinned) return -1;
    if (!a.pinned && b.pinned) return 1;
    return (b.lastMessageTime || b.createdAt) - (a.lastMessageTime || a.createdAt);
  });

  const handleNew = () => {
    const conv = createConversation();
    router.push(`/chat/${conv.id}`);
  };

  const renderItem = ({ item }: { item: Conversation }) => (
    <Pressable
      style={styles.item}
      onPress={() => router.push(`/chat/${item.id}`)}
      onLongPress={() => pinConversation(item.id)}
    >
      <Avatar emoji={settings.agent.avatar} size={48} online />
      <View style={styles.itemBody}>
        <View style={styles.itemHeader}>
          <Text style={styles.itemTitle} numberOfLines={1}>{item.title}</Text>
          <Text style={styles.itemTime}>
            {item.lastMessageTime ? timeAgo(item.lastMessageTime) : ''}
          </Text>
        </View>
        <Text style={styles.itemPreview} numberOfLines={1}>
          {item.lastMessage || '开始对话...'}
        </Text>
      </View>
      {item.pinned && <Text style={styles.pin}>📌</Text>}
    </Pressable>
  );

  return (
    <View style={styles.container}>
      <StatusBar barStyle="dark-content" backgroundColor={colors.bg} />
      <View style={styles.header}>
        <Text style={styles.headerTitle}>{settings.agent.name}</Text>
        <Pressable style={styles.settingsBtn} onPress={() => router.push('/settings')}>
          <Text style={{ fontSize: 20 }}>⚙️</Text>
        </Pressable>
      </View>

      {sorted.length === 0 ? (
        <View style={styles.empty}>
          <Text style={styles.emptyEmoji}>{settings.agent.avatar}</Text>
          <Text style={styles.emptyText}>嗨，我是{settings.agent.name}</Text>
          <Text style={styles.emptySub}>点下面开始聊天吧</Text>
        </View>
      ) : (
        <FlatList
          data={sorted}
          keyExtractor={i => i.id}
          renderItem={renderItem}
          contentContainerStyle={{ paddingBottom: 100 }}
        />
      )}

      <Pressable style={styles.fab} onPress={handleNew}>
        <Text style={styles.fabText}>+</Text>
      </Pressable>
    </View>
  );
}

function timeAgo(ts: number) {
  const d = Date.now() - ts;
  if (d < 60000) return '刚刚';
  if (d < 3600000) return Math.floor(d / 60000) + '分钟前';
  if (d < 86400000) return Math.floor(d / 3600000) + '小时前';
  return Math.floor(d / 86400000) + '天前';
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: colors.bg },
  header: {
    flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between',
    paddingHorizontal: 20, paddingTop: 56, paddingBottom: 16,
    backgroundColor: colors.bg,
  },
  headerTitle: { fontSize: 24, fontWeight: '700', color: colors.textPrimary },
  settingsBtn: { padding: 8 },
  empty: { flex: 1, alignItems: 'center', justifyContent: 'center' },
  emptyEmoji: { fontSize: 64, marginBottom: 16 },
  emptyText: { fontSize: 20, fontWeight: '600', color: colors.textPrimary },
  emptySub: { fontSize: 14, color: colors.textSecondary, marginTop: 8 },
  item: {
    flexDirection: 'row', alignItems: 'center',
    paddingHorizontal: 20, paddingVertical: 14,
    borderBottomWidth: StyleSheet.hairlineWidth, borderBottomColor: colors.divider,
  },
  itemBody: { flex: 1, marginLeft: 12 },
  itemHeader: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  itemTitle: { fontSize: 16, fontWeight: '600', color: colors.textPrimary, flex: 1 },
  itemTime: { fontSize: 12, color: colors.textTertiary, marginLeft: 8 },
  itemPreview: { fontSize: 13, color: colors.textSecondary, marginTop: 4 },
  pin: { fontSize: 14, marginLeft: 8 },
  fab: {
    position: 'absolute', right: 20, bottom: 32,
    width: 56, height: 56, borderRadius: 28,
    backgroundColor: colors.accent, alignItems: 'center', justifyContent: 'center',
    elevation: 4, shadowColor: colors.accent, shadowOffset: { width: 0, height: 4 }, shadowOpacity: 0.3, shadowRadius: 8,
  },
  fabText: { fontSize: 28, color: '#fff', marginTop: -2 },
});
