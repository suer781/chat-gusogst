import React, { useState } from 'react';
import { View, Text, TextInput, Switch, Pressable, ScrollView, StyleSheet, Alert } from 'react-native';
import { useRouter } from 'expo-router';
import { colors } from '../theme/colors';
import Avatar from '../components/Avatar';
import { useChatStore } from '../stores/chatStore';
import { ModelProvider } from '../types';

export default function SettingsScreen() {
  const router = useRouter();
  const { settings, updateSettings, updateAgent, addProvider, removeProvider, setActiveProvider } = useChatStore();

  // 模型配置表单
  const [showAddProvider, setShowAddProvider] = useState(false);
  const [formName, setFormName] = useState('');
  const [formUrl, setFormUrl] = useState('');
  const [formKey, setFormKey] = useState('');
  const [formModel, setFormModel] = useState('');

  // Agent 编辑
  const [editingPersonality, setEditingPersonality] = useState(false);
  const [personalityText, setPersonalityText] = useState(settings.agent.personality);

  const handleAddProvider = () => {
    if (!formUrl || !formModel) { Alert.alert('提示', '请填写 API 地址和模型名'); return; }
    const p: ModelProvider = {
      id: Date.now().toString(36),
      name: formName || formModel,
      apiUrl: formUrl,
      apiKey: formKey,
      model: formModel,
    };
    addProvider(p);
    setShowAddProvider(false);
    setFormName(''); setFormUrl(''); setFormKey(''); setFormModel('');
  };

  const savePersonality = () => {
    updateAgent({ personality: personalityText });
    setEditingPersonality(false);
  };

  return (
    <ScrollView style={styles.container} contentContainerStyle={{ paddingBottom: 40 }}>
      <View style={styles.header}>
        <Pressable onPress={() => router.back()}>
          <Text style={{ fontSize: 20, color: colors.accent }}>‹ 返回</Text>
        </Pressable>
        <Text style={styles.headerTitle}>设置</Text>
        <View style={{ width: 60 }} />
      </View>

      {/* Agent 人设 */}
      <View style={styles.section}>
        <Text style={styles.sectionTitle}>AI 人设</Text>
        <View style={styles.card}>
          <View style={styles.row}>
            <Avatar emoji={settings.agent.avatar} size={48} />
            <View style={{ flex: 1, marginLeft: 12 }}>
              <Text style={styles.label}>名字</Text>
              <TextInput
                style={styles.input}
                value={settings.agent.name}
                onChangeText={n => updateAgent({ name: n })}
                placeholder="AI 名字"
              />
            </View>
          </View>
          <View style={styles.row}>
            <Text style={styles.label}>Emoji 头像</Text>
            <TextInput
              style={[styles.input, { width: 60, textAlign: 'center' }]}
              value={settings.agent.avatar}
              onChangeText={a => updateAgent({ avatar: a })}
            />
          </View>
          <Text style={styles.label}>性格设定</Text>
          {editingPersonality ? (
            <View>
              <TextInput
                style={[styles.input, styles.textArea]}
                value={personalityText}
                onChangeText={setPersonalityText}
                multiline
                numberOfLines={4}
              />
              <Pressable style={styles.saveBtn} onPress={savePersonality}>
                <Text style={styles.saveBtnText}>保存</Text>
              </Pressable>
            </View>
          ) : (
            <Pressable onPress={() => { setPersonalityText(settings.agent.personality); setEditingPersonality(true); }}>
              <Text style={styles.personalityPreview}>{settings.agent.personality}</Text>
            </Pressable>
          )}
        </View>
      </View>

      {/* 主动发消息 */}
      <View style={styles.section}>
        <Text style={styles.sectionTitle}>主动发消息</Text>
        <View style={styles.card}>
          <View style={styles.switchRow}>
            <Text style={styles.switchLabel}>开启主动消息</Text>
            <Switch
              value={settings.agent.proactiveEnabled}
              onValueChange={v => updateAgent({ proactiveEnabled: v })}
              trackColor={{ true: colors.accent, false: colors.border }}
              thumbColor={settings.agent.proactiveEnabled ? '#fff' : colors.textTertiary}
            />
          </View>
          {settings.agent.proactiveEnabled && (
            <View style={styles.row}>
              <Text style={styles.label}>间隔（分钟）</Text>
              <TextInput
                style={[styles.input, { width: 80 }]}
                value={String(settings.agent.proactiveInterval)}
                onChangeText={t => updateAgent({ proactiveInterval: parseInt(t) || 60 })}
                keyboardType="number-pad"
              />
            </View>
          )}
        </View>
      </View>

      {/* 模型提供方 */}
      <View style={styles.section}>
        <Text style={styles.sectionTitle}>模型提供方</Text>
        <View style={styles.card}>
          {settings.providers.map(p => (
            <View key={p.id} style={styles.providerItem}>
              <Pressable style={styles.providerBody} onPress={() => setActiveProvider(p.id)}>
                <View style={[styles.radio, p.id === settings.activeProviderId && styles.radioActive]} />
                <View style={{ flex: 1 }}>
                  <Text style={styles.providerName}>{p.name}</Text>
                  <Text style={styles.providerMeta}>{p.model} · {p.apiUrl.replace(/^https?:\/\//, '').split('/')[0]}</Text>
                </View>
              </Pressable>
              <Pressable onPress={() => removeProvider(p.id)}>
                <Text style={{ color: colors.error, fontSize: 13 }}>删除</Text>
              </Pressable>
            </View>
          ))}
          {settings.providers.length === 0 && (
            <Text style={{ color: colors.textTertiary, fontSize: 13, textAlign: 'center', padding: 12 }}>还没有配置模型</Text>
          )}
          {!showAddProvider ? (
            <Pressable style={styles.addBtn} onPress={() => setShowAddProvider(true)}>
              <Text style={styles.addBtnText}>+ 添加模型</Text>
            </Pressable>
          ) : (
            <View style={styles.addForm}>
              <TextInput style={styles.input} placeholder="名称（可选）" value={formName} onChangeText={setFormName} />
              <TextInput style={styles.input} placeholder="API 地址" value={formUrl} onChangeText={setFormUrl} autoCapitalize="none" />
              <TextInput style={styles.input} placeholder="API Key（可选）" value={formKey} onChangeText={setFormKey} secureTextEntry autoCapitalize="none" />
              <TextInput style={styles.input} placeholder="模型名" value={formModel} onChangeText={setFormModel} autoCapitalize="none" />
              <View style={{ flexDirection: 'row', gap: 8 }}>
                <Pressable style={[styles.saveBtn, { flex: 1 }]} onPress={handleAddProvider}>
                  <Text style={styles.saveBtnText}>添加</Text>
                </Pressable>
                <Pressable style={[styles.saveBtn, { flex: 1, backgroundColor: colors.textTertiary }]} onPress={() => setShowAddProvider(false)}>
                  <Text style={styles.saveBtnText}>取消</Text>
                </Pressable>
              </View>
            </View>
          )}
        </View>
      </View>

      {/* 其他设置 */}
      <View style={styles.section}>
        <Text style={styles.sectionTitle}>其他</Text>
        <View style={styles.card}>
          <View style={styles.switchRow}>
            <Text style={styles.switchLabel}>语音播报</Text>
            <Switch
              value={settings.ttsEnabled}
              onValueChange={v => updateSettings({ ttsEnabled: v })}
              trackColor={{ true: colors.accent, false: colors.border }}
              thumbColor={settings.ttsEnabled ? '#fff' : colors.textTertiary}
            />
          </View>
          <View style={styles.switchRow}>
            <Text style={styles.switchLabel}>触感反馈</Text>
            <Switch
              value={settings.hapticEnabled}
              onValueChange={v => updateSettings({ hapticEnabled: v })}
              trackColor={{ true: colors.accent, false: colors.border }}
              thumbColor={settings.hapticEnabled ? '#fff' : colors.textTertiary}
            />
          </View>
        </View>
      </View>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: colors.bg },
  header: {
    flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between',
    paddingHorizontal: 20, paddingTop: 56, paddingBottom: 16,
  },
  headerTitle: { fontSize: 18, fontWeight: '600', color: colors.textPrimary },
  section: { paddingHorizontal: 20, marginTop: 20 },
  sectionTitle: { fontSize: 13, fontWeight: '600', color: colors.textSecondary, textTransform: 'uppercase', letterSpacing: 0.5, marginBottom: 8, marginLeft: 4 },
  card: { backgroundColor: colors.bgCard, borderRadius: 16, padding: 16 },
  row: { flexDirection: 'row', alignItems: 'center', gap: 12, marginBottom: 12 },
  label: { fontSize: 13, color: colors.textSecondary, marginBottom: 4 },
  input: { backgroundColor: colors.bgInput, borderRadius: 10, paddingHorizontal: 12, paddingVertical: 10, fontSize: 14, color: colors.textPrimary, borderWidth: 1, borderColor: colors.border, marginBottom: 8 },
  textArea: { minHeight: 80, textAlignVertical: 'top' },
  personalityPreview: { fontSize: 14, color: colors.textSecondary, lineHeight: 20, padding: 8, backgroundColor: colors.bgInput, borderRadius: 8 },
  saveBtn: { backgroundColor: colors.accent, borderRadius: 10, paddingVertical: 10, alignItems: 'center', marginTop: 4 },
  saveBtnText: { color: '#fff', fontSize: 14, fontWeight: '600' },
  switchRow: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', paddingVertical: 8 },
  switchLabel: { fontSize: 15, color: colors.textPrimary },
  providerItem: { flexDirection: 'row', alignItems: 'center', paddingVertical: 10, borderBottomWidth: StyleSheet.hairlineWidth, borderBottomColor: colors.divider },
  providerBody: { flex: 1, flexDirection: 'row', alignItems: 'center' },
  radio: { width: 18, height: 18, borderRadius: 9, borderWidth: 2, borderColor: colors.border, marginRight: 10 },
  radioActive: { borderColor: colors.accent, backgroundColor: colors.accent },
  providerName: { fontSize: 15, fontWeight: '500', color: colors.textPrimary },
  providerMeta: { fontSize: 12, color: colors.textTertiary, marginTop: 2 },
  addBtn: { paddingVertical: 12, alignItems: 'center', marginTop: 8 },
  addBtnText: { color: colors.accent, fontSize: 14, fontWeight: '600' },
  addForm: { marginTop: 8 },
});
