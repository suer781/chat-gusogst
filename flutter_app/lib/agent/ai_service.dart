import 'dart:async';
import 'dart:convert';
import 'package:dio/dio.dart';
import 'models.dart';

/// SSE stream event
class StreamEvent {
  final String type; // 'text', 'done', 'error'
  final String content;

  const StreamEvent({required this.type, required this.content});
}

/// AI Service — handles OpenAI-compatible API calls with SSE streaming
class AiService {
  late final Dio _dio;
  ProviderConfig? _config;
  CancelToken? _currentCancel;

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
      if (config.apiKey.isNotEmpty)
        'Authorization': 'Bearer \${config.apiKey}',
      if (config.type == 'anthropic')
        'x-api-key': config.apiKey,
      if (config.type == 'anthropic')
        'anthropic-version': '2023-06-01',
    };
  }

  /// Send message with SSE streaming
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
    final messages = <Map<String, String>>[];
    if (systemPrompt != null && systemPrompt.isNotEmpty) {
      messages.add({'role': 'system', 'content': systemPrompt});
    }
    for (final msg in history) {
      messages.add({'role': msg.role, 'content': msg.content});
    }

    try {
      final response = await _dio.post(
        _getChatEndpoint(),
        data: {
          'model': model,
          'messages': messages,
          'stream': true,
        },
        options: Options(
          responseType: ResponseType.stream,
          receiveTimeout: const Duration(minutes: 5),
        ),
        cancelToken: _currentCancel,
      );

      // Parse SSE stream
      String buffer = '';
      await for (final chunk in response.data.stream) {
        buffer += utf8.decode(chunk);
        // Process complete lines
        while (buffer.contains('\n')) {
          final index = buffer.indexOf('\n');
          final line = buffer.substring(0, index).trim();
          buffer = buffer.substring(index + 1);

          if (line.startsWith('data: ')) {
            final data = line.substring(6);
            if (data == '[DONE]') {
              yield const StreamEvent(type: 'done', content: '');
              return;
            }

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
            } catch (_) {
              // Skip malformed JSON lines
            }
          }
        }
      }

      yield const StreamEvent(type: 'done', content: '');
    } on DioException catch (e) {
      if (e.type == DioExceptionType.cancel) {
        yield const StreamEvent(type: 'done', content: '');
      } else {
        yield StreamEvent(
          type: 'error',
          content: _formatError(e),
        );
      }
    } catch (e) {
      yield StreamEvent(type: 'error', content: e.toString());
    }
  }

  /// Cancel current streaming
  void cancel() {
    _currentCancel?.cancel();
  }

  String _getChatEndpoint() {
    if (_config?.type == 'anthropic') {
      return '/messages';
    }
    return '/chat/completions';
  }

  String _formatError(DioException e) {
    switch (e.type) {
      case DioExceptionType.connectionTimeout:
      case DioExceptionType.sendTimeout:
      case DioExceptionType.receiveTimeout:
        return 'Connection timed out';
      case DioExceptionType.connectionError:
        return 'Connection failed — check your network';
      case DioExceptionType.badResponse:
        final status = e.response?.statusCode;
        return 'Server error ($status)';
      default:
        return 'Request failed: \${e.message}';
    }
  }

  void dispose() {
    cancel();
    _dio.close();
  }
}
