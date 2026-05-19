import 'package:sqflite/sqflite.dart';
import 'package:path/path.dart';
import 'package:uuid/uuid.dart';
import '../../agent/models.dart';

/// SQLite database for chat history and settings
class DatabaseService {
  static Database? _db;
  static const _uuid = Uuid();

  static Future<Database> get database async {
    if (_db != null) return _db!;
    _db = await _initDb();
    return _db!;
  }

  static Future<Database> _initDb() async {
    final path = join(await getDatabasesPath(), 'chat_gusogst.db');
    return openDatabase(
      path,
      version: 1,
      onCreate: (db, version) async {
        await db.execute('''
          CREATE TABLE conversations (
            id TEXT PRIMARY KEY,
            title TEXT NOT NULL,
            created_at INTEGER NOT NULL,
            updated_at INTEGER NOT NULL,
            message_count INTEGER DEFAULT 0
          )
        ''');

        await db.execute('''
          CREATE TABLE messages (
            id TEXT PRIMARY KEY,
            conversation_id TEXT NOT NULL,
            role TEXT NOT NULL,
            content TEXT NOT NULL,
            timestamp INTEGER NOT NULL,
            status TEXT DEFAULT 'sent',
            FOREIGN KEY (conversation_id) REFERENCES conversations(id)
          )
        ''');

        await db.execute('''
          CREATE TABLE personas (
            id TEXT PRIMARY KEY,
            name TEXT NOT NULL,
            system_prompt TEXT NOT NULL,
            avatar_emoji TEXT DEFAULT '💕',
            is_default INTEGER DEFAULT 0
          )
        ''');

        await db.execute('''
          CREATE TABLE settings (
            key TEXT PRIMARY KEY,
            value TEXT NOT NULL
          )
        ''');
      },
    );
  }

  // --- Conversations ---

  static Future<Conversation> createConversation({String title = 'New Chat'}) async {
    final db = await database;
    final now = DateTime.now().millisecondsSinceEpoch;
    final conversation = Conversation(
      id: _uuid.v4(),
      title: title,
      createdAt: now,
      updatedAt: now,
    );
    await db.insert('conversations', {
      'id': conversation.id,
      'title': conversation.title,
      'created_at': conversation.createdAt,
      'updated_at': conversation.updatedAt,
      'message_count': 0,
    });
    return conversation;
  }

  static Future<List<Conversation>> getConversations() async {
    final db = await database;
    final rows = await db.query('conversations', orderBy: 'updated_at DESC');
    return rows.map((r) => Conversation(
      id: r['id'] as String,
      title: r['title'] as String,
      createdAt: r['created_at'] as int,
      updatedAt: r['updated_at'] as int,
      messageCount: r['message_count'] as int,
    )).toList();
  }

  static Future<void> updateConversationTitle(String id, String title) async {
    final db = await database;
    await db.update('conversations', {
      'title': title,
      'updated_at': DateTime.now().millisecondsSinceEpoch,
    }, where: 'id = ?', whereArgs: [id]);
  }

  static Future<void> deleteConversation(String id) async {
    final db = await database;
    await db.delete('messages', where: 'conversation_id = ?', whereArgs: [id]);
    await db.delete('conversations', where: 'id = ?', whereArgs: [id]);
  }

  // --- Messages ---

  static Future<Message> addMessage({
    required String conversationId,
    required String role,
    required String content,
  }) async {
    final db = await database;
    final message = Message(
      id: _uuid.v4(),
      role: role,
      content: content,
      timestamp: DateTime.now().millisecondsSinceEpoch,
    );
    await db.insert('messages', {
      'id': message.id,
      'conversation_id': conversationId,
      'role': message.role,
      'content': message.content,
      'timestamp': message.timestamp,
      'status': 'sent',
    });
    // Update conversation
    await db.rawUpdate(
      'UPDATE conversations SET updated_at = ?, message_count = message_count + 1 WHERE id = ?',
      [message.timestamp, conversationId],
    );
    return message;
  }

  static Future<List<Message>> getMessages(String conversationId) async {
    final db = await database;
    final rows = await db.query(
      'messages',
      where: 'conversation_id = ?',
      whereArgs: [conversationId],
      orderBy: 'timestamp ASC',
    );
    return rows.map((r) => Message(
      id: r['id'] as String,
      role: r['role'] as String,
      content: r['content'] as String,
      timestamp: r['timestamp'] as int,
      status: r['status'] as String?,
    )).toList();
  }

  // --- Personas ---

  static Future<List<Persona>> getPersonas() async {
    final db = await database;
    final rows = await db.query('personas', orderBy: 'is_default DESC');
    if (rows.isEmpty) {
      // Insert defaults
      for (final p in Persona.defaults) {
        await db.insert('personas', {
          'id': p.id,
          'name': p.name,
          'system_prompt': p.systemPrompt,
          'avatar_emoji': p.avatarEmoji,
          'is_default': p.isDefault ? 1 : 0,
        });
      }
      return Persona.defaults;
    }
    return rows.map((r) => Persona(
      id: r['id'] as String,
      name: r['name'] as String,
      systemPrompt: r['system_prompt'] as String,
      avatarEmoji: r['avatar_emoji'] as String?,
      isDefault: (r['is_default'] as int) == 1,
    )).toList();
  }

  // --- Settings ---

  static Future<String?> getSetting(String key) async {
    final db = await database;
    final rows = await db.query('settings', where: 'key = ?', whereArgs: [key], limit: 1);
    return rows.isEmpty ? null : rows.first['value'] as String;
  }

  static Future<void> setSetting(String key, String value) async {
    final db = await database;
    await db.insert('settings', {'key': key, 'value': value},
      conflictAlgorithm: ConflictAlgorithm.replace);
  }
}
