import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_markdown/flutter_markdown.dart';
import 'package:markdown/markdown.dart' as md;
import '../../../agent/models.dart';

class MarkdownBubble extends StatelessWidget {
  final Message message;

  const MarkdownBubble({super.key, required this.message});

  bool get isUser => message.role == 'user';

  @override
  Widget build(BuildContext context) {
    final colorScheme = Theme.of(context).colorScheme;
    final isDark = Theme.of(context).brightness == Brightness.dark;

    return Align(
      alignment: isUser ? Alignment.centerRight : Alignment.centerLeft,
      child: Container(
        constraints: BoxConstraints(
          maxWidth: MediaQuery.of(context).size.width * 0.82,
        ),
        margin: EdgeInsets.only(
          top: 4, bottom: 4,
          left: isUser ? 48 : 4,
          right: isUser ? 4 : 48,
        ),
        child: GestureDetector(
          onLongPress: () {
            Clipboard.setData(ClipboardData(text: message.content));
            ScaffoldMessenger.of(context).showSnackBar(
              const SnackBar(content: Text('Copied'), duration: Duration(seconds: 1)),
            );
          },
          child: Container(
            padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
            decoration: BoxDecoration(
              color: isUser
                  ? colorScheme.primary
                  : isDark
                      ? colorScheme.surfaceContainerHighest.withValues(alpha: 0.5)
                      : colorScheme.surfaceContainerHighest.withValues(alpha: 0.3),
              borderRadius: BorderRadius.only(
                topLeft: const Radius.circular(18),
                topRight: const Radius.circular(18),
                bottomLeft: Radius.circular(isUser ? 18 : 4),
                bottomRight: Radius.circular(isUser ? 4 : 18),
              ),
              boxShadow: [
                BoxShadow(
                  color: Colors.black.withValues(alpha: isDark ? 0.15 : 0.05),
                  blurRadius: 6,
                  offset: const Offset(0, 2),
                ),
              ],
            ),
            child: isUser
                ? SelectableText(
                    message.content,
                    style: TextStyle(
                      color: colorScheme.onPrimary,
                      fontSize: 15,
                      height: 1.4,
                    ),
                  )
                : MarkdownBody(
                    data: message.content,
                    selectable: true,
                    extensionSet: md.ExtensionSet.gitHubFlavored,
                    styleSheet: MarkdownStyleSheet(
                      p: TextStyle(
                        color: colorScheme.onSurface,
                        fontSize: 15,
                        height: 1.4,
                      ),
                      code: TextStyle(
                        color: isDark ? Colors.lightGreenAccent : Colors.red.shade900,
                        fontSize: 13,
                        fontFamily: 'monospace',
                        backgroundColor: (isDark ? Colors.black : Colors.white).withValues(alpha: 0.3),
                      ),
                      codeblockDecoration: BoxDecoration(
                        color: (isDark ? Colors.black : Colors.grey.shade100).withValues(alpha: 0.5),
                        borderRadius: BorderRadius.circular(8),
                        border: Border.all(
                          color: colorScheme.outlineVariant.withValues(alpha: 0.3),
                        ),
                      ),
                      codeblockPadding: const EdgeInsets.all(12),
                      blockquoteDecoration: BoxDecoration(
                        border: Border(
                          left: BorderSide(color: colorScheme.primary, width: 3),
                        ),
                      ),
                      blockquotePadding: const EdgeInsets.only(left: 12, top: 4, bottom: 4),
                      h1: TextStyle(color: colorScheme.onSurface, fontSize: 22, fontWeight: FontWeight.bold),
                      h2: TextStyle(color: colorScheme.onSurface, fontSize: 19, fontWeight: FontWeight.bold),
                      h3: TextStyle(color: colorScheme.onSurface, fontSize: 17, fontWeight: FontWeight.w600),
                      listBullet: TextStyle(color: colorScheme.primary),
                      a: TextStyle(color: colorScheme.primary, decoration: TextDecoration.underline),
                    ),
                  ),
          ),
        ),
      ),
    );
  }
}
