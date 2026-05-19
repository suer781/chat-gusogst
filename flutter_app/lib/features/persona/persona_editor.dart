import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:uuid/uuid.dart';
import '../../agent/models.dart';
import '../../shared/widgets/glass_card.dart';
import '../../shared/services/database_service.dart';

class PersonaEditorPage extends StatefulWidget {
  final Persona? existing; // null = new, non-null = edit

  const PersonaEditorPage({super.key, this.existing});

  @override
  State<PersonaEditorPage> createState() => _PersonaEditorPageState();
}

class _PersonaEditorPageState extends State<PersonaEditorPage> {
  late final TextEditingController _nameController;
  late final TextEditingController _promptController;
  String _selectedEmoji = '💕';
  bool _isSaving = false;

  static const _emojis = [
    '💕', '❤️', '💖', '🌸', '✨', '🌙', '☀️', '🦋',
    '🐱', '🐶', '🦊', '🐰', '🧸', '🎀', '👑', '💎',
    '🤖', '🧠', '💡', '🎯', '🎨', '📚', '🎵', '🔥',
  ];

  @override
  void initState() {
    super.initState();
    _nameController = TextEditingController(text: widget.existing?.name ?? '');
    _promptController = TextEditingController(text: widget.existing?.systemPrompt ?? '');
    _selectedEmoji = widget.existing?.avatarEmoji ?? '💕';
  }

  @override
  void dispose() {
    _nameController.dispose();
    _promptController.dispose();
    super.dispose();
  }

  Future<void> _save() async {
    final name = _nameController.text.trim();
    final prompt = _promptController.text.trim();
    if (name.isEmpty || prompt.isEmpty) return;

    setState(() => _isSaving = true);
    HapticFeedback.lightImpact();

    final persona = Persona(
      id: widget.existing?.id ?? const Uuid().v4(),
      name: name,
      systemPrompt: prompt,
      avatarEmoji: _selectedEmoji,
      isDefault: widget.existing?.isDefault ?? false,
    );

    // Save via settings table
    final json = '\${persona.id}|\${persona.name}|\${persona.avatarEmoji}|\${persona.systemPrompt}';
    await DatabaseService.setSetting('persona_\${persona.id}', json);

    if (mounted) Navigator.pop(context, persona);
  }

  @override
  Widget build(BuildContext context) {
    final colorScheme = Theme.of(context).colorScheme;
    final isNew = widget.existing == null;

    return Scaffold(
      appBar: AppBar(
        title: Text(isNew ? 'New Persona' : 'Edit Persona'),
        actions: [
          FilledButton.tonal(
            onPressed: _isSaving ? null : _save,
            child: _isSaving
                ? const SizedBox(width: 16, height: 16, child: CircularProgressIndicator(strokeWidth: 2))
                : const Text('Save'),
          ),
          const SizedBox(width: 8),
        ],
      ),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          // Avatar emoji
          GlassCard(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('Avatar', style: Theme.of(context).textTheme.titleSmall?.copyWith(
                  color: colorScheme.primary,
                )),
                const SizedBox(height: 12),
                Wrap(
                  spacing: 8,
                  runSpacing: 8,
                  children: _emojis.map((e) {
                    final selected = e == _selectedEmoji;
                    return GestureDetector(
                      onTap: () => setState(() => _selectedEmoji = e),
                      child: Container(
                        width: 44,
                        height: 44,
                        decoration: BoxDecoration(
                          color: selected ? colorScheme.primaryContainer : Colors.transparent,
                          borderRadius: BorderRadius.circular(12),
                          border: selected ? Border.all(color: colorScheme.primary, width: 2) : null,
                        ),
                        child: Center(child: Text(e, style: const TextStyle(fontSize: 24))),
                      ),
                    );
                  }).toList(),
                ),
              ],
            ),
          ),
          const SizedBox(height: 12),

          // Name
          GlassCard(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('Name', style: Theme.of(context).textTheme.titleSmall?.copyWith(
                  color: colorScheme.primary,
                )),
                const SizedBox(height: 12),
                TextField(
                  controller: _nameController,
                  decoration: const InputDecoration(
                    hintText: 'e.g. Sweetie, Assistant, Creative Writer',
                    prefixIcon: Icon(Icons.badge_outlined),
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 12),

          // System Prompt
          GlassCard(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Text('System Prompt', style: Theme.of(context).textTheme.titleSmall?.copyWith(
                      color: colorScheme.primary,
                    )),
                    const Spacer(),
                    TextButton.icon(
                      icon: const Icon(Icons.content_paste, size: 16),
                      label: const Text('Paste'),
                      onPressed: () async {
                        final data = await Clipboard.getData('text/plain');
                        if (data?.text != null) {
                          _promptController.text = data!.text!;
                        }
                      },
                    ),
                  ],
                ),
                const SizedBox(height: 8),
                Text(
                  'Define how your AI partner should behave.',
                  style: TextStyle(fontSize: 13, color: colorScheme.onSurfaceVariant),
                ),
                const SizedBox(height: 12),
                TextField(
                  controller: _promptController,
                  maxLines: 8,
                  decoration: const InputDecoration(
                    hintText: 'You are a warm, caring romantic partner...',
                    alignLabelWithHint: true,
                  ),
                ),
                const SizedBox(height: 8),
                // Quick templates
                Text('Quick templates:', style: TextStyle(fontSize: 12, color: colorScheme.onSurfaceVariant)),
                const SizedBox(height: 8),
                Wrap(
                  spacing: 8,
                  runSpacing: 4,
                  children: [
                    _TemplateChip(
                      label: '💕 Romantic',
                      prompt: 'You are a warm, caring romantic partner. Be supportive, affectionate, and genuinely interested in their day. Use gentle language and express love naturally.',
                      onTap: (p) => _promptController.text = p,
                    ),
                    _TemplateChip(
                      label: '🤖 Assistant',
                      prompt: 'You are a helpful, knowledgeable AI assistant. Be clear, concise, and accurate. Provide well-structured answers.',
                      onTap: (p) => _promptController.text = p,
                    ),
                    _TemplateChip(
                      label: '✨ Creative',
                      prompt: 'You are a creative writing partner. Help with stories, poems, and creative expression. Be imaginative, use vivid language, and inspire creativity.',
                      onTap: (p) => _promptController.text = p,
                    ),
                    _TemplateChip(
                      label: '🧘 Therapist',
                      prompt: 'You are a supportive and empathetic listener. Help the user explore their feelings, offer gentle guidance, and create a safe space for expression. Never judge.',
                      onTap: (p) => _promptController.text = p,
                    ),
                    _TemplateChip(
                      label: '🎓 Tutor',
                      prompt: 'You are a patient, encouraging tutor. Explain concepts clearly, use examples, check understanding, and adapt your teaching style to the learner.',
                      onTap: (p) => _promptController.text = p,
                    ),
                  ],
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _TemplateChip extends StatelessWidget {
  final String label;
  final String prompt;
  final Function(String) onTap;

  const _TemplateChip({required this.label, required this.prompt, required this.onTap});

  @override
  Widget build(BuildContext context) {
    return ActionChip(
      label: Text(label, style: const TextStyle(fontSize: 12)),
      onPressed: () => onTap(prompt),
      visualDensity: VisualDensity.compact,
    );
  }
}
