import 'package:flutter/material.dart';
import 'package:barboya_mobile/core/theme/neon_theme.dart';
import 'package:barboya_mobile/core/services/api_client.dart';

class ServicesScreen extends StatefulWidget {
  const ServicesScreen({super.key});

  @override
  State<ServicesScreen> createState() => _ServicesScreenState();
}

class _ServicesScreenState extends State<ServicesScreen> {
  final ApiClient _apiClient = ApiClient();
  List<dynamic> _services = [];
  bool _isLoading = true;

  @override
  void initState() {
    super.initState();
    _loadServices();
  }

  Future<void> _loadServices() async {
    try {
      final data = await _apiClient.get('/services/');
      setState(() {
        _services = data;
        _isLoading = false;
      });
    } catch (e) {
      setState(() {
        _services = [
          {"id": "srv-01", "name": "Plomería Residencial Express", "category": "Hogar", "price": 45000.0, "provider_name": "Carlos El Plomero", "rating": 4.8},
          {"id": "srv-02", "name": "Limpieza Profunda Apartamento", "category": "Limpieza", "price": 80000.0, "provider_name": "CleanPro S.A.S.", "rating": 4.9}
        ];
        _isLoading = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: _isLoading
          ? const Center(child: CircularProgressIndicator(color: NeonTheme.primaryCyan))
          : ListView.builder(
              padding: const EdgeInsets.all(16),
              itemCount: _services.length,
              itemBuilder: (context, index) {
                final service = _services[index];
                return Card(
                  color: NeonTheme.surfaceDark,
                  margin: const EdgeInsets.only(bottom: 12),
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                  child: ListTile(
                    title: Text(service['name'], style: const TextStyle(fontWeight: FontWeight.bold, color: NeonTheme.textLight)),
                    subtitle: Text('Categoría: ${service['category']} | ⭐ ${service['rating']}', style: const TextStyle(color: NeonTheme.textMuted)),
                    trailing: Text('\$${service['price']} COP', style: const TextStyle(color: NeonTheme.primaryCyan, fontWeight: FontWeight.bold)),
                    onTap: () {
                      ScaffoldMessenger.of(context).showSnackBar(
                        SnackBar(content: Text('Solicitando ${service['name']}...'))
                      );
                    },
                  ),
                );
              },
            ),
    );
  }
}
