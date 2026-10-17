import 'package:flutter/material.dart';
import 'package:barboya_mobile/core/theme/neon_theme.dart';
import 'package:barboya_mobile/core/services/api_client.dart';

class MapScreen extends StatefulWidget {
  const MapScreen({super.key});

  @override
  State<MapScreen> createState() => _MapScreenState();
}

class _MapScreenState extends State<MapScreen> {
  final ApiClient _apiClient = ApiClient();
  bool _isSearching = false;
  String _selectedVehicle = 'moto';

  Future<void> _requestRide() async {
    setState(() => _isSearching = true);
    try {
      await _apiClient.post('/transport/request', {
        'pickup_lat': 4.60971,
        'pickup_lng': -74.08175,
        'dropoff_lat': 4.65838,
        'dropoff_lng': -74.09391,
        'service_type': _selectedVehicle,
      });
      await Future.delayed(const Duration(seconds: 2));
      setState(() => _isSearching = false);
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('¡Conductor asignado! En camino a recogerte.')),
        );
      }
    } catch (e) {
      await Future.delayed(const Duration(seconds: 2));
      setState(() => _isSearching = false);
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('¡Conductor asignado con éxito (Mock)!')),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Stack(
        children: [
          Container(
            color: NeonTheme.backgroundDark,
            child: const Center(
              child: Text(
                '🗺️ [Mapa Ciudad Neón - GPS Activo]\nSimulador de Movilidad Urbana',
                textAlign: TextAlign.center,
                style: TextStyle(color: NeonTheme.textMuted, fontSize: 16),
              ),
            ),
          ),
          Positioned(
            bottom: 24,
            left: 16,
            right: 16,
            child: Container(
              padding: const EdgeInsets.all(16),
              decoration: BoxDecoration(
                color: NeonTheme.surfaceDark,
                borderRadius: BorderRadius.circular(16),
                border: Border.all(color: NeonTheme.primaryCyan.withOpacity(0.3)),
              ),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  const Text('Selecciona tu viaje', style: TextStyle(color: NeonTheme.textLight, fontSize: 18, fontWeight: FontWeight.bold)),
                  const SizedBox(height: 12),
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceAround,
                    children: [
                      _vehicleOption('Moto', 'moto', Icons.two_wheeler),
                      _vehicleOption('Auto', 'car', Icons.directions_car),
                      _vehicleOption('Envío', 'delivery', Icons.local_shipping),
                    ],
                  ),
                  const SizedBox(height: 16),
                  SizedBox(
                    width: double.infinity,
                    child: ElevatedButton(
                      style: ElevatedButton.styleFrom(
                        backgroundColor: NeonTheme.primaryCyan,
                        foregroundColor: NeonTheme.backgroundDark,
                        padding: const EdgeInsets.symmetric(vertical: 14),
                        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                      ),
                      onPressed: _isSearching ? null : _requestRide,
                      child: _isSearching
                          ? const CircularProgressIndicator(color: NeonTheme.backgroundDark)
                          : const Text('Pedir BarboYa Ahora', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
                    ),
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _vehicleOption(String label, String type, IconData icon) {
    final isSelected = _selectedVehicle == type;
    return GestureDetector(
      onTap: () => setState(() => _selectedVehicle = type),
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
        decoration: BoxDecoration(
          color: isSelected ? NeonTheme.primaryCyan.withOpacity(0.2) : Colors.transparent,
          borderRadius: BorderRadius.circular(10),
          border: Border.all(color: isSelected ? NeonTheme.primaryCyan : NeonTheme.textMuted),
        ),
        child: Column(
          children: [
            Icon(icon, color: isSelected ? NeonTheme.primaryCyan : NeonTheme.textMuted),
            const SizedBox(height: 4),
            Text(label, style: TextStyle(color: isSelected ? NeonTheme.primaryCyan : NeonTheme.textMuted, fontWeight: FontWeight.bold)),
          ],
        ),
      ),
    );
  }
}
