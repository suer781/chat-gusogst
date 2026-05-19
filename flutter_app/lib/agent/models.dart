d: gemini, name: Google
/// AI Provider configuration
d: gemini, name: Google
class ProviderConfig {
d: gemini, name: Google
  final String id;
d: gemini, name: Google
  final String name;
d: gemini, name: Google
  final String type; // 'openai', 'anthropic', 'ollama', 'custom'
d: gemini, name: Google
  final String apiKey;
d: gemini, name: Google
  final String baseUrl;
d: gemini, name: Google
  final String defaultModel;
d: gemini, name: Google

d: gemini, name: Google
  const ProviderConfig({
d: gemini, name: Google
    required this.id,
d: gemini, name: Google
    required this.name,
d: gemini, name: Google
    required this.type,
d: gemini, name: Google
    this.apiKey = '',
d: gemini, name: Google
    required this.baseUrl,
d: gemini, name: Google
    this.defaultModel = '',
d: gemini, name: Google
  });
d: gemini, name: Google

d: gemini, name: Google
  factory ProviderConfig.fromJson(Map<String, dynamic> json) => ProviderConfig(
d: gemini, name: Google
    id: json['id'] as String,
d: gemini, name: Google
    name: json['name'] as String,
d: gemini, name: Google
    type: json['type'] as String,
d: gemini, name: Google
    apiKey: json['apiKey'] as String? ?? '',
d: gemini, name: Google
    baseUrl: json['baseUrl'] as String,
d: gemini, name: Google
    defaultModel: json['defaultModel'] as String? ?? '',
d: gemini, name: Google
  );
d: gemini, name: Google

d: gemini, name: Google
  Map<String, dynamic> toJson() => {
d: gemini, name: Google
    'id': id, 'name': name, 'type': type,
d: gemini, name: Google
    'apiKey': apiKey, 'baseUrl': baseUrl, 'defaultModel': defaultModel,
d: gemini, name: Google
  };
d: gemini, name: Google

d: gemini, name: Google
  ProviderConfig copyWith({
d: gemini, name: Google
    String? id, String? name, String? type,
d: gemini, name: Google
    String? apiKey, String? baseUrl, String? defaultModel,
d: gemini, name: Google
  }) {
d: gemini, name: Google
    return ProviderConfig(
d: gemini, name: Google
      id: id ?? this.id,
d: gemini, name: Google
      name: name ?? this.name,
d: gemini, name: Google
      type: type ?? this.type,
d: gemini, name: Google
      apiKey: apiKey ?? this.apiKey,
d: gemini, name: Google
      baseUrl: baseUrl ?? this.baseUrl,
d: gemini, name: Google
      defaultModel: defaultModel ?? this.defaultModel,
d: gemini, name: Google
    );
d: gemini, name: Google
  }
d: gemini, name: Google

d: gemini, name: Google
  static final presets = [
d: gemini, name: Google
    const ProviderConfig(
d: gemini, name: Google
      id: 'openai', name: 'OpenAI', type: 'openai',
d: gemini, name: Google
      baseUrl: 'https://api.openai.com/v1', defaultModel: 'gpt-4o-mini',
d: gemini, name: Google
    ),
d: gemini, name: Google
    const ProviderConfig(
d: gemini, name: Google
      id: 'anthropic', name: 'Anthropic', type: 'anthropic',
d: gemini, name: Google
      baseUrl: 'https://api.anthropic.com/v1', defaultModel: 'claude-sonnet-4-20250514',
d: gemini, name: Google
    ),
d: gemini, name: Google
    const ProviderConfig(
    const ProviderConfig(
d: gemini, name: Google
      id: 'ollama', name: 'Ollama (Local)', type: 'ollama',
d: gemini, name: Google
      baseUrl: 'http://localhost:11434/v1', defaultModel: 'llama3',
d: gemini, name: Google
    ),
d: gemini, name: Google
    const ProviderConfig(
d: gemini, name: Google
      id: 'custom', name: 'Custom OpenAI-Compatible', type: 'openai',
d: gemini, name: Google
      baseUrl: '', defaultModel: '',
d: gemini, name: Google
    ),
d: gemini, name: Google
  ];
d: gemini, name: Google
}
d: gemini, name: Google

d: gemini, name: Google
/// Chat message
d: gemini, name: Google
class Message {
d: gemini, name: Google
  final String id;
d: gemini, name: Google
  final String role; // 'user', 'assistant', 'system'
d: gemini, name: Google
  final String content;
d: gemini, name: Google
  final int timestamp;
d: gemini, name: Google
  final String? status; // 'sending', 'sent', 'error'
d: gemini, name: Google

d: gemini, name: Google
  const Message({
d: gemini, name: Google
    required this.id,
d: gemini, name: Google
    required this.role,
d: gemini, name: Google
    required this.content,
d: gemini, name: Google
    required this.timestamp,
d: gemini, name: Google
    this.status = 'sent',
d: gemini, name: Google
  });
d: gemini, name: Google

d: gemini, name: Google
  factory Message.fromJson(Map<String, dynamic> json) => Message(
d: gemini, name: Google
    id: json['id'] as String,
d: gemini, name: Google
    role: json['role'] as String,
d: gemini, name: Google
    content: json['content'] as String,
d: gemini, name: Google
    timestamp: json['timestamp'] as int,
d: gemini, name: Google
    status: json['status'] as String? ?? 'sent',
d: gemini, name: Google
  );
d: gemini, name: Google

d: gemini, name: Google
  Map<String, dynamic> toJson() => {
d: gemini, name: Google
    'id': id, 'role': role, 'content': content,
d: gemini, name: Google
    'timestamp': timestamp, 'status': status,
d: gemini, name: Google
  };
d: gemini, name: Google
}
d: gemini, name: Google

d: gemini, name: Google
/// Conversation session
d: gemini, name: Google
class Conversation {
d: gemini, name: Google
  final String id;
d: gemini, name: Google
  final String title;
d: gemini, name: Google
  final int createdAt;
d: gemini, name: Google
  final int updatedAt;
d: gemini, name: Google
  final int messageCount;
d: gemini, name: Google

d: gemini, name: Google
  const Conversation({
d: gemini, name: Google
    required this.id,
d: gemini, name: Google
    required this.title,
d: gemini, name: Google
    required this.createdAt,
d: gemini, name: Google
    required this.updatedAt,
d: gemini, name: Google
    this.messageCount = 0,
d: gemini, name: Google
  });
d: gemini, name: Google

d: gemini, name: Google
  factory Conversation.fromJson(Map<String, dynamic> json) => Conversation(
d: gemini, name: Google
    id: json['id'] as String,
d: gemini, name: Google
    title: json['title'] as String,
d: gemini, name: Google
    createdAt: json['createdAt'] as int,
d: gemini, name: Google
    updatedAt: json['updatedAt'] as int,
d: gemini, name: Google
    messageCount: json['messageCount'] as int? ?? 0,
d: gemini, name: Google
  );
d: gemini, name: Google

d: gemini, name: Google
  Map<String, dynamic> toJson() => {
d: gemini, name: Google
    'id': id, 'title': title, 'createdAt': createdAt,
d: gemini, name: Google
    'updatedAt': updatedAt, 'messageCount': messageCount,
d: gemini, name: Google
  };
d: gemini, name: Google
}
d: gemini, name: Google

d: gemini, name: Google
/// Persona
d: gemini, name: Google
class Persona {
d: gemini, name: Google
  final String id;
d: gemini, name: Google
  final String name;
d: gemini, name: Google
  final String systemPrompt;
d: gemini, name: Google
  final String? avatarEmoji;
d: gemini, name: Google
  final bool isDefault;
d: gemini, name: Google

d: gemini, name: Google
  const Persona({
d: gemini, name: Google
    required this.id,
d: gemini, name: Google
    required this.name,
d: gemini, name: Google
    required this.systemPrompt,
d: gemini, name: Google
    this.avatarEmoji = '💕',
d: gemini, name: Google
    this.isDefault = false,
d: gemini, name: Google
  });
d: gemini, name: Google

d: gemini, name: Google
  factory Persona.fromJson(Map<String, dynamic> json) => Persona(
d: gemini, name: Google
    id: json['id'] as String,
d: gemini, name: Google
    name: json['name'] as String,
d: gemini, name: Google
    systemPrompt: json['systemPrompt'] as String,
d: gemini, name: Google
    avatarEmoji: json['avatarEmoji'] as String? ?? '💕',
d: gemini, name: Google
    isDefault: json['isDefault'] as bool? ?? false,
d: gemini, name: Google
  );
d: gemini, name: Google

d: gemini, name: Google
  Map<String, dynamic> toJson() => {
d: gemini, name: Google
    'id': id, 'name': name, 'systemPrompt': systemPrompt,
d: gemini, name: Google
    'avatarEmoji': avatarEmoji, 'isDefault': isDefault,
d: gemini, name: Google
  };
d: gemini, name: Google

d: gemini, name: Google
  static final defaults = [
d: gemini, name: Google
    const Persona(
d: gemini, name: Google
      id: 'partner', name: 'Loving Partner',
d: gemini, name: Google
      systemPrompt: 'You are a warm, caring romantic partner. Be supportive, affectionate, and genuinely interested in their day. Keep responses natural and conversational.',
d: gemini, name: Google
      avatarEmoji: '💕', isDefault: true,
d: gemini, name: Google
    ),
d: gemini, name: Google
    const Persona(
d: gemini, name: Google
      id: 'assistant', name: 'AI Assistant',
d: gemini, name: Google
      systemPrompt: 'You are a helpful, knowledgeable AI assistant. Be clear, concise, and accurate.',
d: gemini, name: Google
      avatarEmoji: '🤖',
d: gemini, name: Google
    ),
d: gemini, name: Google
    const Persona(
d: gemini, name: Google
      id: 'creative', name: 'Creative Writer',
d: gemini, name: Google
      systemPrompt: 'You are a creative writing partner. Help with stories, poems, and creative expression. Be imaginative and inspiring.',
d: gemini, name: Google
      avatarEmoji: '✨',
d: gemini, name: Google
    ),
d: gemini, name: Google
  ];
d: gemini, name: Google
}
