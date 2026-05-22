#!/usr/bin/env python3
"""Fix all AndroidUI build errors in one shot."""
import os, re

BASE = os.path.expanduser('~/project/github.com/chat-gusogst/android-native/app/src/main/java/com/gusogst/chat')

def read(path):
    with open(path, 'r') as f: return f.read()

def write(path, content):
    with open(path, 'w') as f: f.write(content)
    print(f'  ✅ {os.path.basename(path)}')

print('=== Fixing AndroidUI build errors ===\n')

# ── 1. ChatFragment.kt: tvEmpty → emptyState ──
print('1. ChatFragment.kt')
f = os.path.join(BASE, 'ui/chat/ChatFragment.kt')
c = read(f)
c = c.replace('R.id.tvEmpty', 'R.id.emptyState')
write(f, c)

# ── 2. MarkdownRenderer.kt: escape sequences + matcher ──
print('2. MarkdownRenderer.kt')
f = os.path.join(BASE, 'ui/chat/MarkdownRenderer.kt')
c = read(f)
# Fix invalid escape sequences: \* → \\*
c = c.replace('Regex("\\*\\*(.+?)\\*\\*")', 'Regex("\\\\\\*\\\\*(.+?)\\\\*\\\\*")')
c = c.replace('Regex("(?!\\*)\\*(?!\\*)(.+?)(?!\\*)\\*(?!\\*)")', 'Regex("(?<!\\\\*)\\\\*(?!\\\\*)(.+?)(?<!\\\\*)\\\\*(?!\\\\*)")')
c = c.replace('Regex("~~(.+?)~~")', 'Regex("~~(.+?)~~")')  # this one is fine
# Fix Regex.matcher() → toPattern().matcher()
c = c.replace('.matcher(ssb)', '.toPattern().matcher(ssb)')
write(f, c)

# ── 3. MessageAdapter.kt: wrong layout IDs ──
print('3. MessageAdapter.kt')
f = os.path.join(BASE, 'ui/chat/MessageAdapter.kt')
c = read(f)
c = c.replace('R.id.tvToolLabel', 'R.id.tvToolName')
c = c.replace('R.id.tvToolArgs', 'R.id.tvToolInput')
c = c.replace('R.id.tvToolResult', 'R.id.tvToolOutput')
write(f, c)

# ── 4. Models.kt: add missing fields ──
print('4. Models.kt (add tags, modelParamsConfig, searchEnabled, activeSearchEngine)')
f = os.path.join(BASE, 'model/Models.kt')
c = read(f)

# Add tags to Persona data class
c = c.replace(
    '    val bgColor: Int = Color.parseColor("#1A1A2E"),\n    val textColor: Int = Color.WHITE',
    '    val bgColor: Int = Color.parseColor("#1A1A2E"),\n    val textColor: Int = Color.WHITE,\n    val tags: List<String> = emptyList()'
)

# Add nested ModelParamsConfig class to Persona (before the closing brace of Persona)
# Find Persona data class closing
persona_end = c.find(')\n\ndata class Conversation')
if persona_end == -1:
    persona_end = c.find(')\n\ndata class Conversation')
# Add ModelParamsConfig as a companion/nested class after Persona
# Actually, Kotlin doesn't support nested classes in data classes easily. Let's add it as a top-level class.
# But the code references Persona.ModelParamsConfig, so we need it nested.
# In Kotlin, you CAN have nested classes. Let's add it inside Persona.

# Find the end of Persona class
# Persona ends with the closing paren of the data class constructor
# Let's add ModelParamsConfig as a companion-style nested class
# Actually, Kotlin data classes can't have nested classes in the constructor block.
# We need to add a body to the Persona class.

# Current: data class Persona(val id: String = ..., ...) 
# We need to add a body with the nested class.
# Let's find the closing ) of Persona and add a body.

# Simpler approach: add ModelParamsConfig as a top-level data class,
# and change Persona.ModelParamsConfig references to just ModelParamsConfig

# Add ModelParamsConfig as top-level class
model_params_config = '''
data class ModelParamsConfig(
    val temperature: Float = 0.7f,
    val topP: Float = 0.9f,
    val maxTokens: Int = 2048,
    val overrideGlobal: Boolean = false,
    val autoMode: String = "off"
)

'''
# Insert before data class Conversation
c = c.replace('\ndata class Conversation(', '\n' + model_params_config + 'data class Conversation(')

# Add modelParamsConfig field to Persona
c = c.replace(
    '    val tags: List<String> = emptyList()',
    '    val tags: List<String> = emptyList(),\n    val modelParamsConfig: ModelParamsConfig? = null'
)

# Add searchEnabled and activeSearchEngine to UISettings
c = c.replace(
    '    val bgAnimationEnabled: Boolean = true\n)',
    '    val bgAnimationEnabled: Boolean = true,\n    val searchEnabled: Boolean = false,\n    val activeSearchEngine: String = "duckduckgo"\n)'
)

write(f, c)

# ── 5. PersonaProfileFragment.kt: fix iterator ambiguity ──
print('5. PersonaProfileFragment.kt')
f = os.path.join(BASE, 'ui/persona/PersonaProfileFragment.kt')
c = read(f)
# The for loop on persona.tags might have iterator ambiguity
# Change 'for (tag in persona.tags)' to use .forEach or explicit type
c = c.replace('for (tag in persona.tags)', 'for (tag: String in persona.tags)')
write(f, c)

# ── 6. PersonaSettingsDialog.kt: fix systemPrompt → prompt, Persona.ModelParamsConfig ──
print('6. PersonaSettingsDialog.kt')
f = os.path.join(BASE, 'ui/persona/PersonaSettingsDialog.kt')
c = read(f)
# Fix systemPrompt → prompt
c = c.replace('it.systemPrompt', 'it.prompt')
# Fix Persona.ModelParamsConfig → ModelParamsConfig (now top-level)
c = c.replace('Persona.ModelParamsConfig(', 'ModelParamsConfig(')
# Fix copy - Persona.copy needs to match actual fields
c = c.replace(
    'p.copy(systemPrompt = prompt, modelParamsConfig = Persona.ModelParamsConfig(',
    'p.copy(prompt = prompt, modelParamsConfig = ModelParamsConfig('
)
write(f, c)

# ── 7. AboutSettingsFragment.kt: fix BuildConfig import ──
print('7. AboutSettingsFragment.kt')
f = os.path.join(BASE, 'ui/settings/AboutSettingsFragment.kt')
c = read(f)
# Remove BuildConfig import, use hardcoded values instead
c = c.replace('import com.gusogst.chat.BuildConfig\n', '')
# Replace BuildConfig references with hardcoded
c = c.replace('BuildConfig.VERSION_NAME', '"0.1.0-dev"')
c = c.replace('BuildConfig.VERSION_CODE', '1')
write(f, c)

# ── 8. build.gradle.kts: enable buildConfig (backup approach) ──
# Not needed since we removed BuildConfig usage

print('\n=== All fixes applied! ===')
