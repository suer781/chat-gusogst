import React, { useState, useEffect } from 'react';
import type { Persona } from '../types';
import { t, onLangChange } from '../i18n';

interface Props {
  visible: boolean;
  persona: Persona;
  onSave: (updates: Partial<Persona>) => void;
  onClose: () => void;
}

// 规则引擎：关键词 → 参数映射
const RULE_TABLE: { keywords: string[]; adjust: Partial<{ temperature: number; topP: number; maxTokens: number }> }[] = [
  { keywords: ['认真', '严肃', '正式', '严谨', '精确', '专业'], adjust: { temperature: -0.3, topP: -0.1 } },
  { keywords: ['活泼', '搞笑', '幽默', '开心', '有趣', '可爱', '轻松'], adjust: { temperature: +0.3, topP: +0.05 } },
  { keywords: ['详细', '展开', '深入', '全面', '长文'], adjust: { maxTokens: +1024 } },
  { keywords: ['简短', '简洁', '长话短说', '快说', '精简'], adjust: { maxTokens: -512 } },
  { keywords: ['创意', '想象', '天马行空', '脑洞'], adjust: { temperature: +0.4, topP: +0.1 } },
  { keywords: ['客观', '中立', '事实', '准确'], adjust: { temperature: -0.4, topP: -0.15 } },
];

// 规则引擎分析提示词，返回调整量
function analyzeWithRules(prompt: string) {
  const result = { temperature: 0, topP: 0, maxTokens: 0 };
  for (const rule of RULE_TABLE) {
    if (rule.keywords.some(kw => prompt.includes(kw))) {
      if (rule.adjust.temperature) result.temperature += rule.adjust.temperature;
      if (rule.adjust.topP) result.topP += rule.adjust.topP;
      if (rule.adjust.maxTokens) result.maxTokens += rule.adjust.maxTokens;
    }
  }
  return result;
}

// 自主理解：基于提示词语义特征估算参数
function analyzeWithLLM(prompt: string) {
  // 简单语义分析：句子长度、标点密度、情感词比例
  const len = prompt.length;
  const exclamation = (prompt.match(/[！!？?。.]/g) || []).length;
  const emotionalWords = ['喜欢', '讨厌', '爱', '恨', '开心', '难过', '兴奋', '无聊', '感动', '愤怒', '温柔', '冷酷', '热情', '冷漠'];
  const emotionCount = emotionalWords.filter(w => prompt.includes(w)).length;
  
  // 基础值
  let temperature = 0.7;
  let topP = 0.9;
  let maxTokens = 2048;
  
  // 情感丰富 → 提高温度
  if (emotionCount > 2) temperature += 0.3;
  else if (emotionCount === 0) temperature -= 0.2;
  
  // 标点多 → 语气强烈 → 适当提高温度
  if (exclamation > 3) temperature += 0.15;
  
  // 长提示词 → 可能需要更长回复
  if (len > 500) maxTokens = 4096;
  else if (len < 100) maxTokens = 1024;
  
  // 含规则性词汇 → 降低温度
  const ruleWords = ['规则', '格式', '模板', '必须', '禁止', '不要', '严格'];
  if (ruleWords.some(w => prompt.includes(w))) {
    temperature -= 0.3;
    topP -= 0.1;
  }
  
  return {
    temperature: Math.round(Math.max(0, Math.min(2, temperature)) * 10) / 10,
    topP: Math.round(Math.max(0, Math.min(1, topP)) * 20) / 20,
    maxTokens: Math.max(100, Math.min(8000, maxTokens)),
  };
}

type AutoMode = 'off' | 'rule' | 'llm';

export default function PersonaSettingsModal({ visible, persona, onSave, onClose }: Props) {
  const [prompt, setPrompt] = useState(persona.systemPrompt || '');
  const [autoMode, setAutoMode] = useState<AutoMode>(persona.modelParamsConfig?.autoMode || 'off');
  const [sliders, setSliders] = useState({
    temperature: persona.modelParamsConfig?.temperature ?? 0.7,
    topP: persona.modelParamsConfig?.topP ?? 0.9,
    maxTokens: persona.modelParamsConfig?.maxTokens ?? 2048,
  });
  const [tick, setTick] = useState(0);

  useEffect(() => { setPrompt(persona.systemPrompt || ''); }, [persona.systemPrompt]);
  useEffect(() => { onLangChange(() => setTick(v => v + 1)); }, []);

  const clamp = (v: number, min: number, max: number, step: number) => {
    const rounded = Math.round(v / step) * step;
    return Math.min(max, Math.max(min, Number(rounded.toFixed(2))));
  };

  // 快捷预设：规则引擎
  const applyRulePreset = () => {
    const adj = analyzeWithRules(prompt);
    setSliders(prev => ({
      temperature: clamp(prev.temperature + adj.temperature, 0, 2, 0.1),
      topP: clamp(prev.topP + adj.topP, 0, 1, 0.05),
      maxTokens: Math.max(100, Math.min(8000, prev.maxTokens + adj.maxTokens)),
    }));
  };

  // 快捷预设：自主理解
  const applyLLMPreset = () => {
    const result = analyzeWithLLM(prompt);
    setSliders(result);
  };

  const save = () => {
    onSave({
      systemPrompt: prompt,
      modelParamsConfig: {
        autoMode,
        temperature: clamp(sliders.temperature, 0, 2, 0.1),
        topP: clamp(sliders.topP, 0, 1, 0.05),
        maxTokens: Math.max(100, Math.round(sliders.maxTokens)),
      },
    });
    onClose();
  };

  if (!visible) return null;

  const textStyle: React.CSSProperties = { width: '100%', background: '#1a1a3a', border: '1px solid #333355', borderRadius: 8, color: '#e0e0e0', fontSize: 14, padding: '12px', resize: 'vertical', fontFamily: 'inherit', outline: 'none', boxSizing: 'border-box' as const };
  const labelStyle: React.CSSProperties = { fontSize: 14, fontWeight: 600, color: '#aaaacc', marginBottom: 8, display: 'block' };
  const btnStyle = (active: boolean): React.CSSProperties => ({
    flex: 1, padding: '8px 4px', borderRadius: 8, border: active ? '1px solid #e94560' : '1px solid #333355',
    background: active ? 'rgba(233,69,96,0.15)' : '#1a1a3a', color: active ? '#e94560' : '#8888aa',
    fontSize: 12, fontWeight: 600, cursor: 'pointer', transition: 'all 0.2s',
  });

  const Slider = ({ label, emoji, value, min, max, step, unit, onChange }: {
    label: string; emoji: string; value: number; min: number; max: number; step: number; unit?: string;
    onChange: (v: number) => void;
  }) => (
    <div>
      <label style={labelStyle}>{emoji} {label}</label>
      <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
        <span style={{ fontSize: 12, color: '#666', minWidth: 30 }}>{min}{unit}</span>
        <input type="range" min={min} max={max} step={step} value={value}
          onChange={e => onChange(Number(e.target.value))}
          style={{ flex: 1, accentColor: '#e94560' }} />
        <span style={{ fontSize: 12, color: '#666', minWidth: 30, textAlign: 'right' }}>{max}{unit}</span>
      </div>
      <div style={{ textAlign: 'center', fontSize: 13, color: '#e94560', marginTop: 4 }}>
        {value}{unit || ''}
      </div>
    </div>
  );

  return (
    <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.7)', display: 'flex', justifyContent: 'center', alignItems: 'center', zIndex: 1000, padding: 16 }}>
      <div style={{ background: '#0f0f23', borderRadius: 16, width: '100%', maxWidth: 520, maxHeight: '85vh', overflow: 'auto', padding: 20, border: '1px solid #222244', display: 'flex', flexDirection: 'column', gap: 16 }}>

        {/* Header */}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <h3 style={{ margin: 0, fontSize: 18, color: '#e0e0e0' }}>{t('persona.settings') || '角色设置'}</h3>
          <button onClick={onClose} style={{ background: 'none', border: 'none', color: '#888', fontSize: 20, cursor: 'pointer' }}>✕</button>
        </div>

        {/* 系统提示词 */}
        <div>
          <label style={labelStyle}>📝 {t('persona.systemPrompt') || '系统提示词'}</label>
          <textarea value={prompt} onChange={e => setPrompt(e.target.value)} rows={5} style={{ ...textStyle, minHeight: 100 }}
            placeholder={t('persona.systemPromptPlaceholder') || '输入角色的系统提示词...'} />
          <div style={{ textAlign: 'right', fontSize: 12, color: '#666', marginTop: 4 }}>{prompt.length} chars</div>
        </div>

        {/* 快捷预设 */}
        <div>
          <label style={labelStyle}>⚡ 快捷预设（分析提示词，一键设参）</label>
          <div style={{ display: 'flex', gap: 8 }}>
            <button onClick={applyRulePreset} style={btnStyle(false)}>
              🔧 规则引擎
            </button>
            <button onClick={applyLLMPreset} style={btnStyle(false)}>
              🧠 自主理解
            </button>
          </div>
        </div>

        {/* 滑条始终可见 */}
        <Slider label="Temperature（创造力）" emoji="🌡" value={sliders.temperature} min={0} max={2} step={0.1}
          onChange={v => setSliders(prev => ({ ...prev, temperature: v }))} />
        <Slider label="Top P（多样性）" emoji="🎯" value={sliders.topP} min={0} max={1} step={0.05}
          onChange={v => setSliders(prev => ({ ...prev, topP: v }))} />
        <Slider label="Max Tokens（回复长度）" emoji="📏" value={sliders.maxTokens} min={100} max={8000} step={100}
          onChange={v => setSliders(prev => ({ ...prev, maxTokens: v }))} />

        {/* 聊天时自动调节 */}
        <div>
          <label style={labelStyle}>🔄 聊天时自动调节</label>
          <div style={{ display: 'flex', gap: 8 }}>
            {([
              { id: 'off' as AutoMode, label: '无' },
              { id: 'rule' as AutoMode, label: '规则引擎' },
              { id: 'llm' as AutoMode, label: '自主理解' },
            ]).map(opt => (
              <button key={opt.id} onClick={() => setAutoMode(opt.id)} style={btnStyle(autoMode === opt.id)}>
                {opt.label}
              </button>
            ))}
          </div>
        </div>

        {/* 按钮 */}
        <div style={{ display: 'flex', gap: 12, marginTop: 8 }}>
          <button onClick={onClose} style={{ flex: 1, padding: '12px', borderRadius: 10, border: '1px solid #333355', background: '#1a1a3a', color: '#8888aa', fontSize: 14, cursor: 'pointer' }}>
            {t('common.cancel') || '取消'}
          </button>
          <button onClick={save} style={{ flex: 2, padding: '12px', borderRadius: 10, border: 'none', background: 'linear-gradient(135deg, #e94560, #c73e54)', color: '#fff', fontSize: 14, fontWeight: 600, cursor: 'pointer' }}>
            {t('persona.save') || '保存设置'}
          </button>
        </div>
      </div>
    </div>
  );
}
