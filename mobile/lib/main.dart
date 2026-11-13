import 'package:flutter/material.dart';

import 'core/network/api_client.dart';
import 'core/theme/app_theme.dart';
import 'features/auth/presentation/login_page.dart';
import 'features/home/presentation/pages/home_page.dart';

void main() {
  runApp(const BarboyaApp());
}

class BarboyaApp extends StatefulWidget {
  const BarboyaApp({super.key});

  @override
  State<BarboyaApp> createState() => _BarboyaAppState();
}

class _BarboyaAppState extends State<BarboyaApp> {
  // Cambia baseUrl a la IP/dominio de tu backend (emulador Android: 10.0.2.2).
  final ApiClient _api = ApiClient();
  bool _loggedIn = false;

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Barboya',
      debugShowCheckedModeBanner: false,
      theme: BarboyaTheme.light(),
      home: _loggedIn
          ? HomePage(api: _api)
          : LoginPage(
              api: _api,
              onLoggedIn: (token, role) => setState(() {
                _api.token = token;
                _loggedIn = true;
              }),
            ),
    );
  }
}
