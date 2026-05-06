import React, { useState } from 'react';
import { View, TextInput, Pressable, Text, StyleSheet, Keyboard } from 'react-native';
import { colors } from '../theme/colors';

interface Props { onSend: (text: string) => void; disabled?: boolean; }

export default function MessageInput({ onSend, disabled }: Props) {
  const [text, setText] = useState('');
  const send = () => {
    const t = text.trim();
    if (!t || disabled) return;
    onSend(t); setText(''); Keyboard.dismiss();
  };
  return (
    <View style={styles.wrap}>
      <TextInput
        style={styles.input} value={text} onChangeText={setText}
        placeholder="说点什么..." placeholderTextColor={colors.textMuted}
        multiline maxLength={4000} editable={!disabled}
      />
      <Pressable style={[styles.btn, (!text.trim() || disabled) && { opacity: 0.4 }]} onPress={send}>
        <Text style={styles.btnText}>↑</Text>
      </Pressable>
    </View>
  );
}

const styles = StyleSheet.create({
  wrap: { flexDirection: 'row', alignItems: 'flex-end', paddingHorizontal: 12, paddingVertical: 8, backgroundColor: colors.bgCard, borderTopWidth: 1, borderTopColor: colors.borderLight },
  input: { flex: 1, backgroundColor: colors.bgChat, borderRadius: 20, paddingHorizontal: 16, paddingVertical: 10, fontSize: 15, color: colors.text, maxHeight: 100, marginRight: 8 },
  btn: { width: 38, height: 38, borderRadius: 19, backgroundColor: colors.primary, alignItems: 'center', justifyContent: 'center' },
  btnText: { color: '#FFF', fontSize: 18, fontWeight: '600' },
});
