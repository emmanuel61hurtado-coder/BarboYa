import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

/// Identidad visual Barboya: Neo-Brutalismo suave.
/// Bordes negros gruesos + sombras duras desplazadas + colores saturados
/// de alto contraste, legibles bajo luz solar directa.
class BarboyaColors {
  static const ink = Color(0xFF111111);
  static const paper = Color(0xFFFFF6DC); // crema cálido (menos reflejo que blanco puro)
  static const sun = Color(0xFFFFC400); // amarillo principal
  static const leaf = Color(0xFF00B35A); // éxito / aceptar
  static const coral = Color(0xFFFF5A4F); // alerta / SOS
  static const sky = Color(0xFF3D7BFF); // información
}

class BarboyaTheme {
  static const double border = 2.5;
  static const double radius = 18;
  static const Offset shadowOffset = Offset(4, 4);

  static ThemeData light() {
    final base = ThemeData(
      useMaterial3: true,
      colorScheme: ColorScheme.fromSeed(
        seedColor: BarboyaColors.sun,
        brightness: Brightness.light,
        primary: BarboyaColors.ink,
        secondary: BarboyaColors.sun,
        surface: BarboyaColors.paper,
      ),
      scaffoldBackgroundColor: BarboyaColors.paper,
    );
    return base.copyWith(
      textTheme: GoogleFonts.spaceGroteskTextTheme(base.textTheme).apply(
        bodyColor: BarboyaColors.ink,
        displayColor: BarboyaColors.ink,
      ),
      appBarTheme: const AppBarTheme(
        backgroundColor: BarboyaColors.paper,
        foregroundColor: BarboyaColors.ink,
        elevation: 0,
        centerTitle: false,
      ),
      inputDecorationTheme: InputDecorationTheme(
        filled: true,
        fillColor: Colors.white,
        border: _outline(),
        enabledBorder: _outline(),
        focusedBorder: _outline(width: 3.5),
        contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 16),
      ),
    );
  }

  static OutlineInputBorder _outline({double width = border}) => OutlineInputBorder(
        borderRadius: BorderRadius.circular(radius - 4),
        borderSide: BorderSide(color: BarboyaColors.ink, width: width),
      );
}
