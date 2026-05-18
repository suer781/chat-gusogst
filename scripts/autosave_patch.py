import re

FILE = '/data/data/com.termux/files/home/project/github.com/chat-gusogst/app/src/ui/persona/PersonaSettingsModal.tsx'
with open(FILE) as f:
    c = f.read()

# 1. Add states
old = '  const [overrideGlobal, setOverrideGlobal] = useState(persona.modelParamsConfig?.overrideGlobal ?? false);'
new = old + '\n  const [dirty, setDirty] = useState(false);\n  const [showConfirmClose, setShowConfirmClose] = useState(false);\n  const initialPersonaRef = React.useRef(persona);'
assert old in c, 'step1'
c = c.replace(old, new, 1)

# 2. Add autoSave + handleClose
old2 = '  const applyRulePreset'
new2 = '  const autoSave = (p: string, s: typeof sliders, og: boolean, am: AutoMode) => {\n    onSave({\n      systemPrompt: p,\n      modelParamsConfig: { autoMode: am, overrideGlobal: og,\n        temperature: clamp(s.temperature, 0, 2, 0.1),\n        topP: clamp(s.topP, 0, 1, 0.05),\n        maxTokens: Math.max(100, Math.round(s.maxTokens)),\n      },\n    });\n    setDirty(true);\n  };\n\n  const handleClose = () => {\n    if (dirty) { setShowConfirmClose(true); } else { onClose(); }\n  };\n\n  const applyRulePreset'
assert old2 in c, 'step2'
c = c.replace(old2, new2, 1)

# 3. Simplify save
old3 = '  const save = () => {\n    onSave({\n      systemPrompt: prompt,\n      modelParamsConfig: {\n        autoMode,\n        overrideGlobal,\n        temperature: clamp(sliders.temperature, 0, 2, 0.1),\n        topP: clamp(sliders.topP, 0, 1, 0.05),\n        maxTokens: Math.max(100, Math.round(sliders.maxTokens)),\n      },\n    });\n    onClose();\n  };'
new3 = '  const save = () => { onClose(); };'
assert old3 in c, 'step3'
c = c.replace(old3, new3, 1)

# 4. Textarea autoSave
old4 = 'onChange={e => setPrompt(e.target.value)}'
new4 = 'onChange={e => { const v = e.target.value; setPrompt(v); autoSave(v, sliders, overrideGlobal, autoMode); }}'
assert old4 in c, 'step4'
c = c.replace(old4, new4, 1)

# 5+6. Replace onClose with handleClose
for i in range(2):
    idx = c.find('onClick={onClose}')
    if idx != -1:
        c = c[:idx] + 'onClick={handleClose}' + c[idx + len('onClick={onClose}'):]'
        print(f'step{5+i}: replaced onClose')

# 7. Add confirm overlay
confirm = '''\n        {showConfirmClose && (\n          <div style={{ position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, background: 'rgba(0,0,0,0.6)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000 }} onClick={() => setShowConfirmClose(false)}>\n            <div style={{ background: 'var(--bg-secondary, #1e1e2e)', borderRadius: 16, padding: 24, width: '80%', maxWidth: 320, boxShadow: '0 8px 32px rgba(0,0,0,0.5)' }} onClick={(e: React.MouseEvent) => e.stopPropagation()}>\n              <div style={{ fontSize: 18, fontWeight: 700, color: 'var(--gray-100)', marginBottom: 8 }}>{'\u26a0\ufe0f \u9000\u51fa\u7f16\u8f91\uff1f'}</div>\n              <div style={{ fontSize: 14, color: 'var(--gray-400)', marginBottom: 20, lineHeight: 1.5 }}>{'\u5df2\u81ea\u52a8\u4fdd\u5b58\uff0c\u786e\u5b9a\u8981\u9000\u51fa\u5417\uff1f'}</div>\n              <div style={{ display: 'flex', gap: 10 }}>\n                <button onClick={() => setShowConfirmClose(false)} style={{ flex: 1, padding: 10, borderRadius: 10, border: '1px solid var(--border)', background: 'var(--bg-tertiary)', color: 'var(--gray-300)', fontSize: 14, cursor: 'pointer' }}>{'\u7ee7\u7eed\u7f16\u8f91'}</button>\n                <button onClick={onClose} style={{ flex: 1, padding: 10, borderRadius: 10, border: 'none', background: '#ff6b6b', color: '#fff', fontSize: 14, fontWeight: 600, cursor: 'pointer' }}>{'\u9000\u51fa'}</button>\n              </div>\n            </div>\n          </div>\n        )}\n'''
last_div = c.rfind('</div>')
c = c[:last_div] + confirm + '      </div>' + c[last_div + len('</div>'):]'
print('step7: confirm overlay added')

with open(FILE, 'w') as f:
    f.write(c)
print('ALL DONE!')