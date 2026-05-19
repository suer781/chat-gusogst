import 'dart:ui';
import 'package:flutter/material.dart';

/// Native glass card with BackdropFilter blur.
/// This is REAL blur, not CSS backdrop-filter simulation.
class GlassCard extends StatelessWidget {
  final Widget child;
  final double blur;
  final double opacity;
  final EdgeInsetsGeometry? padding;
  final EdgeInsetsGeometry? margin;
  final BorderRadiusGeometry borderRadius;
  final bool showShine;

  const GlassCard({
    super.key,
    required this.child,
    this.blur = 16.0,
    this.opacity = 0.08,
    this.padding,
    this.margin,
    this.borderRadius = const BorderRadius.all(Radius.circular(16)),
    this.showShine = false,
  });

  @override
  Widget build(BuildContext context) {
    final colorScheme = Theme.of(context).colorScheme;
    final isDark = Theme.of(context).brightness == Brightness.dark;

    return Container(
      margin: margin,
      child: ClipRRect(
        borderRadius: borderRadius as BorderRadius,
        child: BackdropFilter(
          filter: ImageFilter.blur(sigmaX: blur, sigmaY: blur),
          child: Container(
            padding: padding ?? const EdgeInsets.all(16),
            decoration: BoxDecoration(
              // Glass fill
              color: isDark
                  ? colorScheme.surface.withValues(alpha: opacity)
                  : Colors.white.withValues(alpha: opacity + 0.15),
              // Glass border
              border: Border.all(
                color: isDark
                    ? Colors.white.withValues(alpha: 0.08)
                    : Colors.white.withValues(alpha: 0.3),
                width: 0.5,
              ),
              borderRadius: borderRadius as BorderRadius,
              // HDR shadow - P3 colors for richer depth
              boxShadow: [
                BoxShadow(
                  color: colorScheme.shadow.withValues(alpha: isDark ? 0.15 : 0.06),
                  blurRadius: 12,
                  offset: const Offset(0, 4),
                ),
                // Inner highlight
                BoxShadow(
                  color: Colors.white.withValues(alpha: isDark ? 0.03 : 0.08),
                  blurRadius: 0,
                  spreadRadius: 0,
                  offset: const Offset(0, 1),
                ),
              ],
            ),
            child: Stack(
              children: [
                child,
                // Optional gloss shine
                if (showShine)
                  Positioned.fill(
                    child: IgnorePointer(
                      child: _GlossShine(isDark: isDark),
                    ),
                  ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class _GlossShine extends StatelessWidget {
  final bool isDark;
  const _GlossShine({required this.isDark});

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(16),
        gradient: LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: [
            Colors.white.withValues(alpha: isDark ? 0.04 : 0.08),
            Colors.transparent,
            Colors.white.withValues(alpha: isDark ? 0.02 : 0.04),
          ],
          stops: const [0.0, 0.5, 1.0],
        ),
      ),
    );
  }
}
