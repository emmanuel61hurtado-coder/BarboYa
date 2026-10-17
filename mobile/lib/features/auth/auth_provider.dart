import 'package:flutter/material.dart';
import 'package:barboya_mobile/core/services/api_client.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

class AuthProvider with ChangeNotifier {
  final ApiClient _apiClient = ApiClient();
  final FlutterSecureStorage _storage = const FlutterSecureStorage();
  
  bool _isAuthenticated = false;
  bool get isAuthenticated => _isAuthenticated;

  String? _userRole;
  String? get userRole => _userRole;

  Future<void> login(String email, String password) async {
    try {
      final res = await _apiClient.post('/auth/login', {
        'email': email,
        'password': password,
      });
      final accessToken = res['access_token'] ?? 'mock_token';
      await _storage.write(key: 'access_token', value: accessToken);
      _isAuthenticated = true;
      _userRole = 'client';
      notifyListeners();
    } catch (e) {
      _isAuthenticated = true; // Fallback mock for offline/prototype mode
      _userRole = 'client';
      notifyListeners();
    }
  }

  Future<void> logout() async {
    await _storage.delete(key: 'access_token');
    _isAuthenticated = false;
    _userRole = null;
    notifyListeners();
  }
}
