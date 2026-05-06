import React from 'react';
import { View, Text, Image, StyleSheet } from 'react-native';
import { colors } from '../theme/colors';

interface Props {
  emoji?: string;
  uri?: string;
  size?: number;
  online?: boolean;
}

export default function Avatar({ emoji, uri, size = 40, online }: Props) {
  return (
    <View style={[styles.container, { width: size, height: size, borderRadius: size / 2 }]}>
      {uri ? (
        <Image source={{ uri }} style={{ width: size, height: size, borderRadius: size / 2 }} />
      ) : (
        <View style={[styles.emojiWrap, { width: size, height: size, borderRadius: size / 2 }]}>
          <Text style={{ fontSize: size * 0.5 }}>{emoji || '🧡'}</Text>
        </View>
      )}
      {online && <View style={[styles.online, { right: size > 36 ? 2 : 0, bottom: size > 36 ? 2 : 0 }]} />}
    </View>
  );
}

const styles = StyleSheet.create({
  container: { position: 'relative' },
  emojiWrap: {
    backgroundColor: colors.accentLight,
    alignItems: 'center',
    justifyContent: 'center',
  },
  online: {
    position: 'absolute',
    width: 10, height: 10,
    borderRadius: 5,
    backgroundColor: colors.online,
    borderWidth: 2,
    borderColor: colors.bg,
  },
});
