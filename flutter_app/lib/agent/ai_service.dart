import 'dart:async';
import 'dart:convert';
import 'package:dio/dio.dart';
import 'models.dart';
import 'tools.dart';

/// SSE stream event
class StreamEvent {
  final String type; // 'text', 'tool_call', 'done', 'error'
  final String content;
  final String? toolName;
  final Map<String, dynamic>? toolArgs;

  const StreamEvent({required this.type, required this.content, this.toolName, this.toolArgs});
}

/// AI Service — handles OpenAI-compatible API calls with SSE streaming + tool calls
class AiService {
  late final Dio _dio;
  ProviderConfig? _config;
  CancelToken? _currentCancel;
  final ToolRegistry _tools = ToolRegistry();

  AiService() {
    _dio = Dio(BaseOptions(
      connectTimeout: const Duration(seconds: 30),
      receiveTimeout: const Duration(minutes: 5),
    ));
  }

  void configure(ProviderConfig config) {
    _config = config;
    _dio.options.baseUrl = config.baseUrl;
    _dio.options.headers = {
      'Content-Type': 'application/json',
      if (config.apiKey.isNotEmpty) 'Authorization': 'Bearer ${config.apiKey}',
      if (config.type == 'anthropic') 'x-api-key': config.apiKey,
      if (config.type == 'anthropic') 'anthropic-version': '2023-06-01',
    };
  }

  ToolRegistry get tools => _tools;

  /// Send message with SSE streaming + tool call loop
  Stream<StreamEvent> sendMessage({
    required List<Message> history,
    required String model,
    String? systemPrompt,
  }) async* {
    if (_config == null) {
      yield const StreamEvent(type: 'error', content: 'Provider not configured');
      return;
    }

    _currentCancel = CancelToken();

    // Build messages array
    final messages = <Map<String, dynamic>>[];
    if (systemPrompt != null && systemPrompt.isNotEmpty) {
      messages.add({'role': 'system', 'content': systemPrompt});
    }
    for (final msg in history) {
      messages.add({'role': msg.role, 'content': msg.content});
    }

    try {
      final requestData = <String, dynamic>{
        'model': model,
        'messages': messages,
        'stream': true,
      };

      // Add tools if available
      if (_tools.hasTools) {
        requestData['tools'] = _tools.toJsonList();
      }

      final response = await _dio.post(
        _getChatEndpoint(),
        data: requestData,
        options: Options(
          responseType: ResponseType.stream,
          receiveTimeout: const Duration(minutes: 5),
        ),
        cancelToken: _currentCancel,
      );

      // Parse SSE stream
      String buffer = '';
      final toolCallsBuffer = <int, Map<String, dynamic>>{};

      await for (final chunk in response.data.stream) {
        buffer += utf8.decode(chunk);
        while (buffer.contains('\n')) {
          final index = buffer.indexOf('\n');
          final line = buffer.substring(0, index).trim();
          buffer = buffer.substring(index + 1);

          if (line.startsWith('data: ')) {
            final data = line.substring(6);
            if (data == '[DONE]') {
              // Process any buffered tool calls
              if (toolCallsBuffer.isNotEmpty) {
                for (final tc in toolCallsBuffer.values) {
                  final name = tc['function']?['name'] as String? ?? '';
                  final argsStr = tc['function']?['arguments'] as String? ?? '{}';
                  Map<String, dynamic> args;
                  try { args = jsonDecode(argsStr); } catch (_) { args = {}; }

                  yield StreamEvent(type: 'tool_call', content: 'Calling $name...', toolName: name, toolArgs: args);

                  // Execute tool
                  final result = await _tools.execute(name, args);
                  messages.add({'role': 'assistant', 'tool_calls': [tc]});
                  messages.add({'role': 'tool', 'tool_call_id': tc['id'], 'content': result.content});
                }
                toolCallsBuffer.clear();

                // Recursive call for tool response
                yield* _continueAfterTools(messages, model);
              }
              yield const StreamEvent(type: 'done', content: '');
              return;
            }

            try {
              final json = jsonDecode(data) as Map<String, dynamic>;
              final choices = json['choices'] as List?;
              if (choices != null && choices.isNotEmpty) {
                final delta = choices[0]['delta'] as Map<String, dynamic>?;

                // Text content
                final content = delta?['content'] as String?;
                if (content != null && content.isNotEmpty) {
                  yield StreamEvent(type: 'text', content: content);
                }

                // Tool calls
                final toolCalls = delta?['tool_calls'] as List?;
                if (toolCalls != null) {
                  for (final tc in toolCalls) {
                    final idx = tc['index'] as int? ?? 0;
                    toolCallsBuffer.putIfAbsent(idx, () => {
                      'id': tc['id'] ?? '',
                      'type': 'function',
                      'function': {'name': '', 'arguments': ''},
                    });
                    if (tc['id'] != null) toolCallsBuffer[idx]!['id'] = tc['id'];
                    if (tc['function']?['name'] != null) {
                      toolCallsBuffer[idx]!['function']!['name'] = tc['function']['name'];
                    }
                    if (tc['function']?['arguments'] != null) {
                      toolCallsBuffer[idx]!['function']!['arguments'] =
                          (toolCallsBuffer[idx]!['function']!['arguments'] as String) + tc['function']['arguments'];
                    }
                  }
                }
              }
            } catch (_) {}
          }
        }
      }
      yield const StreamEvent(type: 'done', content: '');
    } on DioException catch (e) {
      if (e.type == DioExceptionType.cancel) {
        yield const StreamEvent(type: 'done', content: '');
      } else {
        yield StreamEvent(type: 'error', content: _formatError(e));
      }
    } catch (e) {
      yield StreamEvent(type: 'error', content: e.toString());
    }
  }

  /// Continue after tool execution
  Stream<StreamEvent> _continueAfterTools(List<Map<String, dynamic>> messages, String model) async* {
    try {
      final response = await _dio.post(
        _getChatEndpoint(),
        data: {'model': model, 'messages': messages, 'stream': true},
        options: Options(responseType: ResponseType.stream, receiveTimeout: const Duration(minutes: 5)),
        cancelToken: _currentCancel,
      );

      String buffer = '';
      await for (final chunk in response.data.stream) {
        buffer += utf8.decode(chunk);
        while (buffer.contains('\n')) {
          final index = buffer.indexOf('\n');
          final line = buffer.substring(0, index).trim();
          buffer = buffer.substring(index + 1);
          if (line.startsWith('data: ')) {
            final data = line.substring(6);
            if (data == '[DONE]') return;
            try {
              final json = jsonDecode(data) as Map<String, dynamic>;
              final choices = json['choices'] as List?;
              if (choices != null && choices.isNotEmpty) {
                final delta = choices[0]['delta'] as Map<String, dynamic>?;
                final content = delta?['content'] as String?;
                if (content != null && content.isNotEmpty) {
                  yield StreamEvent(type: 'text', content: content);
                }
              }
            } catch (_) {}
          }
        }
      }
    } catch (_) {}
  }

  void cancel() { _currentCancel?.cancel(); }

  String _getChatEndpoint() {
    if (_config?.type == 'anthropic') return '/messages';
    return '/chat/completions';
  }

  String _formatError(DioException e) {
    switch (e.type) {
      case DioExceptionType.connectionTimeout:
      case DioExceptionType.sendTimeout:
      case DioExceptionType.receiveTimeout:
        return 'Connection timed out';
      case DioExceptionType.connectionError:
        return 'Connection failed';
      case DioExceptionType.badResponse:
        return 'Server error (${e.response?.statusCode})';
      default:
        return 'Request failed: ${e.message}';
    }
  }

  void dispose() { cancel(); _dio.close(); _tools.dispose(); }
}
