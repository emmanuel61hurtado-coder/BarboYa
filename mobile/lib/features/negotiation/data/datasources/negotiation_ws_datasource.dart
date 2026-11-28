import 'dart:async';
import 'dart:convert';

import 'package:web_socket_channel/web_socket_channel.dart';

import '../../../../core/network/api_client.dart';
import '../../domain/entities/negotiation_entities.dart';

/// Conecta el WebSocket del backend y traduce los mensajes JSON
/// `{"type": ..., "payload": {...}}` a eventos de dominio.
/// Reconecta con espera exponencial si la conexión se cae.
class NegotiationWsDataSource {
  NegotiationWsDataSource(this._api);

  final ApiClient _api;
  final _controller = StreamController<ServerEvent>.broadcast();
  WebSocketChannel? _channel;
  StreamSubscription<dynamic>? _sub;
  Timer? _ping;
  Timer? _retry;
  int _attempt = 0;
  bool _closed = false;

  Stream<ServerEvent> get events => _controller.stream;

  void connect() {
    if (_closed) return;
    _sub?.cancel();
    _channel = WebSocketChannel.connect(_api.wsUri);
    _sub = _channel!.stream.listen(
      _onMessage,
      onError: (_) => _scheduleReconnect(),
      onDone: _scheduleReconnect,
      cancelOnError: true,
    );
    _ping?.cancel();
    _ping = Timer.periodic(const Duration(seconds: 25), (_) {
      _channel?.sink.add(jsonEncode({'type': 'ping', 'payload': {}}));
    });
  }

  void _scheduleReconnect() {
    if (_closed) return;
    _ping?.cancel();
    _controller.add(const ConnectionLost());
    final seconds = (1 << _attempt).clamp(1, 30).toInt();
    _attempt = (_attempt + 1).clamp(0, 5).toInt();
    _retry?.cancel();
    _retry = Timer(Duration(seconds: seconds), connect);
  }

  void _onMessage(dynamic raw) {
    _attempt = 0;
    try {
      final msg = jsonDecode(raw as String) as Map<String, dynamic>;
      final payload = (msg['payload'] ?? {}) as Map<String, dynamic>;
      switch (msg['type']) {
        case 'offer.new':
          _controller.add(OfferReceived(_parseOffer(payload)));
        case 'offer.accepted':
          _controller.add(TripAccepted(
            tripId: payload['trip_id'] as String,
            agreedFareCop: (payload['agreed_fare_cop'] as num).toInt(),
          ));
        case 'offer.rejected':
          _controller.add(OfferRejected((payload['reason'] ?? 'rejected') as String));
        default:
          break; // pong, trip.status, etc.
      }
    } catch (_) {
      // Mensaje malformado: se ignora para no tumbar el stream.
    }
  }

  DriverOffer _parseOffer(Map<String, dynamic> p) {
    final d = (p['driver'] ?? {}) as Map<String, dynamic>;
    return DriverOffer(
      driverId: p['driver_id'] as String,
      name: (d['name'] ?? 'Conductor') as String,
      vehicle: (d['vehicle'] ?? '') as String,
      plate: (d['plate'] ?? '') as String,
      rating: ((d['rating'] ?? 5.0) as num).toDouble(),
      ratingCount: ((d['rating_count'] ?? 0) as num).toInt(),
      avatarUrl: d['avatar_url'] as String?,
      amountCop: (p['amount_cop'] as num).toInt(),
      etaMinutes: (p['eta_minutes'] as num?)?.toInt(),
      receivedAt: DateTime.now(),
    );
  }

  Future<void> dispose() async {
    _closed = true;
    _ping?.cancel();
    _retry?.cancel();
    await _sub?.cancel();
    await _channel?.sink.close();
    await _controller.close();
  }
}
