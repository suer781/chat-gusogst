import 'dart:async';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'ai_service.dart';
import 'models.dart';
import '../shared/services/database_service.dart';

/// Chat state
enum ChatState { idle, streaming, error }

/// Chat service — manages conversations, AI calls, and persistence
class ChatService extends StateNotifier<ChatState> {
  final AiService _ai = AiService();
  final List<Message> _messages = [];
  final StreamController<String> _streamController = StreamController.broadcast();

  String? _activeConversationId;
  Persona _activePersona = Persona.defaults.first;
  String _model = '';

  ChatService() : super(ChatState.idle);

  // Public getters
  List<Message> get messages => List.unmodifiable(_messages);
  Stream<String> get textStream => _streamController.stream;
  Persona get activePersona => _activePersona;

  /// Configure provider
  void configure(ProviderConfig config) {
    _ai.configure(config);
    _model = config.defaultModel;
  }

  /// Set active persona
  void setPersona(Persona persona) {
    _activePersona = persona;
  }

  /// Load conversation messages
  Future<void> loadConversation(String conversationId) async {
    _activeConversationId = conversationId;
    _messages.clear();
    _messages.addAll(await DatabaseService.getMessages(conversationId));
  }

  /// Start new conversation
  Future<String> newConversation() async {
    final conv = await DatabaseService.createConversation();
    _activeConversationId = conv.id;
    _messages.clear();
    return conv.id;
  }

  /// Send message and get AI response
  Future<void> sendMessage(String content) async {
    if (content.trim().isEmpty) return;
    if (state == ChatState.streaming) return;

    // Ensure conversation exists
    _activeConversationId ??= (await DatabaseService.createConversation()).id;

    // Add user message
    final userMsg = await DatabaseService.addMessage(
      conversationId: _activeConversationId!,
      role: 'user',
      content: content.trim(),
    );
    _messages.add(userMsg);

    // Start streaming
    state = ChatState.streaming;
    final buffer = StringBuffer();

    try {
      await for (final event in _ai.sendMessage(
        history: _messages,
        model: _model,
        systemPrompt: _activePersona.systemPrompt,
      )) {
        switch (event.type) {
          case 'text':
            buffer.write(event.content);
            _streamController.add(event.content);
            break;
          case 'done':
            // Save assistant message
            if (buffer.isNotEmpty) {
              final assistantMsg = await DatabaseService.addMessage(
                conversationId: _activeConversationId!,
                role: 'assistant',
                content: buffer.toString(),
              );
              _messages.add(assistantMsg);
            }
            state = ChatState.idle;
            break;
          case 'error':
            state = ChatState.error;
            // Add error as message
            _streamController.add('\n\n⚠️ \${event.content}');
            break;
        }
      }
    } catch (e) {
      state = ChatState.error;
      _streamController.add('\n\n⚠️ \${e.toString()}');
    }
  }

  /// Cancel current streaming
  void cancelStream() {
    _ai.cancel();
    state = ChatState.idle;
  }

  /// Clear error state
  void clearError() {
    if (state == ChatState.error) state = ChatState.idle;
  }

  @override
  void dispose() {
    _ai.dispose();
    _streamController.close();
    super.dispose();
  }
}

// Riverpod providers
final chatServiceProvider = StateNotifierProvider<ChatService, ChatState>((ref) {
  return ChatService();
});

final messagesProvider = Provider<List<Message>>((ref) {
  final service = ref.watch(chatServiceProvider.notifier);
  return service.messages;
});

final activePersonaProvider = Provider<Persona>((ref) {
  final service = ref.watch(chatServiceProvider.notifier);
  return service.activePersona;
});
