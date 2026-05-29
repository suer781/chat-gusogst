package com.gusogst.chat.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.gusogst.chat.data.ChatStore
import com.gusogst.chat.data.memory.MemoryManager
import com.gusogst.chat.model.*
import com.gusogst.chat.network.ApiClient
import com.gusogst.chat.network.AutoRetryEngine
import com.gusogst.chat.network.EndpointKB
import com.gusogst.chat.network.StreamProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChatViewModel(app: Application) : AndroidViewModel(app) {

    private val store = ChatStore.getInstance(app)
    private val streamProcessor = StreamProcessor()
    private val retryEngine = AutoRetryEngine()
    private val memoryManager = MemoryManager(app)

    private val _conversations = MutableLiveData<List<Conversation>>(emptyList())
    val conversations: LiveData<List<Conversation>> = _conversations

    private val _activeConversation = MutableLiveData<Conversation?>(null)
    val activeConversation: LiveData<Conversation?> = _activeConversation

    private val _messages = MutableLiveData<List<Message>>(emptyList())
    val messages: LiveData<List<Message>> = _messages

    private val _settings = MutableLiveData(UISettings())
    val settings: LiveData<UISettings> = _settings

    private val _providers = MutableLiveData<List<UIProvider>>(emptyList())
    val providers: LiveData<List<UIProvider>> = _providers

    private val _personas = MutableLiveData<List<Persona>>(emptyList())
    val personas: LiveData<List<Persona>> = _personas

    private val _isStreaming = MutableLiveData(false)
    val isStreaming: LiveData<Boolean> = _isStreaming

    init { loadAll() }

    private fun loadAll() {
        _conversations.value = store.loadConversations()
        _settings.value = store.loadSettings()
        _providers.value = store.loadProviders()
        _personas.value = store.loadPersonas()
        val activeId = store.loadActiveConversationId()
        val conv = _conversations.value?.find { it.id == activeId }
        if (conv != null) {
            _activeConversation.value = conv
            _messages.value = conv.messages
        }
    }

    // ===== 对话操作 =====
    fun createConversation() {
        val conv = Conversation()
        val list = _conversations.value.orEmpty().toMutableList()
        list.add(0, conv)
        _conversations.value = list
        _activeConversation.value = conv
        _messages.value = emptyList()
        store.saveConversations(list)
        store.saveActiveConversationId(conv.id)
    }

    fun selectConversation(id: String) {
        val conv = _conversations.value?.find { it.id == id } ?: return
        _activeConversation.value = conv
        _messages.value = conv.messages.toList()
        store.saveActiveConversationId(id)
    }

    fun deleteConversation(id: String) {
        val list = _conversations.value.orEmpty().toMutableList()
        list.removeAll { it.id == id }
        _conversations.value = list
        if (_activeConversation.value?.id == id) {
            _activeConversation.value = null
            _messages.value = emptyList()
            store.saveActiveConversationId("")
        }
        store.saveConversations(list)
    }

    // ===== 消息操作 =====
    fun deleteMessage(messageId: String) {
        val conv = _activeConversation.value ?: return
        conv.messages.removeAll { it.id == messageId }
        _messages.value = conv.messages.toList()
        store.saveConversations(_conversations.value.orEmpty())
    }

    fun regenerate(msg: Message) {
        val conv = _activeConversation.value ?: return
        val idx = conv.messages.indexOfLast { it.id == msg.id }
        if (idx >= 0) {
            conv.messages.removeAt(idx)
            _messages.value = conv.messages.toList()
        }
        callAiApi(conv)
        store.saveConversations(_conversations.value.orEmpty())
    }

    // ===== 发送消息 =====
    fun sendMessage(content: String) {
        val conv = _activeConversation.value ?: run {
            createConversation()
            _activeConversation.value ?: return
        }
        val userMsg = Message(conversationId = conv.id, role = Role.user, content = content)
        conv.messages.add(userMsg)
        conv.updatedAt = System.currentTimeMillis()
        _messages.value = conv.messages.toList()
        if (conv.messages.size == 1) {
            conv.title = content.take(20)
            _conversations.value = _conversations.value
        }
        callAiApi(conv)
        store.saveConversations(_conversations.value.orEmpty())
    }

    private fun callAiApi(conv: Conversation) {
        val providers = _providers.value.orEmpty().filter { it.enabled }
        val primary = providers.firstOrNull() ?: return
        val model = conv.modelId ?: primary.models.firstOrNull()?.id ?: return

        val systemMsg = conv.personaId?.let { pid ->
            _personas.value?.find { it.id == pid }?.prompt
        }?.let { ApiMessage(role = "system", content = it) }

        val apiMessages = mutableListOf<ApiMessage>()
        if (systemMsg != null) apiMessages.add(systemMsg)

        // 注入记忆上下文（基于用户最近一条消息检索）
        val lastUserMsg = conv.messages.lastOrNull { it.role == Role.user }
        if (lastUserMsg != null) {
            val memoryContext = memoryManager.getContextStrings(lastUserMsg.content, 5)
            if (memoryContext.isNotEmpty()) {
                apiMessages.add(ApiMessage(
                    role = "system",
                    content = "以下是与用户相关的记忆，可用于个性化回复：\n" +
                        memoryContext.joinToString("\n")
                ))
            }
        }

        conv.messages.filter { it.status != MessageStatus.error }.forEach {
            apiMessages.add(ApiMessage(role = it.role.name, content = it.content))
        }

        val request = ChatRequest(model = model, messages = apiMessages, stream = true)

        val aiMsg = Message(
            conversationId = conv.id,
            role = Role.assistant,
            content = "",
            status = MessageStatus.streaming,
            providerId = primary.id,
            modelId = model
        )
        conv.messages.add(aiMsg)
        _messages.value = conv.messages.toList()
        _isStreaming.value = true

        // 用 RAG 推断路径：如果用户只填了域名没填路径，自动补全
        val resolvedUrl = resolveEndpointPath(primary.baseUrl)

        viewModelScope.launch {
            // 1) 尝试主端点
            var success = tryEndpoint(
                baseUrl = resolvedUrl,
                apiKey = primary.apiKey,
                request = request,
                aiMsg = aiMsg,
                conv = conv,
                providerId = primary.id,
                userContent = lastUserMsg?.content
            )

            // 2) 主端点失败 → 故障转移
            if (!success) {
                val failedUrl = primary.baseUrl
                val candidates = retryEngine.findFallbacks(failedUrl, providers)

                for (candidate in candidates) {
                    if (_isStreaming.value == false) break

                    val fallbackUrl = resolveEndpointPath(candidate.provider.baseUrl)

                    aiMsg.content += "\n[自动切换: ${candidate.provider.name} → ${candidate.kbEntry.path}]"

                    success = tryEndpoint(
                        baseUrl = fallbackUrl,
                        apiKey = candidate.provider.apiKey,
                        request = request,
                        aiMsg = aiMsg,
                        conv = conv,
                        providerId = candidate.provider.id
                    )

                    if (success) {
                        aiMsg.providerId = candidate.provider.id
                        break
                    }
                }
            }

            // 3) 全部失败
            if (!success) {
                aiMsg.status = MessageStatus.error
                if (aiMsg.content.isBlank()) {
                    aiMsg.content = "所有可用端点均已失败，请检查网络或供应商配置"
                }
                _messages.value = conv.messages.toList()
                _isStreaming.value = false
                store.saveConversations(_conversations.value.orEmpty())
            }
        }
    }

    private suspend fun tryEndpoint(
        baseUrl: String,
        apiKey: String,
        request: ChatRequest,
        aiMsg: Message,
        conv: Conversation,
        providerId: String,
        userContent: String? = null
    ): Boolean {
        return try {
            val service = ApiClient.getService(baseUrl)
            val response = withContext(Dispatchers.IO) {
                service.chatCompletionsStream("Bearer $apiKey", request)
            }

            if (response.isSuccessful) {
                val body = response.body() ?: return false
                var completed = false
                withContext(Dispatchers.IO) {
                    streamProcessor.processStream(
                        responseBody = body,
                        onThinking = { chunk ->
                            aiMsg.thinking = (aiMsg.thinking ?: "") + chunk
                            _messages.postValue(conv.messages.toList())
                        },
                        onContent = { chunk ->
                            aiMsg.content += chunk
                            _messages.postValue(conv.messages.toList())
                        },
                        onComplete = {
                            aiMsg.status = MessageStatus.ready
                            conv.updatedAt = System.currentTimeMillis()
                            _messages.postValue(conv.messages.toList())
                            _isStreaming.postValue(false)
                            store.saveConversations(_conversations.value.orEmpty())
                            completed = true
                            retryEngine.recordSuccess(baseUrl, apiKey)
                            // 自动提取记忆（基于关键字规则，不额外调用 API）
                            if (aiMsg.content.isNotBlank() && userContent != null) {
                                autoExtractMemories(userContent, aiMsg.content)
                            }
                        },
                        onError = { err ->
                            aiMsg.content += "\n[流错误: $err]"
                            _messages.postValue(conv.messages.toList())
                            completed = false
                            retryEngine.recordFailure(baseUrl, apiKey)
                        }
                    )
                }
                completed
            } else {
                retryEngine.recordFailure(baseUrl, apiKey)
                aiMsg.content += "\n[HTTP ${response.code()}]"
                _messages.postValue(conv.messages.toList())
                false
            }
        } catch (e: Exception) {
            retryEngine.recordFailure(baseUrl, apiKey)
            aiMsg.content += "\n[连接失败: ${e.message?.take(60)}]"
            _messages.postValue(conv.messages.toList())
            false
        }
    }

    /**
     * 解析端点路径：如果 baseUrl 末尾没有路径段（如 /v1），
     * 用 EndpointKB 推断路径并附加。
     */
    private fun resolveEndpointPath(baseUrl: String): String {
        val trimmed = baseUrl.trimEnd('/')
        if (trimmed.contains("//")) {
            val pathStart = trimmed.indexOf('/', trimmed.indexOf("://") + 3)
            if (pathStart > 0 && pathStart < trimmed.length - 1) {
                return baseUrl
            }
        }

        val domain = try {
            val start = if (trimmed.startsWith("http://") || trimmed.startsWith("https://"))
                trimmed.indexOf("://") + 3 else 0
            val end = trimmed.indexOf('/', start).let { if (it == -1) trimmed.length else it }
            trimmed.substring(start, end)
        } catch (_: Exception) { trimmed }

        val kbResult = EndpointKB.inferPath(domain)
        return if (kbResult != null) {
            val (path, _, _) = kbResult
            trimmed + path
        } else {
            baseUrl
        }
    }

    /**
     * 自动提取记忆（基于关键字规则，不额外调用 API）
     * 从对话中提取偏好、事实等值得记住的信息。
     * 每次 AI 回复后调用，最多提取 3 条。
     */
    private fun autoExtractMemories(userContent: String, aiContent: String) {
        // 偏好提取：检测"I like/love/enjoy/hate/don't like"等模式
        val preferencePatterns = listOf(
            Regex("我喜欢([^，。！？]{2,30})", setOf(RegexOption.IGNORE_CASE)),
            Regex("我不喜欢([^，。！？]{2,30})", setOf(RegexOption.IGNORE_CASE)),
            Regex("我(?:最爱|讨厌|偏爱|喜欢用)([^，。！？]{2,30})", setOf(RegexOption.IGNORE_CASE))
        )

        for (pattern in preferencePatterns) {
            val match = pattern.find(userContent)?.groupValues?.getOrNull(1)
            if (match != null && match.length >= 2) {
                memoryManager.addMemory(
                    type = "preference",
                    content = "用户$match",
                    source = "conversation"
                )
            }
        }

        // 事实提取：检测用户自我介绍或给出个人信息
        val factPatterns = listOf(
            Regex("我(?:叫|是|在|住在|做|有)([^，。！？]{2,30})", setOf(RegexOption.IGNORE_CASE))
        )
        for (pattern in factPatterns) {
            val match = pattern.find(userContent)?.groupValues?.getOrNull(1)
            if (match != null && match.length >= 2) {
                memoryManager.addMemory(
                    type = "fact",
                    content = "用户${match}",
                    source = "conversation"
                )
            }
        }

        // 计划提取：检测用户提到计划或目标
        val planPattern = Regex("我(?:要|打算|计划|想|准备)([^，。！？]{3,30})", setOf(RegexOption.IGNORE_CASE))
        val planMatch = planPattern.find(userContent)?.groupValues?.getOrNull(1)
        if (planMatch != null && planMatch.length >= 3) {
            memoryManager.addMemory(
                type = "plan",
                content = "用户想$planMatch",
                source = "conversation"
            )
        }
    }

    // ===== 设置 =====
    fun updateSettings(update: (UISettings) -> UISettings) {
        val new = update(_settings.value ?: UISettings())
        _settings.value = new
        store.saveSettings(new)
    }

    
    /**
     * 设置当前对话的模型。
     * 更新对话的 providerId + modelId 并持久化。
     */
    fun setModel(providerId: String, modelId: String) {
        val conv = _activeConversation.value ?: return
        conv.providerId = providerId
        conv.modelId = modelId
        conv.updatedAt = System.currentTimeMillis()
        _activeConversation.value = conv
        store.saveConversations(_conversations.value.orEmpty())
    }

    fun saveProviders(list: List<UIProvider>) {
        _providers.value = list
        store.saveProviders(list)
    }

    fun savePersonas(list: List<Persona>) {
        _personas.value = list
        store.savePersonas(list)
    }

    fun updatePersona(personaId: String, update: (Persona) -> Persona) {
        val list = _personas.value.orEmpty().toMutableList()
        val idx = list.indexOfFirst { it.id == personaId }
        if (idx >= 0) {
            list[idx] = update(list[idx])
            _personas.value = list
            store.savePersonas(list)
        }
    }

    fun setActivePersona(id: String?) {
        _activeConversation.value?.personaId = id
        _activeConversation.value = _activeConversation.value
        store.saveActivePersonaId(id ?: "")
        store.saveConversations(_conversations.value.orEmpty())
    }
}