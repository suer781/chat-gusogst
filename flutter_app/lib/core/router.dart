import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import '../features/chat/chat_page.dart';
import '../features/settings/settings_page.dart';
import '../shared/widgets/home_shell.dart';

final appRouter = GoRouter(
  initialLocation: '/',
  routes: [
    // Home shell with bottom navigation
    StatefulShellRoute.indexedStack(
      builder: (context, state, navigationShell) {
        return HomeShell(navigationShell: navigationShell);
      },
      branches: [
        // Chat tab
        StatefulShellBranch(
          routes: [
            GoRoute(
              path: '/',
              pageBuilder: (context, state) => const NoTransitionPage(
                child: ChatPage(),
              ),
            ),
          ],
        ),
        // Settings tab
        StatefulShellBranch(
          routes: [
            GoRoute(
              path: '/settings',
              pageBuilder: (context, state) => const NoTransitionPage(
                child: SettingsPage(),
              ),
            ),
          ],
        ),
      ],
    ),
  ],
);
