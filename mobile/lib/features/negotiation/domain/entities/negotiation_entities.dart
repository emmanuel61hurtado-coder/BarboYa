import 'package:equatable/equatable.dart';

/// Datos para pedir un viaje/domicilio (lo que escribe el cliente en el Home).
class TripDraft extends Equatable {
  const TripDraft({
    required this.serviceType,
    required this.pickupLat,
    required this.pickupLng,
    required this.dropoffLat,
    required this.dropoffLng,
    this.pickupNote,
    this.dropoffNote,
    this.paymentMethod = 'cash',
  });

  final String serviceType; // passenger | delivery | errand
  final double pickupLat;
  final double pickupLng;
  final double dropoffLat;
  final double dropoffLng;
  final String? pickupNote; // "frente al Parque Principal"
  final String? dropoffNote; // "al lado del D1"
  final String paymentMethod; // cash | nequi | daviplata

  @override
  List<Object?> get props =>
      [serviceType, pickupLat, pickupLng, dropoffLat, dropoffLng, pickupNote, dropoffNote, paymentMethod];
}

class FareQuote extends Equatable {
  const FareQuote({required this.suggested, required this.min, required this.max, required this.distanceM});

  final int suggested;
  final int min;
  final int max;
  final int distanceM;

  @override
  List<Object?> get props => [suggested, min, max, distanceM];
}

class CreatedTrip extends Equatable {
  const CreatedTrip({required this.id, required this.pin, required this.shareUrl});

  final String id;
  final String pin; // 4 dígitos que el cliente dicta al conductor
  final String shareUrl;

  @override
  List<Object?> get props => [id, pin, shareUrl];
}

/// Tarjeta de oferta de un conductor.
class DriverOffer extends Equatable {
  const DriverOffer({
    required this.driverId,
    required this.name,
    required this.vehicle,
    required this.plate,
    required this.rating,
    required this.ratingCount,
    required this.amountCop,
    this.avatarUrl,
    this.etaMinutes,
    required this.receivedAt,
  });

  final String driverId;
  final String name;
  final String vehicle;
  final String plate;
  final double rating;
  final int ratingCount;
  final int amountCop;
  final String? avatarUrl;
  final int? etaMinutes;
  final DateTime receivedAt;

  DriverOffer copyWith({int? amountCop}) => DriverOffer(
        driverId: driverId,
        name: name,
        vehicle: vehicle,
        plate: plate,
        rating: rating,
        ratingCount: ratingCount,
        amountCop: amountCop ?? this.amountCop,
        avatarUrl: avatarUrl,
        etaMinutes: etaMinutes,
        receivedAt: receivedAt,
      );

  @override
  List<Object?> get props => [driverId, amountCop, etaMinutes, receivedAt];
}

/// Eventos que llegan del servidor por WebSocket (ya traducidos al dominio).
sealed class ServerEvent {
  const ServerEvent();
}

class OfferReceived extends ServerEvent {
  const OfferReceived(this.offer);
  final DriverOffer offer;
}

class TripAccepted extends ServerEvent {
  const TripAccepted({required this.tripId, required this.agreedFareCop});
  final String tripId;
  final int agreedFareCop;
}

class OfferRejected extends ServerEvent {
  const OfferRejected(this.reason);
  final String reason;
}

class ConnectionLost extends ServerEvent {
  const ConnectionLost();
}
