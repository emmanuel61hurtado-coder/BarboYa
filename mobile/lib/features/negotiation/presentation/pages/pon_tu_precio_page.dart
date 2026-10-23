import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_bloc/flutter_bloc.dart';

import '../../../../core/theme/app_theme.dart';
import '../../../../core/widgets/brutal.dart';
import '../bloc/negotiation_bloc.dart';
import '../widgets/offer_card.dart';

/// Pantalla "Pon Tu Precio": el cliente propone su tarifa, la publica y ve
/// llegar en tiempo real las ofertas de los conductores cercanos.
class PonTuPrecioPage extends StatelessWidget {
  const PonTuPrecioPage({super.key});

  @override
  Widget build(BuildContext context) {
    return BlocConsumer<NegotiationBloc, NegotiationState>(
      listenWhen: (a, b) => a.offers.length < b.offers.length || a.errorMessage != b.errorMessage,
      listener: (context, state) {
        // Háptico fuerte cuando entra una oferta nueva.
        HapticFeedback.heavyImpact();
        final msg = state.errorMessage;
        if (msg != null) {
          ScaffoldMessenger.of(context)
            ..hideCurrentSnackBar()
            ..showSnackBar(SnackBar(
              content: Text(msg, style: const TextStyle(fontWeight: FontWeight.w700)),
              backgroundColor: BarboyaColors.coral,
            ));
        }
      },
      builder: (context, state) {
        return Scaffold(
          appBar: AppBar(
            title: const Text('Pon tu precio', style: TextStyle(fontWeight: FontWeight.w900)),
          ),
          body: SafeArea(
            child: Column(
              children: [
                if (!state.connected) const _OfflineBanner(),
                Expanded(
                  child: AnimatedSwitcher(
                    duration: const Duration(milliseconds: 280),
                    child: switch (state.status) {
                      NegotiationStatus.idle || NegotiationStatus.quoting =>
                        const Center(key: ValueKey('q'), child: CircularProgressIndicator(color: BarboyaColors.ink)),
                      NegotiationStatus.ready => _PriceSetter(key: const ValueKey('r'), state: state),
                      NegotiationStatus.searching => _OffersList(key: const ValueKey('s'), state: state),
                      NegotiationStatus.accepted => _AcceptedView(key: const ValueKey('a'), state: state),
                      NegotiationStatus.failure => _FailureView(key: const ValueKey('f'), state: state),
                    },
                  ),
                ),
              ],
            ),
          ),
        );
      },
    );
  }
}

class _OfflineBanner extends StatelessWidget {
  const _OfflineBanner();

  @override
  Widget build(BuildContext context) => Container(
        width: double.infinity,
        color: BarboyaColors.coral,
        padding: const EdgeInsets.symmetric(vertical: 6),
        child: const Text('Sin conexión · reconectando…',
            textAlign: TextAlign.center, style: TextStyle(fontWeight: FontWeight.w800)),
      );
}

// ---------------------------------------------------------------- 1. fijar precio
class _PriceSetter extends StatelessWidget {
  const _PriceSetter({super.key, required this.state});

  final NegotiationState state;

  @override
  Widget build(BuildContext context) {
    final bloc = context.read<NegotiationBloc>();
    final q = state.quote!;
    final d = state.draft!;
    final fraction = ((state.offeredFare - q.min) / (q.max - q.min)).clamp(0.0, 1.0).toDouble();

    return ListView(
      padding: const EdgeInsets.all(20),
      children: [
        BrutalCard(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              _RouteRow(icon: Icons.trip_origin, color: BarboyaColors.sky, text: d.pickupNote ?? 'Mi ubicación'),
              const Padding(
                padding: EdgeInsets.only(left: 11),
                child: SizedBox(height: 14, child: VerticalDivider(color: BarboyaColors.ink, thickness: 2)),
              ),
              _RouteRow(icon: Icons.place_rounded, color: BarboyaColors.coral, text: d.dropoffNote ?? 'Destino'),
              const SizedBox(height: 8),
              Text('${(q.distanceM / 1000).toStringAsFixed(1)} km aprox.',
                  style: const TextStyle(fontWeight: FontWeight.w600)),
            ],
          ),
        ),
        const SizedBox(height: 22),
        BrutalCard(
          color: BarboyaColors.sun,
          child: Column(
            children: [
              const Text('TU PRECIO', style: TextStyle(fontWeight: FontWeight.w900, letterSpacing: 1.4)),
              const SizedBox(height: 4),
              AnimatedSwitcher(
                duration: const Duration(milliseconds: 150),
                transitionBuilder: (c, a) => ScaleTransition(scale: a, child: c),
                child: Text(
                  formatCop(state.offeredFare),
                  key: ValueKey(state.offeredFare),
                  style: const TextStyle(fontSize: 52, fontWeight: FontWeight.w900),
                ),
              ),
              const SizedBox(height: 8),
              Row(
                children: [
                  Expanded(
                    child: BrutalButton(
                      label: '-${formatCop(NegotiationBloc.step)}',
                      color: Colors.white,
                      onPressed: () => bloc.add(const FareAdjusted(-NegotiationBloc.step)),
                    ),
                  ),
                  const SizedBox(width: 14),
                  Expanded(
                    child: BrutalButton(
                      label: '+${formatCop(NegotiationBloc.step)}',
                      color: Colors.white,
                      onPressed: () => bloc.add(const FareAdjusted(NegotiationBloc.step)),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 18),
              _RangeBar(fraction: fraction),
              const SizedBox(height: 6),
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Text(formatCop(q.min), style: const TextStyle(fontSize: 12, fontWeight: FontWeight.w700)),
                  Text('Recomendado ${formatCop(q.suggested)}',
                      style: const TextStyle(fontSize: 12, fontWeight: FontWeight.w800)),
                  Text(formatCop(q.max), style: const TextStyle(fontSize: 12, fontWeight: FontWeight.w700)),
                ],
              ),
            ],
          ),
        ),
        if (state.isBelowSuggested)
          Padding(
            padding: const EdgeInsets.only(top: 14),
            child: Text(
              'Tu precio está por debajo de lo habitual: puede tardar más en llegar una oferta.',
              style: TextStyle(fontWeight: FontWeight.w700, color: Colors.deepOrange.shade800),
            ),
          ),
        const SizedBox(height: 26),
        BrutalButton(
          label: 'Publicar mi precio',
          icon: Icons.bolt_rounded,
          onPressed: () => bloc.add(const TripRequested()),
        ),
      ],
    );
  }
}

class _RouteRow extends StatelessWidget {
  const _RouteRow({required this.icon, required this.color, required this.text});

  final IconData icon;
  final Color color;
  final String text;

  @override
  Widget build(BuildContext context) => Row(
        children: [
          Icon(icon, color: color, size: 24),
          const SizedBox(width: 10),
          Expanded(child: Text(text, style: const TextStyle(fontWeight: FontWeight.w800, fontSize: 16))),
        ],
      );
}

class _RangeBar extends StatelessWidget {
  const _RangeBar({required this.fraction});

  final double fraction;

  @override
  Widget build(BuildContext context) {
    return LayoutBuilder(builder: (context, c) {
      const knob = 22.0;
      return SizedBox(
        height: knob,
        child: Stack(
          alignment: Alignment.centerLeft,
          children: [
            Container(
              height: 10,
              decoration: BoxDecoration(
                color: Colors.white,
                borderRadius: BorderRadius.circular(8),
                border: Border.all(color: BarboyaColors.ink, width: 2),
              ),
            ),
            AnimatedPositioned(
              duration: const Duration(milliseconds: 160),
              left: (c.maxWidth - knob) * fraction,
              child: Container(
                width: knob,
                height: knob,
                decoration: BoxDecoration(
                  color: BarboyaColors.ink,
                  shape: BoxShape.circle,
                  border: Border.all(color: Colors.white, width: 3),
                ),
              ),
            ),
          ],
        ),
      );
    });
  }
}

// ---------------------------------------------------------------- 2. ofertas en vivo
class _OffersList extends StatelessWidget {
  const _OffersList({super.key, required this.state});

  final NegotiationState state;

  @override
  Widget build(BuildContext context) {
    final bloc = context.read<NegotiationBloc>();
    final offers = state.offers;

    return ListView(
      padding: const EdgeInsets.fromLTRB(20, 16, 20, 30),
      children: [
        _PinCard(pin: state.trip?.pin ?? '----', subtitle: 'Díctalo al conductor al iniciar'),
        const SizedBox(height: 20),
        Row(
          children: [
            const SizedBox(
                width: 18, height: 18, child: CircularProgressIndicator(strokeWidth: 3, color: BarboyaColors.ink)),
            const SizedBox(width: 10),
            Expanded(
              child: Text(
                offers.isEmpty
                    ? 'Buscando conductores cerca… ${formatCop(state.offeredFare)}'
                    : '${offers.length} ${offers.length == 1 ? 'oferta' : 'ofertas'} · ordenadas por precio',
                style: const TextStyle(fontWeight: FontWeight.w800, fontSize: 16),
              ),
            ),
          ],
        ),
        const SizedBox(height: 16),
        for (var i = 0; i < offers.length; i++)
          OfferCard(
            key: ValueKey('${offers[i].driverId}-${offers[i].amountCop}'),
            offer: offers[i],
            isBest: i == 0 && offers.length > 1,
            onAccept: () => bloc.add(OfferAcceptedByCustomer(offers[i])),
            onCounter: (amount) => bloc.add(OfferCountered(offers[i], amount)),
            onDismiss: () => bloc.add(OfferDismissed(offers[i].driverId)),
          ),
      ],
    );
  }
}

class _PinCard extends StatelessWidget {
  const _PinCard({required this.pin, required this.subtitle});

  final String pin;
  final String subtitle;

  @override
  Widget build(BuildContext context) {
    return BrutalCard(
      color: BarboyaColors.ink,
      child: Row(
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text('TU PIN',
                    style: TextStyle(color: BarboyaColors.sun, fontWeight: FontWeight.w900, letterSpacing: 1.4)),
                Text(subtitle, style: const TextStyle(color: Colors.white70, fontSize: 12)),
              ],
            ),
          ),
          Text(
            pin.split('').join(' '),
            style: const TextStyle(color: Colors.white, fontSize: 34, fontWeight: FontWeight.w900),
          ),
        ],
      ),
    );
  }
}

// ---------------------------------------------------------------- 3. aceptado
class _AcceptedView extends StatelessWidget {
  const _AcceptedView({super.key, required this.state});

  final NegotiationState state;

  @override
  Widget build(BuildContext context) {
    return ListView(
      padding: const EdgeInsets.all(20),
      children: [
        BrutalCard(
          color: BarboyaColors.leaf,
          child: Column(
            children: [
              const Icon(Icons.check_circle_rounded, size: 56),
              const SizedBox(height: 8),
              const Text('¡Conductor en camino!', style: TextStyle(fontWeight: FontWeight.w900, fontSize: 22)),
              const SizedBox(height: 4),
              Text('Precio acordado: ${formatCop(state.agreedFare ?? state.offeredFare)}',
                  style: const TextStyle(fontWeight: FontWeight.w800, fontSize: 16)),
            ],
          ),
        ),
        const SizedBox(height: 20),
        _PinCard(pin: state.trip?.pin ?? '----', subtitle: 'Díctalo para iniciar el viaje o recibir tu pedido'),
      ],
    );
  }
}

class _FailureView extends StatelessWidget {
  const _FailureView({super.key, required this.state});

  final NegotiationState state;

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: BrutalCard(
          color: const Color(0xFFFFE3E0),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Icon(Icons.error_outline_rounded, size: 44),
              const SizedBox(height: 8),
              Text(state.errorMessage ?? 'Algo salió mal',
                  textAlign: TextAlign.center, style: const TextStyle(fontWeight: FontWeight.w800, fontSize: 16)),
              const SizedBox(height: 16),
              BrutalButton(
                label: 'Volver',
                onPressed: () => Navigator.of(context).maybePop(),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
