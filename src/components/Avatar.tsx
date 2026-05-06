import React from 'react';
import { View, Text, StyleSheet } from 'react-native';
import { colors } from '../theme/colors';

interface Props { emoji: string; size?: number; online?: boolean; }

export default function Avatar({ emoji, size = 48, online }: Props) {
  return (
    <View style={[styles.wrap, { width: size, height: size, borderRadius: size / 2 }]}>
      <Text style={{ fontSize: size * 0.48 }}>{emoji}</Text>
      {online && <View style={styles.dot} />}
    </View>
  );
}

const styles = StyleSheet.create({
  wrap: { backgroundColor: colors.primaryGhost, alignItems: 'center', justifyContent: 'center' },
  dot: { position: 'absolute', bottom: 1, right: 1, width: 10, height: 10, borderRadius: 5, backgroundColor: colors.online, borderWidth: 2, borderColor: colors.bgCard },
});
