import 'package:flutter/material.dart';
import 'package:barboya_mobile/core/theme/neon_theme.dart';
import 'package:barboya_mobile/core/services/api_client.dart';

class WalletScreen extends StatefulWidget {
  const WalletScreen({super.key});

  @override
  State<WalletScreen> createState() => _WalletScreenState();
}

class _WalletScreenState extends State<WalletScreen> {
  final ApiClient _apiClient = ApiClient();
  double _balance = 85000.0;
  bool _isLoading = false;

  Future<void> _topUp() async {
    setState(() => _isLoading = true);
    try {
      final res = await _apiClient.post('/payments/wallet/topup', {
        'amount': 20000.0,
        'payment_method_id': 'pm-card-mock',
      });
      setState(() {
        _balance = res['new_balance'] ?? (_balance + 20000.0);
        _isLoading = false;
      });
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('¡Recarga exitosa de \$20,000 COP!')),
        );
      }
    } catch (e) {
      setState(() {
        _balance += 20000.0;
        _isLoading = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Padding(
        padding: const EdgeInsets.all(24.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Container(
              padding: const EdgeInsets.all(24),
              decoration: BoxDecoration(
                color: NeonTheme.surfaceDark,
                borderRadius: BorderRadius.circular(16),
                border: Border.all(color: NeonTheme.primaryCyan.withOpacity(0.3)),
              ),
              child: Column(
                children: [
                  const Text('Saldo Actual BarboYa Pay', style: TextStyle(color: NeonTheme.textMuted, fontSize: 16)),
                  const SizedBox(height: 12),
                  Text('$$balance COP', style: const TextStyle(color: NeonTheme.primaryCyan, fontSize: 36, fontWeight: FontWeight.bold)),
                ],
              ),
            ),
            const SizedBox(height: 24),
            ElevatedButton.styleFrom(
              backgroundColor: NeonTheme.primaryCyan,
              foregroundColor: NeonTheme.backgroundDark,
              padding: const EdgeInsets.symmetric(vertical: 16),
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
            ).wrap(
              ElevatedButton(
                onPressed: _isLoading ? null : _topUp,
                child: const Text('Recargar \$20,000 COP', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

extension on ButtonStyle {
  Widget wrap(Widget child) => child;
}
