import 'package:flutter/material.dart';
import 'package:barboya_mobile/core/theme/neon_theme.dart';
import 'package:barboya_mobile/core/services/api_client.dart';

class ProfileScreen extends StatefulWidget {
  const ProfileScreen({super.key});

  @override
  State<ProfileScreen> createState() => _ProfileScreenState();
}

class _ProfileScreenState extends State<ProfileScreen> {
  final ApiClient _apiClient = ApiClient();
  Map<String, dynamic> _profile = {
    "full_name": "Usuario Demostración",
    "email": "usuario@barboya.com",
    "phone": "+573001234567",
    "role": "client"
  };
  bool _isLoading = true;

  @override
  void initState() {
    super.initState();
    _loadProfile();
  }

  Future<void> _loadProfile() async {
    try {
      final data = await _apiClient.get('/users/me');
      setState(() {
        _profile = data;
        _isLoading = false;
      });
    } catch (e) {
      setState(() => _isLoading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: _isLoading
          ? const Center(child: CircularProgressIndicator(color: NeonTheme.primaryCyan))
          : Padding(
              padding: const EdgeInsets.all(24.0),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  const CircleAvatar(
                    radius: 40,
                    backgroundColor: NeonTheme.surfaceDark,
                    child: Icon(Icons.person, size: 50, color: NeonTheme.primaryCyan),
                  ),
                  const SizedBox(height: 16),
                  Text(
                    _profile['full_name'] ?? 'Usuario BarboYa',
                    textAlign: TextAlign.center,
                    style: const TextStyle(fontSize: 22, fontWeight: FontWeight.bold, color: NeonTheme.textLight),
                  ),
                  const SizedBox(height: 8),
                  Text(
                    _profile['email'] ?? '',
                    textAlign: TextAlign.center,
                    style: const TextStyle(color: NeonTheme.textMuted),
                  ),
                  const SizedBox(height: 32),
                  Container(
                    padding: const EdgeInsets.all(16),
                    decoration: BoxDecoration(
                      color: NeonTheme.surfaceDark,
                      borderRadius: BorderRadius.circular(12),
                    ),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text('Teléfono: ${_profile['phone'] ?? 'N/A'}', style: const TextStyle(color: NeonTheme.textLight)),
                        const SizedBox(height: 8),
                        Text('Rol: ${_profile['role']?.toUpperCase() ?? 'CLIENTE'}', style: const TextStyle(color: NeonTheme.primaryCyan, fontWeight: FontWeight.bold)),
                      ],
                    ),
                  ),
                  const Spacer(),
                  OutlinedButton(
                    style: OutlinedButton.styleFrom(
                      foregroundColor: Colors.redAccent,
                      side: const BorderSide(color: Colors.redAccent),
                      padding: const EdgeInsets.symmetric(vertical: 14),
                      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                    ),
                    onPressed: () {
                      ScaffoldMessenger.of(context).showSnackBar(
                        const SnackBar(content: Text('Sesión cerrada correctamente')),
                      );
                    },
                    child: const Text('Cerrar Sesión', style: TextStyle(fontWeight: FontWeight.bold)),
                  ),
                ],
              ),
            ),
    );
  }
}
