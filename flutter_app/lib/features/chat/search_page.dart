import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import '../../shared/services/database_service.dart';
import '../../agent/models.dart';

class MessageSearchPage extends StatefulWidget {
  const MessageSearchPage({super.key});

  @override
  State<MessageSearchPage> createState() => _MessageSearchPageState();
}

class _MessageSearchPageState extends State<MessageSearchPage> {
  final _controller = TextEditingController();
  List<_SearchResult> _results = [];
  bool _isSearching = false;

  Future<void> _search(String query) async {
    if (query.trim().length < 2) return;
    setState(() => _isSearching = true);

    final db = await DatabaseService.database;
    final rows = await db.rawQuery(
      """SELECT m.id, m.content, m.role, m.timestamp, m.conversation_id,
         c.title as conversation_title
         FROM messages m
         LEFT JOIN conversations c ON m.conversation_id = c.id
         WHERE m.content LIKE ?
         ORDER BY m.timestamp DESC
         LIMIT 50""",
      ['%${query.trim()}%'],
    );

    final results = rows.map((r) => _SearchResult(
      messageId: r['id'] as String,
      content: r['content'] as String,
      role: r['role'] as String,
      timestamp: r['timestamp'] as int,
      conversationId: r['conversation_id'] as String,
      conversationTitle: r['conversation_title'] as String? ?? 'Unknown',
    )).toList();

    setState(() {
      _results = results;
      _isSearching = false;
    });
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final colorScheme = Theme.of(context).colorScheme;

    return Scaffold(
      appBar: AppBar(
        title: TextField(
          controller: _controller,
          autofocus: true,
          decoration: InputDecoration(
            hintText: 'Search messages...',
            border: InputBorder.none,
            hintStyle: TextStyle(color: colorScheme.onSurfaceVariant.withValues(alpha: 0.5)),
          ),
          onSubmitted: _search,
          onChanged: (v) {
            if (v.length >= 3) _search(v);
          },
        ),
      ),
      body: _isSearching
          ? const Center(child: CircularProgressIndicator())
          : _results.isEmpty
              ? Center(
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Icon(Icons.search_off, size: 48, color: colorScheme.onSurfaceVariant.withValues(alpha: 0.3)),
                      const SizedBox(height: 12),
                      Text(
                        _controller.text.isEmpty ? 'Type to search' : 'No results found',
                        style: TextStyle(color: colorScheme.onSurfaceVariant),
                      ),
                    ],
                  ),
                )
              : ListView.separated(
                  itemCount: _results.length,
                  separatorBuilder: (_, __) => const Divider(height: 1),
                  itemBuilder: (context, index) {
                    final r = _results[index];
                    final date = DateTime.fromMillisecondsSinceEpoch(r.timestamp);
                    final dateStr = DateFormat('MM/dd HH:mm').format(date);

                    return ListTile(
                      leading: Icon(
                        r.role == 'user' ? Icons.person : Icons.smart_toy,
                        color: r.role == 'user' ? colorScheme.primary : colorScheme.secondary,
                      ),
                      title: Text(
                        r.content,
                        maxLines: 2,
                        overflow: TextOverflow.ellipsis,
                        style: const TextStyle(fontSize: 14),
                      ),
                      subtitle: Text(
                        '${r.conversationTitle} · $dateStr',
                        style: TextStyle(fontSize: 12, color: colorScheme.onSurfaceVariant),
                      ),
                      onTap: () {
                        // TODO: Navigate to conversation and scroll to message
                        Navigator.pop(context, r.conversationId);
                      },
                    );
                  },
                ),
    );
  }
}

class _SearchResult {
  final String messageId;
  final String content;
  final String role;
  final int timestamp;
  final String conversationId;
  final String conversationTitle;

  _SearchResult({
    required this.messageId,
    required this.content,
    required this.role,
    required this.timestamp,
    required this.conversationId,
    required this.conversationTitle,
  });
}
