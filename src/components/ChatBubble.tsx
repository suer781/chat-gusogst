import React from 'react';
import { View, Text, StyleSheet } from 'react-native';
import { colors } from '../theme/colors';
import { Message } from '../types';

interface Props { message: Message; streaming?: boolean; }

export default function ChatBubble({ message, streaming }: Props) {
  const isUser = message.role === 'user';
  return (
    <View style={[styles.row, isUser ? styles.rowUser : styles.rowAsst]}>
      <View style={[styles.bubble, isUser ? styles.bubbleUser : styles.bubbleAsst]}>
        <Text style={[styles.text, isUser ? styles.textUser : styles.textAsst]}>
          {message.content}{streaming && <Text style={styles.cursor}> ▍</Text>}
        </Text>
        {message.status === 'error' && <Text style={styles.err}>发送失败</Text>}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  row: { marginVertical: 4, paddingHorizontal: 12 },
  rowUser: { alignItems: 'flex-end' },
  rowAsst: { alignItems: 'flex-start' },
  bubble: { maxWidth: '82%', paddingHorizontal: 14, paddingVertical: 10, borderRadius: 20 },
  bubbleUser: { backgroundColor: colors.bubbleUser, borderBottomRightRadius: 6 },
  bubbleAsst: { backgroundColor: colors.bubbleAssistant, borderBottomLeftRadius: 6, borderWidth: 1, borderColor: colors.bubbleAssistantBorder },
  text: { fontSize: 15, lineHeight: 22 },
  textUser: { color: colors.bubbleUserText },
  textAsst: { color: colors.bubbleAssistantText },
  cursor: { color: colors.primary, fontWeight: 'bold' },
  err: { color: colors.error, fontSize: 12, marginTop: 4 },
});
