import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

/// P3 Wide Gamut Colors
/// Flutter's Color.from() supports Display P3 color space natively.
/// On HDR screens these colors are richer; on sRGB screens they auto-degrade.
class P3Colors {
  // Accent - warm rose (P3 reds are deeper)
  static const accent = Color.from(
    colorSpace: ColorSpace.displayP3,
    red: 0.82, green: 0.32, blue: 0.42, alpha: 1.0,
  );

  // Glass highlight - cool blue-white
  static const glassHighlight = Color.from(
    colorSpace: ColorSpace.displayP3,
    red: 0.92, green: 0.93, blue: 0.97, alpha: 0.25,
  );

  // Glass border - subtle cold
  static const glassBorder = Color.from(
    colorSpace: ColorSpace.displayP3,
    red: 0.85, green: 0.87, blue: 0.95, alpha: 0.20,
  );

  // Aurora glow - teal
  static const auroraTeal = Color.from(
    colorSpace: ColorSpace.displayP3,
    red: 0.40, green: 0.80, blue: 0.75, alpha: 0.06,
  );

  // Aurora glow - violet
  static const auroraViolet = Color.from(
    colorSpace: ColorSpace.displayP3,
    red: 0.65, green: 0.45, blue: 0.85, alpha: 0.05,
  );

  // Prismatic - red component
  static const prismRed = Color.from(
    colorSpace: ColorSpace.displayP3,
    red: 0.90, green: 0.72, blue: 0.65, alpha: 0.06,
  );

  // Prismatic - blue component
  static const prismBlue = Color.from(
    colorSpace: ColorSpace.displayP3,
    red: 0.65, green: 0.72, blue: 0.90, alpha: 0.06,
  );

  // Prismatic - green component
  static const prismGreen = Color.from(
    colorSpace: ColorSpace.displayP3,
    red: 0.65, green: 0.88, blue: 0.72, alpha: 0.05,
  );
}

/// Material You Theme
class AppTheme {
  // Seed color for M3 dynamic color
  static const _seed = Color(0xFF6750A4);

  static ThemeData light() {
    final colorScheme = ColorScheme.fromSeed(
      seedColor: _seed,
      brightness: Brightness.light,
    );
    return _build(colorScheme);
  }

  static ThemeData dark() {
    final colorScheme = ColorScheme.fromSeed(
      seedColor: _seed,
      brightness: Brightness.dark,
    );
    return _build(colorScheme);
  }

  static ThemeData _build(ColorScheme colorScheme) {
    final textTheme = GoogleFonts.notoSansScTextTheme(
      ThemeData(colorScheme: colorScheme).textTheme,
    );

    return ThemeData(
      useMaterial3: true,
      colorScheme: colorScheme,
      textTheme: textTheme,

      // AppBar
      appBarTheme: AppBarTheme(
        centerTitle: true,
        elevation: 0,
        scrolledUnderElevation: 1,
        backgroundColor: colorScheme.surface,
        foregroundColor: colorScheme.onSurface,
      ),

      // Cards
      cardTheme: CardTheme(
        elevation: 0,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(16),
          side: BorderSide(
            color: colorScheme.outlineVariant.withValues(alpha: 0.3),
          ),
        ),
      ),

      // Input
      inputDecorationTheme: InputDecorationTheme(
        filled: true,
        fillColor: colorScheme.surfaceContainerHighest.withValues(alpha: 0.3),
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: BorderSide.none,
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: BorderSide(color: colorScheme.primary, width: 2),
        ),
      ),

      // FAB
      floatingActionButtonTheme: FloatingActionButtonThemeData(
        elevation: 2,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(16),
        ),
      ),

      // Navigation
      navigationBarTheme: NavigationBarThemeData(
        elevation: 0,
        height: 72,
        labelBehavior: NavigationDestinationLabelBehavior.onlyShowSelected,
        indicatorColor: colorScheme.secondaryContainer,
      ),

      // Dialog
      dialogTheme: DialogTheme(
        elevation: 3,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(28),
        ),
      ),

      // Divider
      dividerTheme: DividerThemeData(
        color: colorScheme.outlineVariant.withValues(alpha: 0.3),
        thickness: 0.5,
      ),
    );
  }
}
