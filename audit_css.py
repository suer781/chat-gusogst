#!/usr/bin/env python3
import re, os, glob

CSS_DIR = 'app/src/ui'

def read(f):
    with open(os.path.join(CSS_DIR, f)) as fh:
        return fh.read()

# === 1. tailwind.css :root analysis ===
tw = read('tailwind.css')
root_match = re.search(r':root\s*\{([^}]+)\}', tw, re.DOTALL)
root_vars = set()
if root_match:
    root_vars = set(re.findall(r'--([a-zA-Z0-9_-]+)', root_match.group(1)))
    print(f':root 声明了 {len(root_vars)} 个变量')

# Find all data-theme declarations
tw_declared = set(re.findall(r'--([a-zA-Z0-9_-]+):', tw))
tw_used = set(re.findall(r'var\(--([a-zA-Z0-9_-]+)', tw))
missing_root = tw_used - root_vars
print(f'tailwind.css: var() 使用 {len(tw_used)} 个, 缺少 :root 默认值 {len(missing_root)} 个')
if missing_root:
    for v in sorted(missing_root):
        print(f'  --{v}')

# === 2. Other CSS files ===
for css in ['hdr_v3.css', 'material_you.css']:
    c = read(css)
    local_d = set(re.findall(r'--([a-zA-Z0-9_-]+):', c))
    local_u = set(re.findall(r'var\(--([a-zA-Z0-9_-]+)', c))
    unresolved = local_u - root_vars - local_d
    print(f'\n{css}: 声明 {len(local_d)}, 使用 {len(local_u)}, 依赖外部 {len(local_u - local_d)}')
    if unresolved:
        print(f'  未在 :root 声明的: {len(unresolved)} 个')
        for v in sorted(unresolved):
            print(f'    --{v}')

# === 3. JS setting CSS vars without CSS declaration ===
print('\n=== JS 设置但可能缺少 CSS 声明的变量 ===')
all_css_declared = root_vars | tw_declared
for root, dirs, fnames in os.walk('app/src'):
    for fn in fnames:
        if fn.endswith(('.tsx', '.ts')):
            fp = os.path.join(root, fn)
            with open(fp) as f:
                c = f.read()
            js_vars = set(re.findall(r'["\'](--[a-zA-Z0-9_-]+)["\']\s*:', c))
            # Also check setProperty
            js_vars |= set(re.findall(r'setProperty\(["\']--["\']\s*\+\s*["\']([a-zA-Z0-9_-]+)', c))
            js_vars |= set(re.findall(r'setProperty\(["\'](--[a-zA-Z0-9_-]+)["\']', c))
            js_only = js_vars - {f'--{v}' for v in all_css_declared}
            # Actually js_vars has -- prefix already
            js_only = set()
            for jv in js_vars:
                varname = jv.lstrip('-') if jv.startswith('--') else jv
                if varname not in all_css_declared:
                    js_only.add(jv)
            if js_only:
                rel = os.path.relpath(fp, 'app/src')
                print(f'  {rel}: {len(js_only)} 个')
                for v in sorted(js_only):
                    print(f'    {v}')

# === 4. Total summary ===
print(f'\n=== 汇总 ===')
print(f':root 默认值: {len(root_vars)} 个')
print(f'tailwind.css 全部声明(含theme): {len(tw_declared)} 个')
print(f'tailwind.css 使用: {len(tw_used)} 个')
print(f'缺少 :root 默认值: {len(missing_root)} 个')
