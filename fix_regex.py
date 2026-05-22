#!/usr/bin/env python3
import sys
f = sys.argv[1] if len(sys.argv) > 1 else 'android-native/app/src/main/java/com/gusogst/chat/ui/chat/MarkdownRenderer.kt'
with open(f, 'r') as fp:
    lines = fp.readlines()

for i, line in enumerate(lines):
    if 'val boldPattern' in line:
        lines[i] = '        val boldPattern = Regex("""\\*\\*(.+?)\\*\\*""")\n'
        print(f'Fixed line {i+1}: {lines[i].rstrip()}')
    if 'val italicPattern' in line:
        lines[i] = '        val italicPattern = Regex("""(?<!\\*)\\*(?!\\*)(.+?)(?<!\\*)\\*(?!\\*)""")\n'
        print(f'Fixed line {i+1}: {lines[i].rstrip()}')

with open(f, 'w') as fp:
    fp.writelines(lines)
print('Done')
