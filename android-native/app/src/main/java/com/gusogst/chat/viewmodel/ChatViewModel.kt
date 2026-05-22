package com.gusogst.chat.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.gusogst.chat.data.ChatStore
import com.gusogst.chat.model.*
import com.gusogst.chat.network.ApiClient
import com.gusogst.chat.network.StreamProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChatViewModel(app: Application) : AndroidViewModel(app) {

    private val store = ChatStore.getInstance(app)
    private val streamProcessor = StreamProcessor()

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
        _messages.value = conv.messages
        store.saveActiveConversationId(id)
    }

    fun deleteConversation(id: String) {
        val list = _conversations.value.orEmpty().toMutableList()
        list.removeAll { it.id == id }
        _conversations.value = list
        if (_activeConversation.value?.id == id) {
            _activeConversation.value = null
            _messages.value = emptyList()
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
        // 找到最后一条 AI 消息并删除
        val idx = conv.messages.indexOfLast { it.id == msg.id }
        if (idx >= 0) {
            conv.messages.removeAt(idx)
            _messages.value = conv.messages.toList()
        }
        // 重新调用
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
        val provider = _providers.value?.firstOrNull { it.enabled } ?: return
        val apiKey = provider.apiKey
        val baseUrl = provider.baseUrl
        val model = conv.modelId ?: provider.models.firstOrNull() ?: return

        val systemMsg = conv.personaId?.let { pid ->
            _personas.value?.find { it.id == pid }?.prompt
        }?.let { ApiMessage("system", it) }

        val apiMessages = mutableListOf<ApiMessage>()
        if (systemMsg != null) apiMessages.add(systemMsg)
        conv.messages.filter { it.status != MessageStatus.error }.forEach {
            apiMessages.add(ApiMessage(it.role.name, it.content))
        }

        val request = ChatRequest(model = model, messages = apiMessages, stream = true)

        val aiMsg = Message(
            conversationId = conv.id,
            role = Role.assistant,
            content = "",
            status = MessageStatus.streaming,
            providerId = provider.id,
            modelId = model
        )
        conv.messages.add(aiMsg)
        _messages.value = conv.messages.toList()
        _isStreaming.value = true

        viewModelScope.launch {
            try {
                val service = ApiClient.getService(baseUrl)
                val response = withContext(Dispatchers.IO) {
                    service.chatCompletionsStream("Bearer $apiKey", request)
                }
                if (response.isSuccessful) {
                    val body = response.body() ?: return@launch
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
                            },
                            onError = { err ->
                                aiMsg.status = MessageStatus.error
                                aiMsg.content = "Error: $err"
                                _messages.postValue(conv.messages.toList())
                                _isStreaming.postValue(false)
                            }
                        )
                    }
                } else {
                    aiMsg.status = MessageStatus.error
                    aiMsg.content = "HTTP \${response.code()}: \${response.message()}"
                    _messages.value = conv.messages.toList()
                    _isStreaming.value = false
                }
            } catch (e: Exception) {
                aiMsg.status = MessageStatus.error
                aiMsg.content = "Error: \${e.message}"
                _messages.value = conv.messages.toList()
                _isStreaming.value = false
            }
        }
    }

    // ===== 设置 =====
    fun updateSettings(update: (UISettings) -> UISettings) {
        val new = update(_settings.value ?: UISettings())
        _settings.value = new
        store.saveSettings(new)
    }

    fun saveProviders(list: List<UIProvider>) {
        _providers.value = list
        store.saveProviders(list)
    }

    fun savePersonas(list: List<Persona>) {
        _personas.value = list
        store.savePersonas(list)
    }

    fun setActivePersona(id: String?) {
        _activeConversation.value?.personaId = id
        store.saveActivePersonaId(id ?: "")
    }
}
