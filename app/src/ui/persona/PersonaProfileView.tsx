import React, { useState, useEffect } from 'react';
import type { Persona } from '../types';
import { t, onLangChange } from '../i18n';
import PersonaSettingsModal from './PersonaSettingsModal';

interface PersonaProfileViewProps {
  persona: Persona;
  onBack: () => void;
  onStartChat: () => void;
  onUpdate?: (updated: Persona) => void;
}

export default function PersonaProfileView({ persona, onBack, onStartChat, onUpdate }: PersonaProfileViewProps) {
  const [showSettings, setShowSettings] = useState(false);
  const [, setTick] = useState(0);
  useEffect(() => { onLangChange(() => setTick(v => v + 1)); }, []);

  const handleSave = (updates: Partial<Persona>) => {
    if (onUpdate) onUpdate({ ...persona, ...updates });
  };

  const config = persona.modelParamsConfig;
  const paramsBadge = config
    ? config.autoMode === "rule" ? '⚡ Auto (规则)' : config.autoMode === "llm" ? '🧠 Auto (LLM)' : null
    : null;

  return (
    <>
      <div className="glass-panel flex-1 flex flex-col overflow-y-auto" style={{ minHeight: 0, color: 'var(--text-primary)', padding: 16, gap: 16 }}>

        {/* Header */}
        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          <div style={{ width: 64, height: 64, borderRadius: "var(--radius-lg)", background: 'linear-gradient(135deg, var(--accent), var(--accent-hover))', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: "var(--text-4xl)", flexShrink: 0 }}>
            {persona.emoji || persona.name.charAt(0)}
          </div>
          <div style={{ flex: 1, minWidth: 0 }}>
            <div style={{ fontSize: "var(--text-2xl)", fontWeight: 700 }}>{persona.name}</div>
            <div style={{ fontSize: "var(--text-sm)", color: 'var(--text-secondary)', marginTop: 4 }}>
              {persona.personality && Object.entries(persona.personality).map(([k, v]) => `${k} ${Math.round(v * 100)}%`).join(' · ')}
            </div>
          </div>
          {/* ⚙️ Gear Button */}
          <div
            onClick={() => setShowSettings(true)}
            style={{ width: 36, height: 36, borderRadius: "var(--radius-md)", background: 'var(--bg-secondary)', border: '1px solid #333355', display: 'flex', alignItems: 'center', justifyContent: 'center', cursor: 'pointer', flexShrink: 0, fontSize: "var(--text-xl)", transition: 'all 0.2s' }}
            title={t('persona.settings.title')}
          >
            ⚙️
          </div>
        </div>

        {/* Params Badge */}
        {paramsBadge && (
          <div style={{ fontSize: "var(--text-sm)", color: 'var(--accent)', background: 'rgba(233,69,96,0.1)', padding: '6px 10px', borderRadius: "var(--radius-sm)", textAlign: 'center' }}>
            {paramsBadge}
          </div>
        )}

        {/* Tags */}
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6 }}>
          {persona.tags.map(tag => (
            <span key={tag} style={{ fontSize: "var(--text-sm)", padding: '4px 10px', borderRadius: "var(--radius-md)", background: 'var(--bg-secondary)', color: 'var(--text-secondary)' }}>{tag}</span>
          ))}
        </div>

        {/* System Prompt Preview */}
        <div style={{ flex: 1, background: 'var(--bg-secondary)', borderRadius: "var(--radius-md)", padding: 16, fontSize: "var(--text-base)", color: 'var(--text-primary)', whiteSpace: 'pre-wrap', lineHeight: 1.6, overflow: 'auto', minHeight: 80 }}>
          {persona.systemPrompt || 'No system prompt set'}
        </div>

        {/* Buttons */}
        <div style={{ display: 'flex', gap: 10 }}>
          <button onClick={onBack} style={{ flex: 1, padding: '12px 0', borderRadius: "var(--radius-md)", border: '1px solid var(--border-color)', background: 'transparent', color: 'var(--text-secondary)', fontSize: "var(--text-base)", cursor: 'pointer' }}>{t('btn.back')}</button>
          <button onClick={onStartChat} style={{ flex: 2, padding: '12px 0', borderRadius: "var(--radius-md)", border: 'none', background: 'var(--accent)', color: 'var(--text-primary)', fontSize: "var(--text-base)", fontWeight: 700, cursor: 'pointer' }}>{t('btn.startChat')}</button>
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
