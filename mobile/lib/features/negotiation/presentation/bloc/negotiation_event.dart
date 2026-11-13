part of 'negotiation_bloc.dart';

sealed class NegotiationEvent extends Equatable {
  const NegotiationEvent();

  @override
  List<Object?> get props => [];
}

/// Se abre la pantalla: se cotiza la tarifa recomendada y se conecta el socket.
class NegotiationOpened extends NegotiationEvent {
  const NegotiationOpened(this.draft);
  final TripDraft draft;

  @override
  List<Object?> get props => [draft];
}

/// El cliente sube/baja su propuesta con los botones ±.
class FareAdjusted extends NegotiationEvent {
  const FareAdjusted(this.deltaCop);
  final int deltaCop;

  @override
  List<Object?> get props => [deltaCop];
}

/// "Publicar mi precio": crea el viaje y empieza a recibir ofertas.
class TripRequested extends NegotiationEvent {
  const TripRequested();
}

class OfferAcceptedByCustomer extends NegotiationEvent {
  const OfferAcceptedByCustomer(this.offer);
  final DriverOffer offer;

  @override
  List<Object?> get props => [offer];
}

class OfferCountered extends NegotiationEvent {
  const OfferCountered(this.offer, this.amountCop);
  final DriverOffer offer;
  final int amountCop;

  @override
  List<Object?> get props => [offer, amountCop];
}

class OfferDismissed extends NegotiationEvent {
  const OfferDismissed(this.driverId);
  final String driverId;

  @override
  List<Object?> get props => [driverId];
}

/// Interno: llegó algo del WebSocket.
class _ServerEventReceived extends NegotiationEvent {
  const _ServerEventReceived(this.event);
  final ServerEvent event;
}

/// Interno: limpia ofertas vencidas.
class _OffersPruned extends NegotiationEvent {
  const _OffersPruned();
}
