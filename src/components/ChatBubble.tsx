import React from 'react';
import { View, Text, StyleSheet, Pressable } from 'react-native';
import { colors } from '../theme/colors';
import Avatar from './Avatar';
import { Message } from '../types';

interface Props {
  message: Message;
  agentEmoji?: string;
  isLast?: boolean;
  streaming?: boolean;
}

export default function ChatBubble({ message, agentEmoji = '🧡', isLast, streaming }: Props) {
  const isUser = message.role === 'user';

  return (
    <View style={[styles.row, isUser && styles.rowUser]}>
      {!isUser && <Avatar emoji={agentEmoji} size={32} />}
      <View style={styles.bubbleWrap}>
        <View style={[styles.bubble, isUser ? styles.bubbleUser : styles.bubbleAI]}>
          <Text style={[styles.text, isUser ? styles.textUser : styles.textAI]}>
            {message.content}
            {streaming && <Text style={styles.cursor}>│</Text>}
          </Text>
        </View>
        <Text style={[styles.time, isUser && styles.timeUser]}>
          {new Date(message.timestamp).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })}
        </Text>
      </View>
      {isUser && <View style={{ width: 32 }} />}
    </View>
  );
}

const styles = StyleSheet.create({
  row: { flexDirection: 'row', alignItems: 'flex-start', marginVertical: 4, paddingHorizontal: 12 },
  rowUser: { flexDirection: 'row-reverse' },
  bubbleWrap: { flex: 1, marginHorizontal: 8 },
  bubble: { borderRadius: 16, paddingHorizontal: 14, paddingVertical: 10, maxWidth: '85%' },
  bubbleUser: { backgroundColor: colors.bubbleUser, alignSelf: 'flex-end', borderBottomRightRadius: 4 },
  bubbleAI: { backgroundColor: colors.bubbleAI, alignSelf: 'flex-start', borderBottomLeftRadius: 4, elevation: 1, shadowColor: '#000', shadowOffset: { width: 0, height: 1 }, shadowOpacity: 0.05, shadowRadius: 2 },
  text: { fontSize: 15, lineHeight: 22 },
  textUser: { color: colors.bubbleUserText },
  textAI: { color: colors.bubbleAIText },
  cursor: { color: colors.accent, fontWeight: 'bold' },
  time: { fontSize: 11, color: colors.textTertiary, marginTop: 4, marginLeft: 4 },
  timeUser: { textAlign: 'right', marginRight: 4 },
});
