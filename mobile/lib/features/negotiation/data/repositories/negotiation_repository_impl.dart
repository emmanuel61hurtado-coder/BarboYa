import '../../../../core/network/api_client.dart';
import '../../domain/entities/negotiation_entities.dart';
import '../../domain/repositories/negotiation_repository.dart';
import '../datasources/negotiation_ws_datasource.dart';

class NegotiationRepositoryImpl implements NegotiationRepository {
  NegotiationRepositoryImpl(this._api) : _ws = NegotiationWsDataSource(_api);

  final ApiClient _api;
  final NegotiationWsDataSource _ws;

  @override
  Stream<ServerEvent> get events => _ws.events;

  @override
  Future<void> connect() async => _ws.connect();

  Map<String, dynamic> _geo(double lat, double lng) => {'lat': lat, 'lng': lng};

  @override
  Future<FareQuote> quote(TripDraft d) async {
    final r = await _api.post('/trips/quote', {
      'pickup': _geo(d.pickupLat, d.pickupLng),
      'dropoff': _geo(d.dropoffLat, d.dropoffLng),
      'service_type': d.serviceType,
    });
    return FareQuote(
      suggested: (r['suggested_fare_cop'] as num).toInt(),
      min: (r['min_fare_cop'] as num).toInt(),
      max: (r['max_fare_cop'] as num).toInt(),
      distanceM: (r['distance_m'] as num).toInt(),
    );
  }

  @override
  Future<CreatedTrip> createTrip(TripDraft d, int offeredFareCop) async {
    final r = await _api.post('/trips', {
      'service_type': d.serviceType,
      'pickup': _geo(d.pickupLat, d.pickupLng),
      'pickup_note': d.pickupNote,
      'dropoff': _geo(d.dropoffLat, d.dropoffLng),
      'dropoff_note': d.dropoffNote,
      'offered_fare_cop': offeredFareCop,
      'payment_method': d.paymentMethod,
    });
    return CreatedTrip(
      id: r['id'] as String,
      pin: r['confirmation_pin'] as String,
      shareUrl: r['share_url'] as String,
    );
  }

  @override
  Future<void> counter({required String tripId, required String driverId, required int amountCop}) async {
    await _api.post('/trips/$tripId/counter', {'driver_id': driverId, 'amount_cop': amountCop});
  }

  @override
  Future<void> accept({required String tripId, required String driverId, required int amountCop}) async {
    await _api.post('/trips/$tripId/accept', {'driver_id': driverId, 'amount_cop': amountCop});
  }

  @override
  Future<void> dispose() => _ws.dispose();
}
