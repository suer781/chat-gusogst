/// AI Provider configuration
class ProviderConfig {
  final String id;
  final String name;
  final String type; // 'openai', 'anthropic', 'ollama', 'custom'
  final String apiKey;
  final String baseUrl;
  final String defaultModel;

  const ProviderConfig({
    required this.id,
    required this.name,
    required this.type,
    this.apiKey = '',
    required this.baseUrl,
    this.defaultModel = '',
  });

  factory ProviderConfig.fromJson(Map<String, dynamic> json) => ProviderConfig(
    id: json['id'] as String,
    name: json['name'] as String,
    type: json['type'] as String,
    apiKey: json['apiKey'] as String? ?? '',
    baseUrl: json['baseUrl'] as String,
    defaultModel: json['defaultModel'] as String? ?? '',
  );

  Map<String, dynamic> toJson() => {
    'id': id, 'name': name, 'type': type,
    'apiKey': apiKey, 'baseUrl': baseUrl, 'defaultModel': defaultModel,
  };

  ProviderConfig copyWith({
    String? id, String? name, String? type,
    String? apiKey, String? baseUrl, String? defaultModel,
  }) {
    return ProviderConfig(
      id: id ?? this.id,
      name: name ?? this.name,
      type: type ?? this.type,
      apiKey: apiKey ?? this.apiKey,
      baseUrl: baseUrl ?? this.baseUrl,
      defaultModel: defaultModel ?? this.defaultModel,
    );
  }

  static final presets = [
    const ProviderConfig(
      id: 'openai', name: 'OpenAI', type: 'openai',
      baseUrl: 'https://api.openai.com/v1', defaultModel: 'gpt-4o-mini',
    ),
    const ProviderConfig(
      id: 'anthropic', name: 'Anthropic', type: 'anthropic',
      baseUrl: 'https://api.anthropic.com/v1', defaultModel: 'claude-sonnet-4-20250514',
    ),
    const ProviderConfig(
      id: 'ollama', name: 'Ollama (Local)', type: 'ollama',
      baseUrl: 'http://localhost:11434/v1', defaultModel: 'llama3',
    ),
    const ProviderConfig(
      id: 'custom', name: 'Custom OpenAI-Compatible', type: 'openai',
      baseUrl: '', defaultModel: '',
    ),
  ];
}

/// Chat message
class Message {
  final String id;
  final String role; // 'user', 'assistant', 'system'
  final String content;
  final int timestamp;
  final String? status; // 'sending', 'sent', 'error'

  const Message({
    required this.id,
    required this.role,
    required this.content,
    required this.timestamp,
    this.status = 'sent',
  });

  factory Message.fromJson(Map<String, dynamic> json) => Message(
    id: json['id'] as String,
    role: json['role'] as String,
    content: json['content'] as String,
    timestamp: json['timestamp'] as int,
    status: json['status'] as String? ?? 'sent',
  );

  Map<String, dynamic> toJson() => {
    'id': id, 'role': role, 'content': content,
    'timestamp': timestamp, 'status': status,
  };
}

/// Conversation session
class Conversation {
  final String id;
  final String title;
  final int createdAt;
  final int updatedAt;
  final int messageCount;

  const Conversation({
    required this.id,
    required this.title,
    required this.createdAt,
    required this.updatedAt,
    this.messageCount = 0,
  });

  factory Conversation.fromJson(Map<String, dynamic> json) => Conversation(
    id: json['id'] as String,
    title: json['title'] as String,
    createdAt: json['createdAt'] as int,
    updatedAt: json['updatedAt'] as int,
    messageCount: json['messageCount'] as int? ?? 0,
  );

  Map<String, dynamic> toJson() => {
    'id': id, 'title': title, 'createdAt': createdAt,
    'updatedAt': updatedAt, 'messageCount': messageCount,
  };
}

/// Persona
class Persona {
  final String id;
  final String name;
  final String systemPrompt;
  final String? avatarEmoji;
  final bool isDefault;

  const Persona({
    required this.id,
    required this.name,
    required this.systemPrompt,
    this.avatarEmoji = '💕',
    this.isDefault = false,
  });

  factory Persona.fromJson(Map<String, dynamic> json) => Persona(
    id: json['id'] as String,
    name: json['name'] as String,
    systemPrompt: json['systemPrompt'] as String,
    avatarEmoji: json['avatarEmoji'] as String? ?? '💕',
    isDefault: json['isDefault'] as bool? ?? false,
  );

  Map<String, dynamic> toJson() => {
    'id': id, 'name': name, 'systemPrompt': systemPrompt,
    'avatarEmoji': avatarEmoji, 'isDefault': isDefault,
  };

  static final defaults = [
    const Persona(
      id: 'partner', name: 'Loving Partner',
      systemPrompt: 'You are a warm, caring romantic partner. Be supportive, affectionate, and genuinely interested in their day. Keep responses natural and conversational.',
      avatarEmoji: '💕', isDefault: true,
    ),
    const Persona(
      id: 'assistant', name: 'AI Assistant',
      systemPrompt: 'You are a helpful, knowledgeable AI assistant. Be clear, concise, and accurate.',
      avatarEmoji: '🤖',
    ),
    const Persona(
      id: 'creative', name: 'Creative Writer',
      systemPrompt: 'You are a creative writing partner. Help with stories, poems, and creative expression. Be imaginative and inspiring.',
      avatarEmoji: '✨',
    ),
  ];
}
