import '../entities/negotiation_entities.dart';

abstract class NegotiationRepository {
  /// Flujo de eventos en tiempo real (ofertas, aceptación, caída de conexión).
  Stream<ServerEvent> get events;

  Future<void> connect();

  Future<FareQuote> quote(TripDraft draft);

  /// Crea el viaje con el precio que propone el cliente ("Pon tu precio").
  Future<CreatedTrip> createTrip(TripDraft draft, int offeredFareCop);

  /// El cliente responde a un conductor con otro monto.
  Future<void> counter({required String tripId, required String driverId, required int amountCop});

  /// El cliente acepta a un conductor al monto indicado.
  Future<void> accept({required String tripId, required String driverId, required int amountCop});

  Future<void> dispose();
}
