f = 'android-native/app/src/main/java/com/gusogst/chat/data/AgentBridge.kt'
with open(f, 'r') as fp:
    c = fp.read()

# Fix 1: Replace settings.modelName/temperature/topP/maxTokens with provider model + defaults
old = '''        return AgentConfig(
            apiKey = activeProvider?.apiKey ?: "",
            baseUrl = activeProvider?.baseUrl ?: "https://api.openai.com/v1",
            model = settings.modelName.ifEmpty { "gpt-4o-mini" },
            temperature = settings.temperature,
            topP = settings.topP,
            maxTokens = settings.maxTokens,
            systemPrompt = systemPrompt
        )'''
new = '''        return AgentConfig(
            apiKey = activeProvider?.apiKey ?: "",
            baseUrl = activeProvider?.baseUrl ?: "https://api.openai.com/v1",
            model = activeProvider?.models?.firstOrNull() ?: "gpt-4o-mini",
            systemPrompt = systemPrompt
        )'''
c = c.replace(old, new)

# Fix 2: Remove duplicate NOTE comments, keep only one
old2 = '''    // ⚠️ UIMessage 是 Models.kt 里 Message 的 typealias，别改回 Message（会跟本地 Message 冲突）
    // ⚠️ UIMessage 是 Models.kt 里 Message 的 typealias，别改回 Message（会跟本地 Message 冲突）
// [NOTE] UIMessage 是 Models.kt 里 Message 的 typealias，不能直接用 Message（会跟本地 data class Message 冲突）'''
new2 = '''    // [NOTE] UIMessage 是 Models.kt 里 Message 的 typealias，不能直接用 Message（会跟本地 data class Message 冲突）'''
c = c.replace(old2, new2)

# Fix 3: msg.role is Role enum, need .name to convert to String
c = c.replace('role = msg.role,', 'role = msg.role.name,')

with open(f, 'w') as fp:
    fp.write(c)
print('AgentBridge.kt fixed')
