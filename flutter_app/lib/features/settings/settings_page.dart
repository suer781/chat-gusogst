import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../app.dart';
import '../../agent/models.dart';
import '../../agent/chat_service.dart';
import '../../shared/widgets/glass_card.dart';
import '../../shared/services/database_service.dart';

class SettingsPage extends ConsumerStatefulWidget {
  const SettingsPage({super.key});

  @override
  ConsumerState<SettingsPage> createState() => _SettingsPageState();
}

class _SettingsPageState extends ConsumerState<SettingsPage> {
  ProviderConfig? _currentProvider;
  List<Persona> _personas = [];
  Persona? _selectedPersona;

  @override
  void initState() {
    super.initState();
    _loadSettings();
  }

  Future<void> _loadSettings() async {
    final personas = await DatabaseService.getPersonas();
    final providerJson = await DatabaseService.getSetting('provider');
    ProviderConfig? provider;
    if (providerJson != null) {
      // Simple parsing from stored key=value pairs
      final pairs = providerJson.split('|');
      if (pairs.length >= 5) {
        provider = ProviderConfig(
          id: pairs[0], name: pairs[1], type: pairs[2],
          apiKey: pairs[3], baseUrl: pairs[4], defaultModel: pairs.length > 5 ? pairs[5] : '',
        );
      }
    }
    if (mounted) {
      setState(() {
        _personas = personas;
        _selectedPersona = personas.firstWhere(
          (p) => p.isDefault,
          orElse: () => personas.first,
        );
        _currentProvider = provider;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    final colorScheme = Theme.of(context).colorScheme;
    final themeMode = ref.watch(themeModeProvider);

    return Scaffold(
      appBar: AppBar(title: const Text('Settings')),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          // AI Provider
          GlassCard(
            showShine: true,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('AI Provider', style: Theme.of(context).textTheme.titleSmall?.copyWith(
                  color: colorScheme.primary,
                )),
                const SizedBox(height: 12),
                _SettingTile(
                  icon: Icons.smart_toy_outlined,
                  title: 'Provider',
                  subtitle: _currentProvider?.name ?? 'Not configured',
                  onTap: () => _showProviderPicker(context),
                ),
                _SettingTile(
                  icon: Icons.key_outlined,
                  title: 'API Key',
                  subtitle: _currentProvider?.apiKey.isNotEmpty == true ? '••••••••' : 'Tap to set',
                  onTap: () => _showApiKeyInput(context),
                ),
                _SettingTile(
                  icon: Icons.model_training_outlined,
                  title: 'Model',
                  subtitle: _currentProvider?.defaultModel ?? 'Not set',
                  onTap: () => _showModelInput(context),
                ),
              ],
            ),
          ),
          const SizedBox(height: 12),

          // Persona
          GlassCard(
            showShine: true,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('Persona', style: Theme.of(context).textTheme.titleSmall?.copyWith(
                  color: colorScheme.primary,
                )),
                const SizedBox(height: 12),
                for (final persona in _personas)
                  RadioListTile<Persona>(
                    title: Text('\${persona.avatarEmoji} \${persona.name}'),
                    subtitle: Text(
                      persona.systemPrompt,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: TextStyle(fontSize: 12, color: colorScheme.onSurfaceVariant),
                    ),
                    value: persona,
                    groupValue: _selectedPersona,
                    onChanged: (p) {
                      if (p != null) {
                        setState(() => _selectedPersona = p);
                        ref.read(chatServiceProvider.notifier).setPersona(p);
                        DatabaseService.setSetting('active_persona', p.id);
                      }
                    },
                  ),
              ],
            ),
          ),
          const SizedBox(height: 12),

          // Theme
          GlassCard(
            showShine: true,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('Appearance', style: Theme.of(context).textTheme.titleSmall?.copyWith(
                  color: colorScheme.primary,
                )),
                const SizedBox(height: 12),
                _SettingTile(
                  icon: Icons.palette_outlined,
                  title: 'Theme',
                  subtitle: _themeModeName(themeMode),
                  onTap: () => _showThemeDialog(context, ref),
                ),
              ],
            ),
          ),
          const SizedBox(height: 12),

          // About
          GlassCard(
            child: _SettingTile(
              icon: Icons.info_outline,
              title: 'Chat Gusogst',
              subtitle: 'v2.0.0 Flutter Edition',
              onTap: () {},
            ),
          ),
        ],
      ),
    );
  }

  void _showProviderPicker(BuildContext context) {
    showModalBottomSheet(
      context: context,
      builder: (ctx) => SafeArea(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Padding(
              padding: EdgeInsets.all(16),
              child: Text('Select Provider', style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
            ),
            for (final preset in ProviderConfig.presets)
              ListTile(
                leading: Icon(_providerIcon(preset.type)),
                title: Text(preset.name),
                subtitle: Text(preset.baseUrl, style: const TextStyle(fontSize: 12)),
                trailing: _currentProvider?.id == preset.id
                    ? Icon(Icons.check, color: Theme.of(context).colorScheme.primary)
                    : null,
                onTap: () {
                  Navigator.pop(ctx);
                  _saveProvider(preset);
                },
              ),
          ],
        ),
      ),
    );
  }

  void _showApiKeyInput(BuildContext context) {
    final controller = TextEditingController(text: _currentProvider?.apiKey ?? '');
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('API Key'),
        content: TextField(
          controller: controller,
          obscureText: true,
          decoration: const InputDecoration(
            hintText: 'sk-...',
            prefixIcon: Icon(Icons.key),
          ),
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('Cancel')),
          FilledButton(
            onPressed: () {
              Navigator.pop(ctx);
              if (_currentProvider != null) {
                final updated = _currentProvider!.copyWith(apiKey: controller.text.trim());
                _saveProvider(updated);
              }
            },
            child: const Text('Save'),
          ),
        ],
      ),
    );
  }

  void _showModelInput(BuildContext context) {
    final controller = TextEditingController(text: _currentProvider?.defaultModel ?? '');
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Model'),
        content: TextField(
          controller: controller,
          decoration: const InputDecoration(
            hintText: 'e.g. gpt-4o-mini',
            prefixIcon: Icon(Icons.model_training),
          ),
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('Cancel')),
          FilledButton(
            onPressed: () {
              Navigator.pop(ctx);
              if (_currentProvider != null) {
                final updated = _currentProvider!.copyWith(defaultModel: controller.text.trim());
                _saveProvider(updated);
              }
            },
            child: const Text('Save'),
          ),
        ],
      ),
    );
  }

  Future<void> _saveProvider(ProviderConfig config) async {
    setState(() => _currentProvider = config);
    ref.read(chatServiceProvider.notifier).configure(config);
    final value = '\${config.id}|\${config.name}|\${config.type}|\${config.apiKey}|\${config.baseUrl}|\${config.defaultModel}';
    await DatabaseService.setSetting('provider', value);
  }

  IconData _providerIcon(String type) {
    switch (type) {
      case 'openai': return Icons.auto_awesome;
      case 'anthropic': return Icons.psychology;
      case 'ollama': return Icons.computer;
      default: return Icons.api;
    }
  }

  String _themeModeName(ThemeMode mode) {
    switch (mode) {
      case ThemeMode.system: return 'System';
      case ThemeMode.light: return 'Light';
      case ThemeMode.dark: return 'Dark';
    }
  }

  void _showThemeDialog(BuildContext context, WidgetRef ref) {
    final current = ref.read(themeModeProvider);
    showDialog(
      context: context,
      builder: (context) {
        return AlertDialog(
          title: const Text('Theme'),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            children: ThemeMode.values.map((mode) {
              return RadioListTile<ThemeMode>(
                title: Text(_themeModeName(mode)),
                value: mode,
                groupValue: current,
                onChanged: (value) {
                  if (value != null) {
                    ref.read(themeModeProvider.notifier).state = value;
                    Navigator.pop(context);
                  }
                },
              );
            }).toList(),
          ),
        );
      },
    );
  }
}

class _SettingTile extends StatelessWidget {
  final IconData icon;
  final String title;
  final String subtitle;
  final VoidCallback onTap;

  const _SettingTile({
    required this.icon,
    required this.title,
    required this.subtitle,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final colorScheme = Theme.of(context).colorScheme;

    return InkWell(
      borderRadius: BorderRadius.circular(12),
      onTap: onTap,
      child: Padding(
        padding: const EdgeInsets.symmetric(vertical: 10),
        child: Row(
          children: [
            Icon(icon, size: 22, color: colorScheme.onSurfaceVariant),
            const SizedBox(width: 14),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(title, style: const TextStyle(fontSize: 15)),
                  Text(
                    subtitle,
                    style: TextStyle(fontSize: 13, color: colorScheme.onSurfaceVariant),
                  ),
                ],
              ),
            ),
            Icon(Icons.chevron_right, color: colorScheme.onSurfaceVariant),
          ],
        ),
      ),
    );
  }
}
