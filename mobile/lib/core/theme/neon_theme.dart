import 'package:flutter/material.dart';

class NeonTheme {
  // Paleta Ciudad Neón
  static const Color backgroundDark = Color(0xFF0A0F1D);
  static const Color surfaceDark = Color(0xFF131B2E);
  static const Color primaryCyan = Color(0xFF00E5FF);
  static const Color accentViolet = Color(0xFF9D4EDD);
  static const Color successLime = Color(0xFF00F5D4);
  static const Color warningAmber = Color(0xFFFFB703);
  static const Color textLight = Color(0xFFF8F9FA);
  static const Color textMuted = Color(0xFF8D99AE);

  static ThemeData get darkTheme {
    return ThemeData(
      useMaterial3: true,
      brightness: Brightness.dark,
      scaffoldBackgroundColor: backgroundDark,
      colorScheme: const ColorScheme.dark(
        background: backgroundDark,
        surface: surfaceDark,
        primary: primaryCyan,
        secondary: accentViolet,
        tertiary: successLime,
        error: Color(0xFFEF476F),
      ),
      fontFamily: 'Inter',
      appBarTheme: const AppBarTheme(
        backgroundColor: surfaceDark,
        elevation: 0,
        centerTitle: true,
        titleTextStyle: TextStyle(
          fontFamily: 'SpaceGrotesk',
          fontSize: 20,
          fontWeight: FontWeight.bold,
          color: textLight,
        ),
      ),
    );
  }
}
