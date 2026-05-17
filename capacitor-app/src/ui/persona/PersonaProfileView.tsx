import React, { useState, useEffect } from 'react';
import type { Persona } from '../types';
import { t, onLangChange } from '../i18n';
import PersonaSettingsModal from './PersonaSettingsModal';

interface Props {
  persona: Persona;
  onBack: () => void;
  onStartChat: () => void;
  onUpdate?: (updated: Persona) => void;
}

export default function PersonaProfileView({ persona, onBack, onStartChat, onUpdate }: Props) {
  const [showSettings, setShowSettings] = useState(false);
  const [, setTick] = useState(0);
  useEffect(() => { onLangChange(() => setTick(v => v + 1)); }, []);

  const handleSave = (updates: Partial<Persona>) => {
    if (onUpdate) onUpdate({ ...persona, ...updates });
  };

  const config = persona.modelParamsConfig;
  const paramsBadge = config
    ? config.mode === 'rule' ? '⚡ Auto (规则)' : config.mode === 'llm' ? '🧠 Auto (LLM)' : '⚙️ Manual'
    : null;

  return (
    <>
      <div style={{ height: '100%', display: 'flex', flexDirection: 'column', background: '#0f0f23', color: '#e0e0e0', padding: 16, gap: 16, overflow: 'auto' }}>

        {/* Header */}
        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          <div style={{ width: 64, height: 64, borderRadius: 16, background: 'linear-gradient(135deg, #e94560, #c73e54)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 32, flexShrink: 0 }}>
            {persona.emoji || persona.name.charAt(0)}
          </div>
          <div style={{ flex: 1, minWidth: 0 }}>
            <div style={{ fontSize: 20, fontWeight: 700 }}>{persona.name}</div>
            <div style={{ fontSize: 12, color: '#8888aa', marginTop: 4 }}>
              {persona.personality && Object.entries(persona.personality).map(([k, v]) => `${k} ${Math.round(v * 100)}%`).join(' · ')}
            </div>
          </div>
          {/* ⚙️ Gear Button */}
          <div
            onClick={() => setShowSettings(true)}
            style={{ width: 36, height: 36, borderRadius: 10, background: '#1a1a3a', border: '1px solid #333355', display: 'flex', alignItems: 'center', justifyContent: 'center', cursor: 'pointer', flexShrink: 0, fontSize: 18, transition: 'all 0.2s' }}
            title={t('persona.settings.title')}
          >
            ⚙️
          </div>
        </div>

        {/* Params Badge */}
        {paramsBadge && (
          <div style={{ fontSize: 12, color: '#e94560', background: 'rgba(233,69,96,0.1)', padding: '6px 10px', borderRadius: 8, textAlign: 'center' }}>
            {paramsBadge}
          </div>
        )}

        {/* Tags */}
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6 }}>
          {persona.tags.map(tag => (
            <span key={tag} style={{ fontSize: 12, padding: '4px 10px', borderRadius: 12, background: '#1a1a3a', color: '#8888aa' }}>{tag}</span>
          ))}
        </div>

        {/* System Prompt Preview */}
        <div style={{ flex: 1, background: '#1a1a3a', borderRadius: 12, padding: 16, fontSize: 14, color: '#c0c0d0', whiteSpace: 'pre-wrap', lineHeight: 1.6, overflow: 'auto', minHeight: 80 }}>
          {persona.systemPrompt || 'No system prompt set'}
        </div>

        {/* Buttons */}
        <div style={{ display: 'flex', gap: 10 }}>
          <button onClick={onBack} style={{ flex: 1, padding: '12px 0', borderRadius: 10, border: '1px solid #666688', background: 'transparent', color: '#8888aa', fontSize: 14, cursor: 'pointer' }}>{t('btn.back')}</button>
          <button onClick={onStartChat} style={{ flex: 2, padding: '12px 0', borderRadius: 10, border: 'none', background: '#e94560', color: '#fff', fontSize: 14, fontWeight: 700, cursor: 'pointer' }}>{t('btn.startChat')}</button>
        </div>
      </div>

      {/* Settings Modal */}
      <PersonaSettingsModal
        visible={showSettings}
        persona={persona}
        onSave={handleSave}
        onClose={() => setShowSettings(false)}
      />
    </>
  );
}
