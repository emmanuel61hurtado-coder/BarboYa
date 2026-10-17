import 'package:flutter/material.dart';
import 'package:barboya_mobile/core/theme/neon_theme.dart';

void main() {
  runApp(const BarboYaApp());
}

class BarboYaApp extends StatelessWidget {
  const BarboYaApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'BarboYa Súper App',
      debugShowCheckedModeBanner: false,
      theme: NeonTheme.darkTheme,
      home: const MainHomeScreen(),
    );
  }
}

class MainHomeScreen extends StatefulWidget {
  const MainHomeScreen({super.key});

  @override
  State<MainHomeScreen> createState() => _MainHomeScreenState();
}

class _MainHomeScreenState extends State<MainHomeScreen> {
  int _currentIndex = 0;

  final List<Widget> _screens = [
    const Center(child: Text('🗺️ Mapa Ciudad Virtual (Ciudad Neón)', style: TextStyle(fontSize: 18))),
    const Center(child: Text('🛠️ Catálogo de Servicios y Profesionales', style: TextStyle(fontSize: 18))),
    const Center(child: Text('💰 Billetera y Pagos BarboYa', style: TextStyle(fontSize: 18))),
    const Center(child: Text('👤 Perfil y Configuración', style: TextStyle(fontSize: 18))),
  ];

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('BARBOYA V2'),
      ),
      body: _screens[_currentIndex],
      bottomNavigationBar: BottomNavigationBar(
        currentIndex: _currentIndex,
        onTap: (index) => setState(() => _currentIndex = index),
        type: BottomNavigationBarType.fixed,
        backgroundColor: NeonTheme.surfaceDark,
        selectedItemColor: NeonTheme.primaryCyan,
        unselectedItemColor: NeonTheme.textMuted,
        items: const [
          BottomNavigationBarItem(icon: Icon(Icons.map), label: 'Mapa'),
          BottomNavigationBarItem(icon: Icon(Icons.room_service), label: 'Servicios'),
          BottomNavigationBarItem(icon: Icon(Icons.account_balance_wallet), label: 'Billetera'),
          BottomNavigationBarItem(icon: Icon(Icons.person), label: 'Perfil'),
        ],
      ),
    );
  }
}
