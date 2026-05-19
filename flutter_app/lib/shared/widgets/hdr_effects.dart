import 'dart:math';
import 'dart:ui';
import 'package:flutter/material.dart';
import '../../core/theme.dart';

/// HDR-enabled animated glass card with prismatic refraction.
/// Uses CustomPainter for GPU-accelerated rendering.
class HdrGlassCard extends StatefulWidget {
  final Widget child;
  final bool enableAnimation;
  final EdgeInsetsGeometry? padding;
  final EdgeInsetsGeometry? margin;
  final BorderRadius borderRadius;

  const HdrGlassCard({
    super.key,
    required this.child,
    this.enableAnimation = true,
    this.padding,
    this.margin,
    this.borderRadius = const BorderRadius.all(Radius.circular(16)),
  });

  @override
  State<HdrGlassCard> createState() => _HdrGlassCardState();
}

class _HdrGlassCardState extends State<HdrGlassCard>
    with SingleTickerProviderStateMixin {
  late AnimationController _controller;

  @override
  void initState() {
    super.initState();
    _controller = AnimationController(
      duration: const Duration(seconds: 8),
      vsync: this,
    );
    if (widget.enableAnimation) {
      _controller.repeat();
    }
  }

  @override
  void didUpdateWidget(HdrGlassCard oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (widget.enableAnimation && !_controller.isAnimating) {
      _controller.repeat();
    } else if (!widget.enableAnimation && _controller.isAnimating) {
      _controller.stop();
    }
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;

    return Container(
      margin: widget.margin,
      child: ClipRRect(
        borderRadius: widget.borderRadius,
        child: BackdropFilter(
          filter: ImageFilter.blur(sigmaX: 16, sigmaY: 16),
          child: Container(
            padding: widget.padding ?? const EdgeInsets.all(16),
            decoration: BoxDecoration(
              color: isDark
                  ? Colors.white.withValues(alpha: 0.06)
                  : Colors.white.withValues(alpha: 0.25),
              borderRadius: widget.borderRadius,
              border: Border.all(
                color: isDark
                    ? Colors.white.withValues(alpha: 0.08)
                    : Colors.white.withValues(alpha: 0.35),
                width: 0.5,
              ),
            ),
            child: Stack(
              children: [
                widget.child,
                // Prismatic refraction overlay
                Positioned.fill(
                  child: IgnorePointer(
                    child: AnimatedBuilder(
                      animation: _controller,
                      builder: (context, _) {
                        return CustomPaint(
                          painter: _PrismaticPainter(
                            progress: _controller.value,
                            isDark: isDark,
                          ),
                        );
                      },
                    ),
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

/// Prismatic refraction — slow-moving rainbow highlights
class _PrismaticPainter extends CustomPainter {
  final double progress;
  final bool isDark;

  _PrismaticPainter({required this.progress, required this.isDark});

  @override
  void paint(Canvas canvas, Size size) {
    final rect = Offset.zero & size;

    // Aurora glow — slow drift
    final auroraGradient = LinearGradient(
      begin: Alignment(-1 + progress * 2, -0.5),
      end: Alignment(1 - progress * 2, 0.5),
      colors: [
        P3Colors.auroraTeal.withValues(alpha: isDark ? 0.06 : 0.03),
        P3Colors.auroraViolet.withValues(alpha: isDark ? 0.05 : 0.02),
        Colors.transparent,
      ],
    );
    canvas.drawRect(rect, Paint()..shader = auroraGradient.createShader(rect));

    // Prismatic bands — rainbow light strip
    final bandX = size.width * (progress * 1.5 - 0.25);
    if (bandX > -size.width * 0.3 && bandX < size.width * 1.3) {
      final prismGradient = LinearGradient(
        begin: Alignment(bandX / size.width - 0.15, 0),
        end: Alignment(bandX / size.width + 0.15, 0),
        colors: [
          Colors.transparent,
          P3Colors.prismRed.withValues(alpha: isDark ? 0.08 : 0.04),
          P3Colors.prismGreen.withValues(alpha: isDark ? 0.06 : 0.03),
          P3Colors.prismBlue.withValues(alpha: isDark ? 0.08 : 0.04),
          Colors.transparent,
        ],
      );
      canvas.drawRect(rect, Paint()..shader = prismGradient.createShader(rect));
    }

    // Gloss highlight — top edge shimmer
    final glossY = size.height * 0.15;
    final glossGradient = LinearGradient(
      begin: Alignment(-1 + progress * 2, 0),
      end: Alignment(-0.5 + progress * 2, 0),
      colors: [
        Colors.transparent,
        Colors.white.withValues(alpha: isDark ? 0.03 : 0.08),
        Colors.transparent,
      ],
    );
    canvas.drawRect(
      Rect.fromLTWH(0, glossY, size.width, 1),
      Paint()..shader = glossGradient.createShader(Rect.fromLTWH(0, glossY, size.width, 1)),
    );
  }

  @override
  bool shouldRepaint(_PrismaticPainter oldDelegate) {
    return oldDelegate.progress != progress || oldDelegate.isDark != isDark;
  }
}

/// AnimatedBuilder helper
class AnimatedBuilder extends AnimatedWidget {
  final Widget Function(BuildContext, Widget?) builder;
  final Widget? child;

  const AnimatedBuilder({
    super.key,
    required super.listenable,
    required this.builder,
    this.child,
  });

  @override
  Widget build(BuildContext context) {
    return builder(context, child);
  }
}
