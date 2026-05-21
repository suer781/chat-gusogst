import React, { useState, useEffect, useCallback } from 'react';
import { saveSnapshot, loadSnapshot, clearSnapshot, EditSnapshot } from './snapshotStorage';
import type { Persona } from '../types';
import { t, onLangChange } from '../i18n';
import { light as hapticLight, medium as hapticMedium, success as hapticSuccess, selectionStart, selectionChangedThrottled, selectionEnd, glassTap } from '../haptics';
import { useSettingsStore } from '../stores';

interface PersonaSettingsModalProps {
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

export default function PersonaSettingsModal({ visible, persona, onSave, onClose }: PersonaSettingsModalProps) {
  const [prompt, setPrompt] = useState(persona.systemPrompt || '');
  const [autoMode, setAutoMode] = useState<AutoMode>(persona.modelParamsConfig?.autoMode || 'off');
  const [sliders, setSliders] = useState({
    temperature: persona.modelParamsConfig?.temperature ?? 0.7,
    topP: persona.modelParamsConfig?.topP ?? 0.9,
    maxTokens: persona.modelParamsConfig?.maxTokens ?? 2048,
  });
  const [tick, setTick] = useState(0);
  const [overrideGlobal, setOverrideGlobal] = useState(persona.modelParamsConfig?.overrideGlobal ?? false);
  const stateKey = (p: string, s: typeof sliders, og: boolean, am: string) =>
    `${p}|${s.temperature}|${s.topP}|${s.maxTokens}|${og}|${am}`;
  const initialStateRef = React.useRef('');
  const [showConfirmClose, setShowConfirmClose] = useState(false);
  const [showRecovery, setShowRecovery] = useState(false);
  const [showDiscardConfirm, setShowDiscardConfirm] = useState(false);
  const [snapshotData, setSnapshotData] = useState<EditSnapshot | null>(null);
  const [llmLoading, setLlmLoading] = useState(false);
  const [llmResult, setLlmResult] = useState('');
  const apiKey = useSettingsStore((s) => s.model.apiKey);
  const baseUrl = useSettingsStore((s) => s.model.baseUrl);
  const apiHost = useSettingsStore((s) => s.model.apiHost);
  const model = useSettingsStore((s) => s.model);

  useEffect(() => { setPrompt(persona.systemPrompt || ''); }, [persona.systemPrompt]);
  useEffect(() => { initialStateRef.current = stateKey(persona.systemPrompt || '', sliders, overrideGlobal, autoMode); }, [visible]);

  // crash recovery: check for snapshot on open
  useEffect(() => {
    if (visible && persona.id) {
      const snap = loadSnapshot(persona.id);
      if (snap) {
        setSnapshotData(snap);
        setShowRecovery(true);
      }
    }
  }, [visible, persona.id]);
  useEffect(() => { onLangChange(() => setTick(v => v + 1)); }, []);

  const clamp = (v: number, min: number, max: number, step: number) => {
    const rounded = Math.round(v / step) * step;
    return Math.min(max, Math.max(min, Number(rounded.toFixed(2))));
  };

  // 快捷预设：规则引擎
  const autoSave = (p: string, s: typeof sliders, og: boolean, am: AutoMode) => {
    if (!persona.id) return;
    saveSnapshot(persona.id, {
      prompt: p,
      sliders: { temperature: s.temperature, topP: s.topP, maxTokens: s.maxTokens },
      overrideGlobal: og,
      autoMode: am,
    });
  };

  const handleClose = () => {
    const current = stateKey(prompt, sliders, overrideGlobal, autoMode);
    if (current !== initialStateRef.current) { setShowConfirmClose(true); } else { onClose(); }
  };

  const applyRulePreset = () => {
    const adj = analyzeWithRules(prompt);
    setSliders(prev => ({
      temperature: clamp(prev.temperature + adj.temperature, 0, 2, 0.1),
      topP: clamp(prev.topP + adj.topP, 0, 1, 0.05),
      maxTokens: Math.max(100, Math.min(8000, prev.maxTokens + adj.maxTokens)),
    }));
  };

  // 快捷预设：自主理解 — 调模型分析系统提示词
  const applyLLMPreset = useCallback(async () => {
    if (!apiKey) { setLlmResult('请先填写 API Key'); return; }
    setLlmLoading(true);
    setLlmResult('');
    try {
      const host = (apiHost || baseUrl || 'https://api.openai.com').replace(/\/$/, '');
      const endpoint = host + '/v1/chat/completions';
      const metaPrompt = `你是一个 AI 参数调优专家。根据以下系统提示词描述的角色性格和情绪特征，推荐最合适的模型参数。

系统提示词：
${prompt || '（未设置自定义提示词，使用默认助手角色）'}

请分析这个角色的性格特征（如温柔、活泼、严谨、幽默等），然后推荐以下三个参数，并返回严格 JSON 格式：
{
  "temperature": 0.0-2.0 之间的浮点数（活泼/创意角色偏高，严谨/专业角色偏低）,
  "topP": 0.0-1.0 之间的浮点数,
  "maxTokens": 100-8000 之间的整数（话痨角色偏大，简洁角色偏小）
}

只返回 JSON，不要任何其他文字。`;

      const resp = await fetch(endpoint, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + apiKey },
        body: JSON.stringify({ model: model.model, messages: [{ role: 'user', content: metaPrompt }], temperature: 0.3, max_tokens: 256 }),
      });
      if (!resp.ok) throw new Error('API 请求失败: ' + resp.status);
      const data = await resp.json();
      const content = data.choices?.[0]?.message?.content || '';
      const jsonMatch = content.match(/\{[\s\S]*\}/);
      if (!jsonMatch) throw new Error('模型返回格式异常');

      const params = JSON.parse(jsonMatch[0]);
      setSliders({
        temperature: clamp(Number(params.temperature) || 0.7, 0, 2, 0.1),
        topP: clamp(Number(params.topP) || 0.9, 0, 1, 0.05),
        maxTokens: Math.max(100, Math.min(8000, Math.round(Number(params.maxTokens) || 2048))),
      });
      setAutoMode('llm');
      setLlmResult('✅ 模型分析完成');
    } catch (e: any) {
      // 降级到本地分析
      const fallback = analyzeWithLLM(prompt);
      setSliders(fallback);
      setLlmResult('⚠️ 模型调用失败，已降级为本地分析: ' + (e.message || ''));
    } finally {
      setLlmLoading(false);
    }
  }, [apiKey, baseUrl, apiHost, model.model, prompt]);

  const save = () => {
    hapticSuccess();
    onSave({
      systemPrompt: prompt,
      modelParamsConfig: {
        autoMode,
        overrideGlobal,
        temperature: clamp(sliders.temperature, 0, 2, 0.1),
        topP: clamp(sliders.topP, 0, 1, 0.05),
        maxTokens: Math.max(100, Math.round(sliders.maxTokens)),
      },
    });
    if (persona.id) clearSnapshot(persona.id);
    onClose();
  };

  if (!visible) return null;

  const textStyle: React.CSSProperties = { width: '100%', background: 'var(--bg-tertiary)', border: '1px solid var(--border)', borderRadius: 8, color: 'var(--text-primary)', fontSize: "var(--text-base)", padding: '12px', resize: 'vertical', fontFamily: 'inherit', outline: 'none', boxSizing: 'border-box' as const };
  const labelStyle: React.CSSProperties = { fontSize: "var(--text-base)", fontWeight: 600, color: 'var(--gray-100)', marginBottom: 8, display: 'block' };
  const btnStyle = (active: boolean): React.CSSProperties => ({
    flex: 1, padding: '8px 4px', borderRadius: 8, border: active ? '1px solid var(--accent)' : '1px solid var(--border)',
    background: active ? 'var(--accent-soft)' : 'var(--bg-tertiary)', color: active ? 'var(--accent)' : 'var(--gray-300)',
    fontSize: "var(--text-sm)", fontWeight: 600, cursor: 'pointer', transition: 'all 0.2s',
  });

  const Slider = ({ label, emoji, value, min, max, step, unit, onChange, disabled }: {
    label: string; emoji: string; value: number; min: number; max: number; step: number; unit?: string;
    onChange: (v: number) => void; disabled?: boolean;
  }) => (
    <div style={{ opacity: disabled ? 0.4 : 1, pointerEvents: disabled ? 'none' : 'auto', transition: 'opacity 0.2s' }}>
      <label style={labelStyle}>{emoji} {label}</label>
      <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
        <span style={{ fontSize: "var(--text-sm)", color: 'var(--gray-400)', minWidth: 30 }}>{min}{unit}</span>
        <input type="range" min={min} max={max} step={step} value={value}
          onPointerDown={() => selectionStart()}
          onChange={e => { selectionChangedThrottled(); onChange(Number(e.target.value)) }}
          onPointerUp={() => selectionEnd()}
          style={{ flex: 1, accentColor: 'var(--accent)', touchAction: 'pan-y' }} />
        <span style={{ fontSize: "var(--text-sm)", color: 'var(--gray-400)', minWidth: 30, textAlign: 'right' }}>{max}{unit}</span>
      </div>
      <div style={{ textAlign: 'center', fontSize: "var(--text-base)", color: 'var(--accent)', marginTop: 4, transition: 'all 0.3s ease' }}>
        {value}{unit || ''}
      </div>
    </div>
  );

  return (
    <div className="glass-panel" style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.35)', display: 'flex', justifyContent: 'center', alignItems: 'center', zIndex: 1000, padding: 16, backdropFilter: 'blur(8px)', WebkitBackdropFilter: 'blur(8px)' }}>
      <div className="glass-panel" style={{ borderRadius: 16, width: '100%', maxWidth: 520, maxHeight: '85vh', overflow: 'auto', padding: 20, border: '1px solid rgba(255,255,255,0.15)', display: 'flex', flexDirection: 'column', gap: 16, background: 'linear-gradient(165deg, rgba(255,255,255,0.08) 0%, rgba(13,13,43,0.55) 100%)' }}>

        {/* Header */}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <h3 style={{ margin: 0, fontSize: "var(--text-xl)", color: 'var(--text-primary)' }}>{t('persona.settings.title') || '角色设置'}</h3>
          <button onClick={() => { glassTap(); handleClose() }} style={{ background: 'none', border: 'none', color: 'var(--gray-400)', fontSize: "var(--text-2xl)", cursor: 'pointer' }}>✕</button>
        </div>

        {/* 系统提示词 */}
        <div>
          <label style={labelStyle}>📝 {t('persona.settings.systemPrompt') || '系统提示词'}</label>
          <textarea value={prompt} onChange={e => { const v = e.target.value; setPrompt(v); autoSave(v, sliders, overrideGlobal, autoMode); }} rows={5} style={{ ...textStyle, minHeight: 100 }}
            placeholder={t('persona.settings.promptPlaceholder') || '输入角色的系统提示词...'} />
          <div style={{ textAlign: 'right', fontSize: "var(--text-sm)", color: 'var(--gray-400)', marginTop: 4 }}>{prompt.length} chars</div>
        </div>


        {/* 独立参数开关 */}
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '10px 14px', borderRadius: 10, background: 'var(--bg-tertiary)', border: '1px solid var(--border)' }}>
          <div>
            <div style={{ fontSize: "var(--text-base)", fontWeight: 600, color: 'var(--gray-100)' }}>⚙️ 独立参数</div>
            <div style={{ fontSize: "var(--text-sm)", color: 'var(--gray-400)', marginTop: 2 }}>
              {overrideGlobal ? '使用本角色独立参数（覆盖全局）' : '跟随全局模型设置'}
            </div>
          </div>
          <button onClick={() => { hapticMedium(); setOverrideGlobal((v: boolean) => { const nv = !v; autoSave(prompt, sliders, nv, autoMode); return nv; })}} style={{
            width: 44, height: 24, borderRadius: 12, border: 'none', cursor: 'pointer', position: 'relative',
            background: overrideGlobal ? 'var(--accent)' : 'var(--gray-600)', transition: 'background 0.2s',
          }}>
            <span style={{
              position: 'absolute', top: 3, left: overrideGlobal ? 23 : 3, width: 18, height: 18,
              borderRadius: '50%', background: '#fff', transition: 'left 0.2s',
            }} />
          </button>
        </div>

        {/* 以下受独立参数开关控制 */}
        <div style={{ opacity: overrideGlobal ? 1 : 0.35, pointerEvents: overrideGlobal ? 'auto' : 'none', transition: 'opacity 0.3s' }}>

        {/* 快捷预设 */}
        <div>
          <label style={labelStyle}>⚡ 快捷预设（分析提示词，一键设参）</label>
          <div style={{ display: 'flex', gap: 8 }}>
            <button onClick={() => { hapticMedium(); applyRulePreset() }} style={btnStyle(false)}>
              🔧 规则引擎
            </button>
            <button onClick={() => { hapticMedium(); applyLLMPreset() }} disabled={llmLoading} style={btnStyle(false)}>
              {llmLoading ? '⏳ 分析中...' : '🧠 自主理解'}
            </button>
          </div>
          {llmResult && (
            <div style={{ marginTop: 8, fontSize: "var(--text-sm)", color: llmResult.startsWith('✅') ? '#00b894' : '#ff6b6b' }}>
              {llmResult}
            </div>
          )}
        </div>

        {/* 滑条：独立参数开启时可编辑，关闭时灰显 */}
        <Slider label="Temperature（创造力）" emoji="🌡" value={sliders.temperature} min={0} max={2} step={0.1}
          onChange={v => setSliders(prev => { const ns = { ...prev, temperature: v }; autoSave(prompt, ns, overrideGlobal, autoMode); return ns; })} />
        <Slider label="Top P（多样性）" emoji="🎯" value={sliders.topP} min={0} max={1} step={0.05}
          onChange={v => setSliders(prev => { const ns = { ...prev, topP: v }; autoSave(prompt, ns, overrideGlobal, autoMode); return ns; })} />
        <Slider label="Max Tokens（回复长度）" emoji="📏" value={sliders.maxTokens} min={100} max={8000} step={100}
          onChange={v => setSliders(prev => { const ns = { ...prev, maxTokens: v }; autoSave(prompt, ns, overrideGlobal, autoMode); return ns; })} />

        {/* 聊天时自动调节 */}
        <div>
          <label style={labelStyle}>🔄 聊天时自动调节</label>
          <div style={{ display: 'flex', gap: 8 }}>
            {([
              { id: 'off' as AutoMode, label: t('persona.settings.modeOff') },
              { id: 'rule' as AutoMode, label: t('persona.settings.modeRule') },
              { id: 'llm' as AutoMode, label: t('persona.settings.modeLlm') },
            ]).map(opt => (
              <button key={opt.id} onClick={() => setAutoMode(opt.id)} style={btnStyle(autoMode === opt.id)}>
                {opt.label}
              </button>
            ))}
          </div>
        </div>

        </div> {/* /开关控制 */}

        {/* 按钮 */}
        <div style={{ display: 'flex', gap: 12, marginTop: 8 }}>
          <button onClick={handleClose} style={{ flex: 1, padding: '12px', borderRadius: 10, border: '1px solid var(--border)', background: 'var(--bg-tertiary)', color: 'var(--gray-300)', fontSize: "var(--text-base)", cursor: 'pointer' }}>
            {t('persona.settings.cancel') || '取消'}
          </button>
          <button onClick={save} style={{ flex: 2, padding: '12px', borderRadius: 10, border: 'none', background: 'linear-gradient(135deg, var(--accent), var(--accent-hover))', color: 'var(--text-primary)', fontSize: "var(--text-base)", fontWeight: 600, cursor: 'pointer' }}>
            {t('persona.settings.save') || '保存设置'}
          </button>
        </div>
      {showRecovery && snapshotData && (
        <div className="dialog-overlay" style={{position:'fixed',top:0,left:0,right:0,bottom:0,display:'flex',alignItems:'center',justifyContent:'center',zIndex:1000}} onClick={() => {}}>
          <div style={{background:'var(--bg-secondary,#1e1e2e)',borderRadius:16,padding:24,width:'85%',maxWidth:360,boxShadow:'0 8px 40px rgba(0,0,0,0.6), 0 0 0 1px rgba(255,255,255,0.08)',backdropFilter:'blur(20px)',WebkitBackdropFilter:'blur(20px)'}} onClick={(e: React.MouseEvent) => e.stopPropagation()}>
            <div style={{fontSize:20,fontWeight:700,color:'var(--gray-100)',marginBottom:8}}>{'⚠️ 检测到未保存的修改'}</div>
            <div style={{fontSize:14,color:'var(--gray-400)',marginBottom:6,lineHeight:1.5}}>{'上次编辑未正常关闭，您有修改尚未保存。'}</div>
            <div style={{display:'flex',gap:8}}>
              <button onClick={() => { setShowDiscardConfirm(true); }} style={{flex:1,padding:10,borderRadius:10,border:'1px solid var(--border)',background:'var(--bg-tertiary)',color:'#ff6b6b',fontSize:13,cursor:'pointer'}}>{'取消并丢弃'}</button>
              <button onClick={() => { setPrompt(snapshotData.prompt); setSliders(snapshotData.sliders); setOverrideGlobal(snapshotData.overrideGlobal); setShowRecovery(false); setSnapshotData(null); }} style={{flex:1,padding:10,borderRadius:10,border:'1px solid var(--border)',background:'var(--bg-tertiary)',color:'var(--gray-300)',fontSize:13,cursor:'pointer'}}>{'查看'}</button>
              <button onClick={() => { setPrompt(snapshotData.prompt); setSliders(snapshotData.sliders); setOverrideGlobal(snapshotData.overrideGlobal); onSave({systemPrompt:snapshotData.prompt,modelParamsConfig:{autoMode:snapshotData.autoMode,overrideGlobal:snapshotData.overrideGlobal,temperature:clamp(snapshotData.sliders.temperature,0,2,0.1),topP:clamp(snapshotData.sliders.topP,0,1,0.05),maxTokens:Math.max(100,Math.round(snapshotData.sliders.maxTokens))}}); if(persona.id) clearSnapshot(persona.id); setShowRecovery(false); setSnapshotData(null); onClose(); }} style={{flex:1,padding:10,borderRadius:10,border:'none',background:'linear-gradient(135deg,var(--accent),var(--accent-hover))',color:'#fff',fontSize:13,fontWeight:600,cursor:'pointer'}}>{'保存'}</button>
            </div>
          </div>
        </div>
      )}

      {showDiscardConfirm && (
        <div className="dialog-overlay" style={{position:'fixed',top:0,left:0,right:0,bottom:0,display:'flex',alignItems:'center',justifyContent:'center',zIndex:1000}} onClick={() => setShowDiscardConfirm(false)}>
          <div style={{background:'var(--bg-secondary,#1e1e2e)',borderRadius:16,padding:24,width:'85%',maxWidth:360,boxShadow:'0 8px 40px rgba(0,0,0,0.6), 0 0 0 1px rgba(255,255,255,0.08)',backdropFilter:'blur(20px)',WebkitBackdropFilter:'blur(20px)'}} onClick={(e: React.MouseEvent) => e.stopPropagation()}>
            <div style={{fontSize:20,fontWeight:700,color:'var(--gray-100)',marginBottom:8}}>{'⚠️ 确认丢弃？'}</div>
            <div style={{fontSize:14,color:'var(--gray-400)',marginBottom:20,lineHeight:1.5}}>{'取消并丢弃会丢失上一次做出的修改，此操作不可恢复。'}</div>
            <div style={{display:'flex',gap:10}}>
              <button onClick={() => setShowDiscardConfirm(false)} style={{flex:1,padding:10,borderRadius:10,border:'1px solid var(--border)',background:'var(--bg-tertiary)',color:'var(--gray-300)',fontSize:14,cursor:'pointer'}}>{'继续编辑'}</button>
              <button onClick={() => { if(persona.id) clearSnapshot(persona.id); setShowDiscardConfirm(false); setShowRecovery(false); setSnapshotData(null); onClose(); }} style={{flex:1,padding:10,borderRadius:10,border:'none',background:'#ff6b6b',color:'#fff',fontSize:14,fontWeight:600,cursor:'pointer'}}>{'确认丢弃'}</button>
            </div>
          </div>
        </div>
      )}

      {showConfirmClose && (
        <div className="dialog-overlay" style={{position:'fixed',top:0,left:0,right:0,bottom:0,display:'flex',alignItems:'center',justifyContent:'center',zIndex:1000}} onClick={() => setShowConfirmClose(false)}>
          <div style={{background:'var(--bg-secondary,#1e1e2e)',borderRadius:16,padding:24,width:'85%',maxWidth:360,boxShadow:'0 8px 40px rgba(0,0,0,0.6), 0 0 0 1px rgba(255,255,255,0.08)',backdropFilter:'blur(20px)',WebkitBackdropFilter:'blur(20px)'}} onClick={(e: React.MouseEvent) => e.stopPropagation()}>
            <div style={{fontSize:20,fontWeight:700,color:'var(--gray-100)',marginBottom:8}}>{'⚠️ 退出编辑？'}</div>
            <div style={{fontSize:14,color:'var(--gray-400)',marginBottom:20,lineHeight:1.5}}>{'已自动保存，确定要退出吗？'}</div>
            <div style={{display:'flex',gap:10}}>
              <button onClick={() => setShowConfirmClose(false)} style={{flex:1,padding:10,borderRadius:10,border:'1px solid var(--border)',background:'var(--bg-tertiary)',color:'var(--gray-300)',fontSize:14,cursor:'pointer'}}>{'继续编辑'}</button>
              <button onClick={onClose} style={{flex:1,padding:10,borderRadius:10,border:'none',background:'#ff6b6b',color:'#fff',fontSize:14,fontWeight:600,cursor:'pointer'}}>{'退出'}</button>
            </div>
          </div>
        </div>
      )}

      </div>
    </div>
  );
}
