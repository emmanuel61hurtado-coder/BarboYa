import 'dart:async';

import 'package:equatable/equatable.dart';
import 'package:flutter_bloc/flutter_bloc.dart';

import '../../../../core/network/api_client.dart';
import '../../domain/entities/negotiation_entities.dart';
import '../../domain/repositories/negotiation_repository.dart';

part 'negotiation_event.dart';
part 'negotiation_state.dart';

/// Tiempo de vida de una tarjeta de oferta en pantalla.
const offerTtl = Duration(seconds: 120);

/// Puente entre el WebSocket del backend y la UI de "Pon Tu Precio".
class NegotiationBloc extends Bloc<NegotiationEvent, NegotiationState> {
  NegotiationBloc(this._repo) : super(const NegotiationState()) {
    on<NegotiationOpened>(_onOpened);
    on<FareAdjusted>(_onFareAdjusted);
    on<TripRequested>(_onTripRequested);
    on<OfferAcceptedByCustomer>(_onAccept);
    on<OfferCountered>(_onCounter);
    on<OfferDismissed>(_onDismiss);
    on<_ServerEventReceived>(_onServerEvent);
    on<_OffersPruned>(_onPruned);
  }

  final NegotiationRepository _repo;
  StreamSubscription<ServerEvent>? _sub;
  Timer? _pruneTimer;

  static const int step = 500;

  Future<void> _onOpened(NegotiationOpened e, Emitter<NegotiationState> emit) async {
    emit(state.copyWith(status: NegotiationStatus.quoting, draft: e.draft, clearError: true));
    try {
      final quote = await _repo.quote(e.draft);
      emit(state.copyWith(status: NegotiationStatus.ready, quote: quote, offeredFare: quote.suggested));
    } on ApiException catch (ex) {
      emit(state.copyWith(status: NegotiationStatus.failure, errorCode: ex.code, errorMessage: ex.message));
    } catch (_) {
      emit(state.copyWith(status: NegotiationStatus.failure, errorMessage: 'No pudimos calcular la tarifa'));
    }
  }

  void _onFareAdjusted(FareAdjusted e, Emitter<NegotiationState> emit) {
    final q = state.quote;
    if (q == null) return;
    // No se deja bajar de la mitad de lo sugerido ni subir de 2x (evita errores de dedo).
    final next = (state.offeredFare + e.deltaCop).clamp((q.suggested / 2).round(), q.suggested * 2).toInt();
    emit(state.copyWith(offeredFare: next));
  }

  Future<void> _onTripRequested(TripRequested e, Emitter<NegotiationState> emit) async {
    final draft = state.draft;
    if (draft == null || state.status != NegotiationStatus.ready) return;
    emit(state.copyWith(clearError: true));
    try {
      await _repo.connect();
      _sub ??= _repo.events.listen((ev) => add(_ServerEventReceived(ev)));
      final trip = await _repo.createTrip(draft, state.offeredFare);
      _pruneTimer ??= Timer.periodic(const Duration(seconds: 5), (_) => add(const _OffersPruned()));
      emit(state.copyWith(status: NegotiationStatus.searching, trip: trip, offers: const []));
    } on ApiException catch (ex) {
      emit(state.copyWith(status: NegotiationStatus.failure, errorCode: ex.code, errorMessage: ex.message));
    } catch (_) {
      emit(state.copyWith(status: NegotiationStatus.failure, errorMessage: 'No pudimos publicar tu pedido'));
    }
  }

  void _onServerEvent(_ServerEventReceived e, Emitter<NegotiationState> emit) {
    final ev = e.event;
    switch (ev) {
      case OfferReceived(:final offer):
        // Una oferta nueva del mismo conductor reemplaza a la anterior (contraoferta).
        final others = state.offers.where((o) => o.driverId != offer.driverId);
        final sorted = [...others, offer]..sort((a, b) => a.amountCop.compareTo(b.amountCop));
        emit(state.copyWith(offers: sorted));
      case TripAccepted(:final agreedFareCop):
        _pruneTimer?.cancel();
        emit(state.copyWith(status: NegotiationStatus.accepted, agreedFare: agreedFareCop, offers: const []));
      case OfferRejected():
        break; // otro conductor tomó el viaje; para el cliente no cambia nada
      case ConnectionLost():
        emit(state.copyWith(connected: false));
    }
    if (ev is! ConnectionLost && !state.connected) {
      emit(state.copyWith(connected: true));
    }
  }

  Future<void> _onAccept(OfferAcceptedByCustomer e, Emitter<NegotiationState> emit) async {
    final trip = state.trip;
    if (trip == null) return;
    try {
      await _repo.accept(tripId: trip.id, driverId: e.offer.driverId, amountCop: e.offer.amountCop);
      // La confirmación real llega por WebSocket (TripAccepted) para ambos lados.
    } on ApiException catch (ex) {
      // Ej.: el conductor se quedó sin saldo/pase entre la oferta y la aceptación.
      emit(state.copyWith(
        offers: state.offers.where((o) => o.driverId != e.offer.driverId).toList(),
        errorCode: ex.code,
        errorMessage: ex.message,
      ));
    }
  }

  Future<void> _onCounter(OfferCountered e, Emitter<NegotiationState> emit) async {
    final trip = state.trip;
    if (trip == null) return;
    try {
      await _repo.counter(tripId: trip.id, driverId: e.offer.driverId, amountCop: e.amountCop);
      // Se refleja de inmediato en la tarjeta; el conductor responde con su propia oferta.
      final updated = state.offers
          .map((o) => o.driverId == e.offer.driverId ? o.copyWith(amountCop: e.amountCop) : o)
          .toList()
        ..sort((a, b) => a.amountCop.compareTo(b.amountCop));
      emit(state.copyWith(offers: updated));
    } on ApiException catch (ex) {
      emit(state.copyWith(errorCode: ex.code, errorMessage: ex.message));
    }
  }

  void _onDismiss(OfferDismissed e, Emitter<NegotiationState> emit) {
    emit(state.copyWith(offers: state.offers.where((o) => o.driverId != e.driverId).toList()));
  }

  void _onPruned(_OffersPruned e, Emitter<NegotiationState> emit) {
    final now = DateTime.now();
    final alive = state.offers.where((o) => now.difference(o.receivedAt) < offerTtl).toList();
    if (alive.length != state.offers.length) emit(state.copyWith(offers: alive));
  }

  @override
  Future<void> close() async {
    _pruneTimer?.cancel();
    await _sub?.cancel();
    await _repo.dispose();
    return super.close();
  }
}
