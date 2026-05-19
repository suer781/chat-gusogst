import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'core/theme.dart';
import 'core/router.dart';

class ChatGusogstApp extends ConsumerWidget {
  const ChatGusogstApp({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final themeMode = ref.watch(themeModeProvider);

    return MaterialApp.router(
      title: 'Chat Gusogst',
      debugShowCheckedModeBanner: false,

      // Theme
      theme: AppTheme.light(),
      darkTheme: AppTheme.dark(),
      themeMode: themeMode,

      // Router
      routerConfig: appRouter,

      // HDR: enable wide color gamut
      color: const Color(0xFF6750A4),
    );
  }
}

// Theme mode state
final themeModeProvider = StateProvider<ThemeMode>(
  (ref) => ThemeMode.system,
);
