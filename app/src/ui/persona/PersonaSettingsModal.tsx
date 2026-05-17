import React, { useState, useEffect } from 'react';
import type { Persona } from '../types';
import { t, onLangChange } from '../i18n';

interface Props {
  visible: boolean;
  persona: Persona;
  onSave: (updates: Partial<Persona>) => void;
  onClose: () => void;
}

type ParamsMode = 'rule' | 'llm' | 'off';

export default function PersonaSettingsModal({ visible, persona, onSave, onClose }: Props) {
  const [prompt, setPrompt] = useState(persona.systemPrompt || '');
  const [mode, setMode] = useState<ParamsMode>(persona.modelParamsConfig?.mode || 'off');
  const [manual, setManual] = useState({
    temperature: persona.modelParamsConfig?.manual?.temperature ?? 0.7,
    topP: persona.modelParamsConfig?.manual?.topP ?? 0.9,
    maxTokens: persona.modelParamsConfig?.manual?.maxTokens ?? 2048,
  });
  const [, setTick] = useState(0);
  const isAuto = mode === 'rule' || mode === 'llm';

  useEffect(() => { setPrompt(persona.systemPrompt || ''); }, [persona.systemPrompt]);
  useEffect(() => { onLangChange(() => setTick(v => v + 1)); }, []);

  const clamp = (v: number, min: number, max: number, step: number) => {
    const rounded = Math.round(v / step) * step;
    return Math.min(max, Math.max(min, Number(rounded.toFixed(2))));
  };

  const save = () => {
    onSave({
      systemPrompt: prompt,
      modelParamsConfig: {
        mode,
        ...(mode === 'off' ? { manual: {
          temperature: clamp(manual.temperature, 0, 2, 0.1),
          topP: clamp(manual.topP, 0, 1, 0.05),
          maxTokens: Math.max(1, Math.round(manual.maxTokens)),
        }} : {}),
      },
    });
    onClose();
  };

  if (!visible) return null;

  const textStyle: React.CSSProperties = { width: '100%', background: '#1a1a3a', border: '1px solid #333355', borderRadius: 8, color: '#e0e0e0', fontSize: 14, padding: '12px', resize: 'vertical', fontFamily: 'inherit', outline: 'none', boxSizing: 'border-box' as const };
  const labelStyle: React.CSSProperties = { fontSize: 14, fontWeight: 600, color: '#aaaacc', marginBottom: 8, display: 'block' };
  const modeBtn = (active: boolean): React.CSSProperties => ({
    flex: 1, padding: '8px 4px', borderRadius: 8, border: active ? '1px solid #e94560' : '1px solid #333355',
    background: active ? 'rgba(233,69,96,0.15)' : '#1a1a3a', color: active ? '#e94560' : '#8888aa',
    fontSize: 12, fontWeight: 600, cursor: 'pointer', transition: 'all 0.2s',
  });

  return (
    <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.7)', display: 'flex', justifyContent: 'center', alignItems: 'center', zIndex: 1000, padding: 16 }}>
      <div style={{ background: '#0f0f23', borderRadius: 16, width: '100%', maxWidth: 520, maxHeight: '85vh', overflow: 'auto', padding: 20, border: '1px solid #222244', display: 'flex', flexDirection: 'column', gap: 16 }}>

        {/* Header */}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <div style={{ fontSize: 16, fontWeight: 700, color: '#e0e0e0' }}>{t('persona.settings.title')}</div>
          <span onClick={onClose} style={{ fontSize: 22, color: '#666', cursor: 'pointer', padding: '0 4px' }}>✕</span>
        </div>

        {/* System Prompt */}
        <div>
          <label style={labelStyle}>📝 {t('persona.settings.systemPrompt')}</label>
          <textarea value={prompt} onChange={e => setPrompt(e.target.value)} rows={10} style={textStyle} placeholder={t('persona.settings.promptPlaceholder')} />
          <div style={{ fontSize: 12, color: '#666', marginTop: 4 }}>{prompt.length} chars</div>
        </div>

        {/* Divider */}
        <div style={{ height: 1, background: '#222244' }} />

        {/* Parameter Adjustment Mode */}
        <div>
          <label style={labelStyle}>⚡ {t('persona.settings.paramsMode')}</label>
          <div style={{ display: 'flex', gap: 8, marginBottom: 8 }}>
            <button onClick={() => setMode('rule')} style={modeBtn(mode === 'rule')}>{t('persona.settings.modeRule')}</button>
            <button onClick={() => setMode('llm')} style={modeBtn(mode === 'llm')}>{t('persona.settings.modeLlm')}</button>
            <button onClick={() => setMode('off')} style={modeBtn(mode === 'off')}>{t('persona.settings.modeOff')}</button>
          </div>
          <div style={{ fontSize: 12, color: '#8888aa', padding: '8px 12px', background: '#1a1a3a', borderRadius: 8, lineHeight: 1.5 }}>
            {mode === 'rule' && `💡 ${t('persona.settings.modeRuleDesc')}`}
            {mode === 'llm' && `🧠 ${t('persona.settings.modeLlmDesc')}`}
            {mode === 'off' && `⚙️ ${t('persona.settings.modeOffDesc')}`}
          </div>
        </div>

        {/* Manual Params */}
        {!isAuto && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
            <div>
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 4 }}>
                <span style={{ fontSize: 13, color: '#aaaacc' }}>{t('persona.settings.temperature')}</span>
                <span style={{ fontSize: 13, color: '#e94560', fontWeight: 600 }}>{manual.temperature.toFixed(1)}</span>
              </div>
              <input type="range" min={0} max={2} step={0.1} value={manual.temperature} onChange={e => setManual(m => ({ ...m, temperature: parseFloat(e.target.value) }))} style={{ width: '100%', accentColor: '#e94560' }} />
              <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 11, color: '#555' }}><span>0 (精确)</span><span>2 (创意)</span></div>
            </div>
            <div>
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 4 }}>
                <span style={{ fontSize: 13, color: '#aaaacc' }}>{t('persona.settings.topP')}</span>
                <span style={{ fontSize: 13, color: '#e94560', fontWeight: 600 }}>{manual.topP.toFixed(2)}</span>
              </div>
              <input type="range" min={0} max={1} step={0.05} value={manual.topP} onChange={e => setManual(m => ({ ...m, topP: parseFloat(e.target.value) }))} style={{ width: '100%', accentColor: '#e94560' }} />
              <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 11, color: '#555' }}><span>0 (窄选)</span><span>1 (宽选)</span></div>
            </div>
            <div>
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 4 }}>
                <span style={{ fontSize: 13, color: '#aaaacc' }}>{t('persona.settings.maxTokens')}</span>
                <span style={{ fontSize: 13, color: '#e94560', fontWeight: 600 }}>{manual.maxTokens}</span>
              </div>
              <input type="range" min={100} max={8000} step={100} value={manual.maxTokens} onChange={e => setManual(m => ({ ...m, maxTokens: parseInt(e.target.value) }))} style={{ width: '100%', accentColor: '#e94560' }} />
              <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 11, color: '#555' }}><span>100</span><span>8000</span></div>
            </div>
          </div>
        )}

        {/* Buttons */}
        <div style={{ display: 'flex', gap: 10, marginTop: 4 }}>
          <button onClick={onClose} style={{ flex: 1, padding: '12px 0', borderRadius: 10, border: '1px solid #666688', background: 'transparent', color: '#8888aa', fontSize: 14, cursor: 'pointer' }}>{t('persona.settings.cancel')}</button>
          <button onClick={save} style={{ flex: 2, padding: '12px 0', borderRadius: 10, border: 'none', background: '#e94560', color: '#fff', fontSize: 14, fontWeight: 700, cursor: 'pointer' }}>{t('persona.settings.save')}</button>
        </div>
      </div>
    </div>
  );
}
