# Flutter Migration Plan (Plan C Hybrid)

> Decision Date: 2026-05-19
> Strategy: UI + AI chat rewrite in Dart, Python backend kept as-is
> Goal: Real HDR + native blur + 120fps + zero WebView

## Architecture

```
Old: Capacitor + React + TS + WebView + Chaquopy(Python)
New: Flutter + Dart + Impeller engine + native rendering
```

## Principles

1. Small steps - each Phase independently verifiable
2. Chat first, polish later
3. Use pub.dev libraries, dont reinvent
4. Reference old code, dont copy blindly

---

## Phase 0: Project Init (Day 1)

### 0.1 Flutter Environment
- [ ] Install Flutter SDK (stable channel)
- [ ] flutter doctor check
- [ ] Confirm Impeller enabled (Android default)

### 0.2 Create Project
- [ ] flutter create chat_gusogst --org com.chatgusogst
- [ ] Configure build.gradle: minSdk 24, targetSdk 34
- [ ] Enable P3 color: android:colorMode="wideColorGamut" in AndroidManifest.xml
- [ ] Enable Impeller meta-data

### 0.3 Directory Structure
```
lib/
  main.dart                 # Entry
  app.dart                  # MaterialApp + theme
  core/                     # Core utils
    constants.dart
    theme.dart              # M3 theme
    router.dart             # GoRouter
  features/
    chat/                   # Chat
    settings/               # Settings
    persona/                # Persona
  shared/
    widgets/                # Reusable widgets
    services/               # Shared services
  agent/                    # Agent core (Dart rewrite)
    providers/              # AI providers
    chat_service.dart
    stream_handler.dart
```

### 0.4 Dependencies
```yaml
dependencies:
  google_fonts: ^6.0
  flutter_animate: ^4.0
  shimmer: ^3.0
  dio: ^5.0                  # SSE streaming
  riverpod: ^2.0             # State management
  shared_preferences: ^2.0
  sqflite: ^2.0
  path_provider: ^2.0
  flutter_markdown: ^0.6
  uuid: ^4.0
  intl: ^0.19
```

---

## Phase 1: Foundation (Day 2-3)

### 1.1 Theme System
- [ ] M3 dynamic color (ColorScheme.fromSeed)
- [ ] Dark/Light theme switch
- [ ] P3 wide gamut colors:
  ```dart
  static const accentP3 = Color.from(
    colorSpace: ColorSpace.displayP3,
    red: 0.8, green: 0.3, blue: 0.4, alpha: 1.0,
  );
  ```
- [ ] HDR glass effect variables

### 1.2 Routing
- [ ] GoRouter config
- [ ] Home -> Chat -> Settings routes
- [ ] Material You shared axis transitions

### 1.3 Base Widgets
- [ ] GlassCard - BackdropFilter blur
- [ ] GlassHeader - Blur top bar
- [ ] GlassNav - Blur bottom nav
- [ ] AppButton - Scale + bounce animation

---

## Phase 2: Chat Core (Day 4-6)

### 2.1 Data Models
- [ ] Message (id, role, content, timestamp, status)
- [ ] Conversation (id, title, messages, createdAt)
- [ ] ProviderConfig (name, apiKey, baseUrl, model)
- [ ] Persona (name, systemPrompt, avatar)

### 2.2 Chat UI
- [ ] ChatPage - main chat page
- [ ] MessageBubble - AI/user distinction
- [ ] ChatInput - input + send button
- [ ] TypingIndicator - 3-dot bounce
- [ ] MarkdownRenderer - code highlight
- [ ] Message list - reverse ListView + auto scroll

### 2.3 AI Calling
- [ ] OpenAI-compatible API (Dio)
- [ ] SSE streaming parse:
  ```dart
  final response = await dio.post(
    '$baseUrl/chat/completions',
    options: Options(responseType: ResponseType.stream),
    data: {'model': model, 'messages': msgs, 'stream': true},
  );
  ```
- [ ] Multi-provider (OpenAI/Anthropic/Custom/Ollama)
- [ ] Endpoint prober (auto-detect API paths)
- [ ] Retry + timeout

### 2.4 Chat History
- [ ] SQLite storage
- [ ] Session list page
- [ ] Create/delete/rename sessions
- [ ] Message search

---

## Phase 3: Settings & Persona (Day 7-8)

### 3.1 Settings Page
- [ ] SettingsPage
- [ ] API config (Provider + Key + Model)
- [ ] Theme switch (light/dark/system)
- [ ] HDR toggle
- [ ] Glass toggle
- [ ] About page

### 3.2 Persona System
- [ ] PersonaPage - manage personas
- [ ] Preset persona templates
- [ ] Custom persona (name + system prompt)
- [ ] Persona switch (keep chat history)

---

## Phase 4: HDR Glass Effects (Day 9-10)

### 4.1 Glass Base
- [ ] BackdropFilter + ImageFilter.blur
- [ ] Dark/Light different blur strength
- [ ] ClipRRect rounded corners

### 4.2 HDR Rendering
- [ ] P3 color application
- [ ] CustomPainter prismatic refraction
- [ ] AnimationController aurora breath
- [ ] Shadow pulse (BoxShadow animation)
- [ ] Gloss flow (gradient offset animation)

### 4.3 Haptic Feedback
- [ ] HapticFeedback.lightImpact() on button
- [ ] HapticFeedback.mediumImpact() on long press
- [ ] Bounce button animation (scale + bounce curve)

### 4.4 Reduced Motion
- [ ] MediaQuery.disableAnimations detection
- [ ] Disable effects in reduced mode

---

## Phase 5: Features (Day 11-13)

### 5.1 Web Search
- [ ] Search tool (Tavily / DuckDuckGo)
- [ ] Search result card
- [ ] Search trigger detection

### 5.2 Tool System
- [ ] Tool definition protocol
- [ ] Tool call parsing (tool_calls)
- [ ] Tool exec + result return
- [ ] Basic tools: search, weather, datetime

### 5.3 Memory System
- [ ] Long-term memory (SharedPreferences / SQLite)
- [ ] Key info extraction
- [ ] Memory injection into System Prompt

---

## Phase 6: Polish & Release (Day 14-15)

### 6.1 Performance
- [ ] Message list lazy loading
- [ ] Image cache
- [ ] Animation frame monitoring

### 6.2 UX Polish
- [ ] Splash screen
- [ ] Empty state page
- [ ] Error handling (network, API errors)
- [ ] Long press copy/delete

### 6.3 Build & Deploy
- [ ] GitHub Actions CI (Flutter build)
- [ ] APK signing config
- [ ] Version management
- [ ] Release build + upload artifact

---

## Tech Decisions

| Decision | Choice | Reason |
|----------|--------|--------|
| State Mgmt | Riverpod | More flexible than Provider, simpler than Bloc |
| Network | Dio | Native SSE streaming support |
| Markdown | flutter_markdown | Official, good enough |
| Database | sqflite | Structured query for chat history |
| Routing | GoRouter | Declarative, M3 transitions |
| Animation | flutter_animate + native | Simple use library, complex use CustomPainter |

## Not Migrating

| Old Code | Action | Reason |
|----------|--------|--------|
| React UI (22 files) | Dart rewrite | Different framework entirely |
| Hermes Python backend | Keep for now | 80+ tools, rewrite as needed |
| Chaquopy bridge | Drop | Dart calls API directly |
| TS Agent core | Dart rewrite core | AI call + streaming + tool protocol |
| endpoint_prober.py | Dart rewrite | Probing logic in Dart |
| MCP tools | Phase 5 as needed | Skip for now |

## Risks

| Risk | Mitigation |
|------|------------|
| Impeller issues on some devices | Skia fallback: --no-enable-impeller |
| P3 not supported on old devices | Auto fallback to sRGB |
| flutter_markdown not enough | Fallback: flutter_highlight or super_editor |
| SSE parsing issues | Fallback: web_socket_channel |