import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter/services.dart';
import 'package:go_router/go_router.dart';
import '../../agent/chat_service.dart';
import '../../agent/models.dart';
import '../../shared/widgets/glass_card.dart';
import 'widgets/message_bubble.dart';
import 'widgets/chat_input.dart';
import 'widgets/typing_indicator.dart';

class ChatPage extends ConsumerStatefulWidget {
  const ChatPage({super.key});

  @override
  ConsumerState<ChatPage> createState() => _ChatPageState();
}

class _ChatPageState extends ConsumerState<ChatPage> {
  final _scrollController = ScrollController();
  final List<String> _streamingBuffer = [];
  bool _isStreaming = false;

  @override
  void dispose() {
    _scrollController.dispose();
    super.dispose();
  }

  void _scrollToBottom() {
    if (_scrollController.hasClients) {
      Future.delayed(const Duration(milliseconds: 50), () {
        _scrollController.animateTo(
          _scrollController.position.maxScrollExtent,
          duration: const Duration(milliseconds: 200),
          curve: Curves.easeOutCubic,
        );
      });
    }
  }

  void _sendMessage(String text) {
    if (text.trim().isEmpty) return;

    HapticFeedback.lightImpact();
    final service = ref.read(chatServiceProvider.notifier);

    setState(() => _isStreaming = true);
    _streamingBuffer.clear();

    // Listen to text stream
    final sub = service.textStream.listen((chunk) {
      setState(() {
        _streamingBuffer.add(chunk);
      });
      _scrollToBottom();
    }, onDone: () {
      setState(() => _isStreaming = false);
      _scrollToBottom();
    }, onError: (_) {
      setState(() => _isStreaming = false);
    });

    // Send message (async)
    service.sendMessage(text);
    _scrollToBottom();
  }

  @override
  Widget build(BuildContext context) {
    final messages = ref.watch(messagesProvider);
    final persona = ref.watch(activePersonaProvider);
    final chatState = ref.watch(chatServiceProvider);
    final colorScheme = Theme.of(context).colorScheme;
    final hasError = chatState == ChatState.error;

    return Scaffold(
      appBar: AppBar(
        title: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(persona.avatarEmoji ?? '💕', style: const TextStyle(fontSize: 20)),
            const SizedBox(width: 8),
            Text(persona.name),
          ],
        ),
        actions: [
          IconButton(
            icon: const Icon(Icons.add_circle_outline),
            onPressed: () async {
              final service = ref.read(chatServiceProvider.notifier);
              await service.newConversation();
              setState(() {});
            },
          ),
          IconButton(
            icon: const Icon(Icons.tune),
            onPressed: () => context.go('/settings'),
          ),
        ],
      ),
      body: Column(
        children: [
          // Error banner
          if (hasError)
            Container(
              width: double.infinity,
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
              color: colorScheme.errorContainer,
              child: Row(
                children: [
                  Icon(Icons.error_outline, size: 18, color: colorScheme.error),
                  const SizedBox(width: 8),
                  Expanded(
                    child: Text(
                      'AI response failed. Tap retry or send again.',
                      style: TextStyle(color: colorScheme.onErrorContainer, fontSize: 13),
                    ),
                  ),
                  TextButton(
                    onPressed: () => ref.read(chatServiceProvider.notifier).clearError(),
                    child: const Text('Dismiss'),
                  ),
                ],
              ),
            ),
          // Messages list
          Expanded(
            child: messages.isEmpty && !_isStreaming
                ? _buildEmptyState(colorScheme, persona)
                : ListView.builder(
                    controller: _scrollController,
                    padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                    itemCount: messages.length + (_isStreaming ? 1 : 0),
                    itemBuilder: (context, index) {
                      if (index == messages.length && _isStreaming) {
                        return Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            const TypingIndicator(),
                            if (_streamingBuffer.isNotEmpty)
                              Padding(
                                padding: const EdgeInsets.only(left: 4, top: 4),
                                child: Text(
                                  _streamingBuffer.join(),
                                  style: TextStyle(
                                    fontSize: 15,
                                    height: 1.4,
                                    color: colorScheme.onSurface,
                                  ),
                                ),
                              ),
                          ],
                        );
                      }
                      return MessageBubble(message: messages[index]);
                    },
                  ),
          ),
          // Input
          ChatInput(onSend: _sendMessage),
        ],
      ),
    );
  }

  Widget _buildEmptyState(ColorScheme colorScheme, Persona persona) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(32),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            GlassCard(
              showShine: true,
              padding: const EdgeInsets.all(28),
              child: Column(
                children: [
                  Text(
                    persona.avatarEmoji ?? '💕',
                    style: const TextStyle(fontSize: 48),
                  ),
                  const SizedBox(height: 16),
                  Text(
                    'Hi! I\'m \${persona.name}',
                    style: Theme.of(context).textTheme.titleMedium,
                  ),
                  const SizedBox(height: 8),
                  Text(
                    'Say something to start chatting',
                    style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                      color: colorScheme.onSurfaceVariant,
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}
