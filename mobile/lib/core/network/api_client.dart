import 'package:flutter/foundation.dart';
import 'package:http/http.dart' as http;

class ApiException implements Exception {
  ApiException(this.statusCode, this.code, this.message);

  final int statusCode;
  final String? code; // p.ej. INSUFFICIENT_BALANCE, SUBSCRIPTION_EXPIRED
  final String message;

  @override
  String toString() => message;
}

/// Cliente REST mínimo. Cambia [baseUrl] por la IP/dominio de tu backend
/// (en emulador Android, el host local es 10.0.2.2; en web/desktop es localhost).
class ApiClient {
  ApiClient({String? baseUrl, this.token})
      : baseUrl = baseUrl ?? (kIsWeb ? 'http://localhost:8000' : 'http://10.0.2.2:8000');

  final String baseUrl;
  String? token;

  Uri get wsUri {
    final u = Uri.parse(baseUrl);
    return Uri(
      scheme: u.scheme == 'https' ? 'wss' : 'ws',
      host: u.host,
      port: u.hasPort ? u.port : null,
      path: '/ws',
      queryParameters: {'token': token ?? ''},
    );
  }

  Map<String, String> get _headers => {
        'Content-Type': 'application/json',
        if (token != null) 'Authorization': 'Bearer $token',
      };

  Future<Map<String, dynamic>> post(String path, [Map<String, dynamic>? body]) async {
    final res = await http.post(
      Uri.parse('$baseUrl$path'),
      headers: _headers,
      body: jsonEncode(body ?? {}),
    );
    return _decode(res);
  }

  Map<String, dynamic> _decode(http.Response res) {
    final dynamic data = res.body.isEmpty ? <String, dynamic>{} : jsonDecode(utf8.decode(res.bodyBytes));
    if (res.statusCode >= 200 && res.statusCode < 300) {
      return data is Map<String, dynamic> ? data : {'data': data};
    }
    // FastAPI devuelve {"detail": "..."} o {"detail": {"code":..., "message":...}}
    final detail = data is Map ? data['detail'] : null;
    if (detail is Map) {
      throw ApiException(res.statusCode, detail['code'] as String?, (detail['message'] ?? 'Error') as String);
    }
    throw ApiException(res.statusCode, null, detail?.toString() ?? 'Error ${res.statusCode}');
  }
}
