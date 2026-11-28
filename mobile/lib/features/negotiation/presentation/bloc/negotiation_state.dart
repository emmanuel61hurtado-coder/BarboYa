part of 'negotiation_bloc.dart';

enum NegotiationStatus { idle, quoting, ready, searching, accepted, failure }

class NegotiationState extends Equatable {
  const NegotiationState({
    this.status = NegotiationStatus.idle,
    this.draft,
    this.quote,
    this.offeredFare = 0,
    this.trip,
    this.offers = const [],
    this.agreedFare,
    this.connected = true,
    this.errorCode,
    this.errorMessage,
  });

  final NegotiationStatus status;
  final TripDraft? draft;
  final FareQuote? quote;
  final int offeredFare;
  final CreatedTrip? trip;
  final List<DriverOffer> offers; // ordenadas por precio ascendente
  final int? agreedFare;
  final bool connected;
  final String? errorCode;
  final String? errorMessage;

  /// ¿La propuesta del cliente está por debajo de lo razonable?
  bool get isBelowSuggested => quote != null && offeredFare < quote!.min;

  NegotiationState copyWith({
    NegotiationStatus? status,
    TripDraft? draft,
    FareQuote? quote,
    int? offeredFare,
    CreatedTrip? trip,
    List<DriverOffer>? offers,
    int? agreedFare,
    bool? connected,
    String? errorCode,
    String? errorMessage,
    bool clearError = false,
  }) {
    return NegotiationState(
      status: status ?? this.status,
      draft: draft ?? this.draft,
      quote: quote ?? this.quote,
      offeredFare: offeredFare ?? this.offeredFare,
      trip: trip ?? this.trip,
      offers: offers ?? this.offers,
      agreedFare: agreedFare ?? this.agreedFare,
      connected: connected ?? this.connected,
      errorCode: clearError ? null : (errorCode ?? this.errorCode),
      errorMessage: clearError ? null : (errorMessage ?? this.errorMessage),
    );
  }

  @override
  List<Object?> get props =>
      [status, draft, quote, offeredFare, trip, offers, agreedFare, connected, errorCode, errorMessage];
}
