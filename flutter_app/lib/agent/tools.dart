import 'dart:convert';
import 'package:dio/dio.dart';

/// Tool definition
class ToolDef {
  final String name;
  final String description;
  final Map<String, dynamic> parameters; // JSON Schema

  const ToolDef({required this.name, required this.description, required this.parameters});

  Map<String, dynamic> toJson() => {
    'type': 'function',
    'function': {
      'name': name,
      'description': description,
      'parameters': parameters,
    },
  };
}

/// Tool call result
class ToolResult {
  final String name;
  final String content;
  final bool isError;

  const ToolResult({required this.name, required this.content, this.isError = false});
}

/// Tool registry — manages available tools
class ToolRegistry {
  final Dio _dio = Dio();
  final Map<String, ToolDef> _tools = {};
  final Map<String, Future<ToolResult> Function(Map<String, dynamic>)> _handlers = {};

  ToolRegistry() {
    _registerBuiltinTools();
  }

  void _registerBuiltinTools() {
    register(
      const ToolDef(
        name: 'web_search',
        description: 'Search the web for information. Use when the user asks about current events, facts, or anything requiring up-to-date knowledge.',
        parameters: {
          'type': 'object',
          'properties': {
            'query': {'type': 'string', 'description': 'Search query'},
          },
          'required': ['query'],
        },
      ),
      _webSearch,
    );

    register(
      const ToolDef(
        name: 'get_weather',
        description: 'Get current weather for a location.',
        parameters: {
          'type': 'object',
          'properties': {
            'location': {'type': 'string', 'description': 'City name or coordinates'},
          },
          'required': ['location'],
        },
      ),
      _getWeather,
    );

    register(
      const ToolDef(
        name: 'get_datetime',
        description: 'Get current date and time.',
        parameters: {'type': 'object', 'properties': {}, 'required': []},
      ),
      _getDateTime,
    );
  }

  void register(ToolDef tool, Future<ToolResult> Function(Map<String, dynamic>) handler) {
    _tools[tool.name] = tool;
    _handlers[tool.name] = handler;
  }

  List<ToolDef> get tools => _tools.values.toList();

  bool get hasTools => _tools.isNotEmpty;

  Future<ToolResult> execute(String name, Map<String, dynamic> args) async {
    final handler = _handlers[name];
    if (handler == null) {
      return ToolResult(name: name, content: 'Tool not found: $name', isError: true);
    }
    try {
      return await handler(args);
    } catch (e) {
      return ToolResult(name: name, content: 'Error: $e', isError: true);
    }
  }

  /// Format tool calls for API request
  List<Map<String, dynamic>> toJsonList() {
    return _tools.values.map((t) => t.toJson()).toList();
  }

  // --- Built-in tool implementations ---

  Future<ToolResult> _webSearch(Map<String, dynamic> args) async {
    final query = args['query'] as String? ?? '';
    // Use DuckDuckGo HTML (free, no API key)
    try {
      final response = await _dio.get(
        'https://html.duckduckgo.com/html/',
        queryParameters: {'q': query},
        options: Options(
          headers: {'User-Agent': 'Mozilla/5.0'},
          receiveTimeout: const Duration(seconds: 10),
        ),
      );
      // Simple extraction from HTML results
      final html = response.data as String;
      final results = <String>[];
      final snippetRegex = RegExp(r'class="result__snippet">(.*?)</a>', dotAll: true);
      final matches = snippetRegex.allMatches(html).take(3);
      for (final m in matches) {
        final snippet = m.group(1)?.replaceAll(RegExp(r'<[^>]+>'), '').trim();
        if (snippet != null && snippet.isNotEmpty) results.add(snippet);
      }
      return ToolResult(
        name: 'web_search',
        content: results.isEmpty ? 'No results found for: $query' : results.join('\n\n'),
      );
    } catch (e) {
      return ToolResult(name: 'web_search', content: 'Search failed: $e', isError: true);
    }
  }

  Future<ToolResult> _getWeather(Map<String, dynamic> args) async {
    final location = args['location'] as String? ?? '';
    // Use wttr.in (free, no API key)
    try {
      final response = await _dio.get(
        'https://wttr.in/${Uri.encodeComponent(location)}',
        queryParameters: {'format': 'j1'},
        options: Options(receiveTimeout: const Duration(seconds: 10)),
      );
      final data = response.data as Map<String, dynamic>;
      final current = data['current_condition']?[0];
      if (current != null) {
        final temp = current['temp_C'];
        final desc = current['weatherDesc']?[0]?['value'];
        final humidity = current['humidity'];
        final wind = current['windspeedKmph'];
        return ToolResult(
          name: 'get_weather',
          content: 'Weather in $location: $temp°C, $desc. Humidity: $humidity%, Wind: $wind km/h',
        );
      }
      return ToolResult(name: 'get_weather', content: 'Could not get weather for $location');
    } catch (e) {
      return ToolResult(name: 'get_weather', content: 'Weather lookup failed: $e', isError: true);
    }
  }

  Future<ToolResult> _getDateTime(Map<String, dynamic> args) async {
    final now = DateTime.now();
    final weekdays = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday'];
    return ToolResult(
      name: 'get_datetime',
      content: 'Current date: ${now.year}-${now.month.toString().padLeft(2, '0')}-${now.day.toString().padLeft(2, '0')} ${weekdays[now.weekday - 1]}, ${now.hour.toString().padLeft(2, '0')}:${now.minute.toString().padLeft(2, '0')}',
    );
  }

  void dispose() {
    _dio.close();
  }
}
