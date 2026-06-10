import { useState, useMemo, useCallback } from 'react';
import { useSettingsStore } from '../stores';
import {
  ChevronRight, ChevronDown, Search, RefreshCw,
  ExternalLink, Check, X, Sparkles, BookOpen
} from 'lucide-react';
import { t, onLangChange } from '../i18n';
import providersData from '../../data/providers-registry.json';

type ModelInfo = { id: string; name: string; context_length: number; max_output: number; cost_input: number; cost_output: number };
type ProviderInfo = { id: string; name: string; env_key: string; base_url: string; doc: string; api: string; models: ModelInfo[] };

const providers = providersData as ProviderInfo[];

/* ── 分类定义 ── */
const CATEGORIES = [
  { key: 'recommended', label: '推荐', emoji: '⭐' },
  { key: 'aggregator',  label: '聚合', emoji: '🔗' },
  { key: 'domestic',    label: '国产', emoji: '🇨🇳' },
  { key: 'overseas',    label: '海外', emoji: '🌏' },
  { key: 'all',         label: '全部', emoji: '📋' },
] as const;

/* 硬编码分类映射（精确匹配） */
const EXACT_MAP: Record<string, string> = {
  'nano-gpt': 'aggregator', 'wafer.ai': 'aggregator',
  'kuae-cloud-coding-plan': 'domestic', 'tencent-tokenhub': 'domestic',
  'xpersona': 'overseas', 'abliteration-ai': 'overseas',
  'claudinio': 'overseas', 'firepass': 'overseas',
};

/* 关键词推断（模糊匹配） */
const DOMESTIC_KW = ['zhipu','glm','qwen','wenxin','ernie','tongyi','doubao','deepseek','tencent','kuae','step','hunyuan','minimax','moonshot','kimi'];
const AGGREGATOR_KW = ['nano','wafer','router','proxy','relay','openrouter'];

function classify(id: string): string {
  if (EXACT_MAP[id]) return EXACT_MAP[id];
  const low = id.toLowerCase();
  if (DOMESTIC_KW.some(k => low.includes(k))) return 'domestic';
  if (AGGREGATOR_KW.some(k => low.includes(k))) return 'aggregator';
  return 'overseas';
}

const RECOMMENDED = ['nano-gpt','openai','anthropic','zhipu','deepseek'];

/* ── 主组件 ── */
export function ProviderSettings({ onDone }: { onDone: () => void }) {
  const [search, setSearch]           = useState('');
  const [tab, setTab]                 = useState('recommended');
  const [expandedId, setExpandedId]   = useState<string|null>(null);
  const [modelSearch, setModelSearch] = useState('');
  const [apiKeys, setApiKeys]         = useState<Record<string,string>>({});
  const [baseUrls, setBaseUrls]       = useState<Record<string,string>>({});
  const [fetching, setFetching]       = useState<string|null>(null);
  const [liveModels, setLiveModels]   = useState<Record<string,ModelInfo[]>>({});
  const [connected, setConnected]     = useState<Record<string,boolean>>({});
  const [showGuide, setShowGuide]     = useState(() => !localStorage.getItem('provider-guide-seen'));
  const [, forceRender]               = useState(0);

  const { model, setModel } = useSettingsStore();
  onLangChange(() => forceRender(n => n + 1));

  /* ── helpers ── */
  const fmtCtx  = useCallback((n: number) => {
    if (!n) return '';
    if (n >= 1e6) return (n / 1e6).toFixed(1).replace(/\.0$/, '') + 'M';
    if (n >= 1e3) return Math.round(n / 1e3) + 'K';
    return String(n);
  }, []);
  const fmtCost = useCallback((n: number) => n ? '$' + n.toFixed(4) : '', []);

  const totalModels = useMemo(() =>
    providers.reduce((s, p) => s + (p.models?.length || 0), 0), []);

  /* ── 分类 + 搜索过滤 ── */
  const filtered = useMemo(() => {
    if (search) {
      const q = search.toLowerCase();
      return providers.filter(p =>
        p.id.toLowerCase().includes(q) || p.name.toLowerCase().includes(q) ||
        (p.models||[]).some(m => m.id.toLowerCase().includes(q) || (m.name||'').toLowerCase().includes(q))
      );
    }
    if (tab === 'all') return providers;
    if (tab === 'recommended') return providers.filter(p => RECOMMENDED.includes(p.id));
    return providers.filter(p => classify(p.id) === tab);
  }, [search, tab]);

  /* ── 获取实时模型 ── */
  const fetchLive = useCallback(async (p: ProviderInfo) => {
    const key = apiKeys[p.id];
    if (!key) return;
    setFetching(p.id);
    try {
      const url = (baseUrls[p.id] || p.base_url).replace(/\/+$/, '');
      const r = await fetch(`${url}/models`, { headers: { Authorization: `Bearer ${key}` } });
      if (!r.ok) throw new Error(r.statusText);
      const d = await r.json();
      const list: ModelInfo[] = (d.data || d.models || d || []).map((m: any) => ({
        id: m.id || m.name || '', name: m.name || m.id || '',
        context_length: m.context_length || 0, max_output: m.max_output || 0,
        cost_input: m.cost_input || 0, cost_output: m.cost_output || 0,
      }));
      setLiveModels(prev => ({ ...prev, [p.id]: list.length ? list : p.models }));
      setConnected(prev => ({ ...prev, [p.id]: true }));
    } catch {
      setConnected(prev => ({ ...prev, [p.id]: false }));
      setLiveModels(prev => ({ ...prev, [p.id]: p.models }));
    } finally { setFetching(null); }
  }, [apiKeys, baseUrls]);

  const getModels = useCallback((p: ProviderInfo): ModelInfo[] => {
    const list = liveModels[p.id] || p.models || [];
    if (!modelSearch || expandedId !== p.id) return list;
    const q = modelSearch.toLowerCase();
    return list.filter(m => m.id.toLowerCase().includes(q) || (m.name||'').toLowerCase().includes(q));
  }, [liveModels, modelSearch, expandedId]);

  const dismissGuide = () => { setShowGuide(false); localStorage.setItem('provider-guide-seen', '1'); };

  /* ── 卡片组件 ── */
  const Card = (p: ProviderInfo) => {
    const open      = expandedId === p.id;
    const hasKey    = !!apiKeys[p.id];
    const isLive    = !!liveModels[p.id];
    const ok        = connected[p.id];
    const active    = model?.provider === p.id;
    const models    = getModels(p);

    return (
      <div key={p.id} style={{
        background: 'var(--bg-card, #fff)', borderRadius: 14, marginBottom: 8,
        border: active ? '2px solid var(--accent, #6C5CE7)' : '1px solid var(--border-light, rgba(0,0,0,0.06))',
        transition: 'border .2s', overflow: 'hidden',
      }}>
        {/* ── 卡片头（始终可见） ── */}
        <button onClick={() => setExpandedId(open ? null : p.id)} style={{
          display: 'flex', alignItems: 'center', gap: 12, width: '100%',
          padding: '13px 14px', border: 'none', background: 'transparent',
          cursor: 'pointer', color: 'var(--text-primary, #1a1a2e)',
        }}>
          <img
            src={`https://cdn.models.dev/icons/${p.id}.svg`} alt=""
            style={{ width: 30, height: 30, borderRadius: 7 }}
            onError={e => { (e.target as HTMLImageElement).style.display = 'none'; }}
          />
          <div style={{ flex: 1, textAlign: 'left' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 7 }}>
              <span style={{ fontWeight: 600, fontSize: 15 }}>{p.name}</span>
              {hasKey && <span style={{
                width: 8, height: 8, borderRadius: '50%', display: 'inline-block',
                background: ok === false ? '#e74c3c' : '#27ae60',
              }} />}
              {isLive && <span style={{
                fontSize: 10, padding: '1px 6px', borderRadius: 4,
                background: 'var(--accent-light, rgba(108,92,231,0.1))',
                color: 'var(--accent, #6C5CE7)', fontWeight: 600,
              }}>LIVE</span>}
            </div>
            <span style={{ fontSize: 12, color: 'var(--text-muted, #999)' }}>
              {models.length} 模型{hasKey ? ' · 已配置' : ''}
            </span>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
            {active && <Check size={16} style={{ color: 'var(--accent, #6C5CE7)' }} />}
            {open
              ? <ChevronDown size={16} style={{ color: '#aaa' }} />
              : <ChevronRight size={16} style={{ color: '#aaa' }} />}
          </div>
        </button>

        {/* ── 展开详情 ── */}
        {open && <div style={{ padding: '0 14px 14px', borderTop: '1px solid var(--border-light, rgba(0,0,0,0.05))' }}>
          {/* API Key */}
          <div style={{ marginBottom: 10 }}>
            <div style={{ fontSize: 11, color: 'var(--text-muted, #999)', marginBottom: 3 }}>{t('apiKey')} ({p.env_key})</div>
            <input type="password" placeholder="sk-..." value={apiKeys[p.id]||''}
              onChange={e => setApiKeys(prev => ({...prev, [p.id]: e.target.value}))}
              style={{ width: '100%', padding: '8px 10px', borderRadius: 8, border: '1px solid var(--border-light, rgba(0,0,0,0.1))', background: 'var(--bg-input, #f8f9fa)', color: 'var(--text-primary, #1a1a2e)', fontSize: 13, outline: 'none', boxSizing: 'border-box' }} />
          </div>
          {/* Base URL */}
          <div style={{ marginBottom: 10 }}>
            <div style={{ fontSize: 11, color: 'var(--text-muted, #999)', marginBottom: 3 }}>{t('baseUrl')} ({t('optional')})</div>
            <input type="text" placeholder={p.base_url} value={baseUrls[p.id]||''}
              onChange={e => setBaseUrls(prev => ({...prev, [p.id]: e.target.value}))}
              style={{ width: '100%', padding: '8px 10px', borderRadius: 8, border: '1px solid var(--border-light, rgba(0,0,0,0.1))', background: 'var(--bg-input, #f8f9fa)', color: 'var(--text-primary, #1a1a2e)', fontSize: 13, outline: 'none', boxSizing: 'border-box' }} />
          </div>
          {/* 操作按钮行 */}
          <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 12 }}>
            <button onClick={() => fetchLive(p)}
              disabled={fetching===p.id || !apiKeys[p.id]}
              style={{
                display: 'flex', alignItems: 'center', gap: 6,
                padding: '8px 14px', borderRadius: 8, border: 'none',
                background: apiKeys[p.id] ? 'var(--accent, #6C5CE7)' : 'var(--bg-tertiary, #e9ecef)',
                color: apiKeys[p.id] ? '#fff' : 'var(--text-muted, #999)',
                fontSize: 13, fontWeight: 600, cursor: 'pointer',
              }}>
              <RefreshCw size={14} className={fetching===p.id ? 'spinning' : ''} />
              {fetching===p.id ? t('fetching') : t('fetchLive')}
            </button>
            {ok===true  && <Check size={17} style={{ color: '#27ae60' }} />}
            {ok===false && <X size={17} style={{ color: '#e74c3c' }} />}
            {p.doc && <a href={p.doc} target="_blank" rel="noopener noreferrer"
              style={{ marginLeft: 'auto', color: 'var(--text-muted, #999)' }}><ExternalLink size={15} /></a>}
          </div>
          {/* 模型搜索 */}
          <div style={{ position: 'relative', marginBottom: 8 }}>
            <Search size={13} style={{ position: 'absolute', left: 10, top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted, #999)' }} />
            <input type="text" placeholder={t('searchModel')} value={modelSearch}
              onChange={e => setModelSearch(e.target.value)}
              style={{ width: '100%', padding: '7px 10px 7px 30px', borderRadius: 8, border: '1px solid var(--border-light, rgba(0,0,0,0.1))', background: 'var(--bg-input, #f8f9fa)', color: 'var(--text-primary, #1a1a2e)', fontSize: 12, outline: 'none', boxSizing: 'border-box' }} />
          </div>
          {/* 模型列表 */}
          <div style={{ maxHeight: 250, overflowY: 'auto', borderRadius: 8 }}>
            {models.map(m => {
              const sel = model?.model===m.id && model?.provider===p.id;
              return (
                <button key={m.id}
                  onClick={() => setModel(p.id, m.id, apiKeys[p.id]||'', baseUrls[p.id]||'')}
                  style={{
                    display: 'flex', alignItems: 'center', gap: 8,
                    width: '100%', padding: '8px 10px', border: 'none',
                    background: sel ? 'var(--accent-light, rgba(108,92,231,0.1))' : 'transparent',
                    color: sel ? 'var(--accent, #6C5CE7)' : 'var(--text-primary, #1a1a2e)',
                    cursor: 'pointer', fontSize: 13, textAlign: 'left', borderRadius: 6,
                  }}>
                  <span style={{ flex: 1 }}>{m.name || m.id}</span>
                  {m.context_length > 0 && <span style={{ fontSize: 11, color: 'var(--text-muted, #999)' }}>{fmtCtx(m.context_length)}</span>}
                  {m.cost_input > 0 && <span style={{ fontSize: 11, color: 'var(--text-muted, #999)' }}>{fmtCost(m.cost_input)}</span>}
                  {sel && <Check size={14} />}
                </button>
              );
            })}
            {!models.length && <div style={{ padding: 20, textAlign: 'center', color: 'var(--text-muted, #999)', fontSize: 13 }}>{t('noModels')}</div>}
          </div>
        </div>}
      </div>
    );
  };

  /* ── 主渲染 ── */
  return (
    <div className="flex-1 flex flex-col overflow-y-auto" style={{ minHeight: 0, background: 'var(--bg-primary, #f0f2f5)' }}>

      {/* ── 头部 ── */}
      <div style={{ padding: '16px 16px 6px', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <div>
          <div style={{ fontSize: 20, fontWeight: 700, color: 'var(--text-primary, #1a1a2e)' }}>{t('providers')}</div>
          <div style={{ fontSize: 12, color: 'var(--text-muted, #999)', marginTop: 1 }}>
            {providers.length} {t('providersCount')} · {totalModels} {t('modelsCount')}
          </div>
        </div>
        <button onClick={onDone} style={{
          padding: '6px 14px', borderRadius: 8, border: 'none',
          background: 'var(--bg-card, #fff)', color: 'var(--text-primary, #1a1a2e)',
          fontSize: 13, fontWeight: 500, cursor: 'pointer',
        }}>{t('done')}</button>
      </div>

      {/* ── 当前模型徽章 ── */}
      {model && <div style={{
        margin: '0 16px 8px', padding: '7px 12px', borderRadius: 10,
        background: 'var(--accent-light, rgba(108,92,231,0.1))',
        display: 'flex', alignItems: 'center', gap: 8,
      }}>
        <Check size={14} style={{ color: 'var(--accent, #6C5CE7)' }} />
        <span style={{ fontSize: 13, fontWeight: 500, color: 'var(--accent, #6C5CE7)' }}>
          {model.model}
        </span>
      </div>}

      {/* ── 新手引导（首次可见，点"知道了"关闭） ── */}
      {showGuide && <div style={{
        margin: '0 16px 8px', padding: '12px 14px', borderRadius: 12,
        background: 'var(--bg-card, #fff)',
        border: '1px solid var(--accent-light, rgba(108,92,231,0.2))',
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 8 }}>
          <Sparkles size={16} style={{ color: 'var(--accent, #6C5CE7)' }} />
          <span style={{ fontSize: 14, fontWeight: 600, color: 'var(--text-primary, #1a1a2e)' }}>快速开始</span>
        </div>
        <div style={{ fontSize: 13, color: 'var(--text-secondary, #555)', lineHeight: 1.7, marginBottom: 10 }}>
          ① 选一个供应商（推荐 NanoGPT，模型最多）<br />
          ② 填入你的 API 密钥<br />
          ③ 点击「获取实时模型」<br />
          ④ 选个模型，开始聊天！
        </div>
        <button onClick={dismissGuide} style={{
          padding: '6px 14px', borderRadius: 6, border: 'none',
          background: 'var(--accent, #6C5CE7)', color: '#fff',
          fontSize: 12, fontWeight: 600, cursor: 'pointer',
        }}>知道了</button>
      </div>}

      {/* ── 分类 Tab ── */}
      <div style={{
        display: 'flex', gap: 4, padding: '4px 16px 8px',
        overflowX: 'auto', WebkitOverflowScrolling: 'touch',
      }}>
        {CATEGORIES.map(c => (
          <button key={c.key} onClick={() => { setTab(c.key); setSearch(''); }}
            style={{
              padding: '6px 12px', borderRadius: 20, fontSize: 13, fontWeight: 500,
              whiteSpace: 'nowrap', flexShrink: 0, cursor: 'pointer',
              border: tab===c.key ? '1px solid var(--accent, #6C5CE7)' : '1px solid var(--border-light, rgba(0,0,0,0.08))',
              background: tab===c.key ? 'var(--accent, #6C5CE7)' : 'var(--bg-card, #fff)',
              color: tab===c.key ? '#fff' : 'var(--text-secondary, #555)',
            }}>
            {c.emoji} {c.label}
          </button>
        ))}
      </div>

      {/* ── 搜索栏 ── */}
      <div style={{ padding: '0 16px 8px', position: 'relative' }}>
        <Search size={14} style={{ position: 'absolute', left: 28, top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted, #999)' }} />
        <input type="text" placeholder={t('searchProviders')} value={search}
          onChange={e => setSearch(e.target.value)}
          style={{
            width: '100%', padding: '9px 12px 9px 36px', borderRadius: 10,
            border: '1px solid var(--border-light, rgba(0,0,0,0.08))',
            background: 'var(--bg-card, #fff)', color: 'var(--text-primary, #1a1a2e)',
            fontSize: 14, outline: 'none', boxSizing: 'border-box',
          }} />
      </div>

      {/* ── 供应商列表 ── */}
      <div style={{ flex: 1, overflowY: 'auto', padding: '0 16px 16px' }}>
        {filtered.map(Card)}
        {!filtered.length && <div style={{ textAlign: 'center', padding: 40, color: 'var(--text-muted, #999)' }}>
          <BookOpen size={32} style={{ marginBottom: 8, opacity: .5 }} />
          <div>{t('noMatch')}</div>
        </div>}
      </div>
    </div>
  );
}