import 'dart:async';

import 'package:flutter/material.dart';

import '../../../../core/theme/app_theme.dart';
import '../../../../core/widgets/brutal.dart';
import '../../domain/entities/negotiation_entities.dart';
import '../bloc/negotiation_bloc.dart' show offerTtl;

/// Tarjeta flotante de oferta de un conductor.
/// Entra deslizándose, muestra una barra de tiempo restante y permite
/// aceptar, contraofertar (-$500) o descartar.
class OfferCard extends StatefulWidget {
  const OfferCard({
    super.key,
    required this.offer,
    required this.isBest,
    required this.onAccept,
    required this.onCounter,
    required this.onDismiss,
  });

  final DriverOffer offer;
  final bool isBest;
  final VoidCallback onAccept;
  final ValueChanged<int> onCounter;
  final VoidCallback onDismiss;

  static const int counterStep = 500;

  @override
  State<OfferCard> createState() => _OfferCardState();
}

class _OfferCardState extends State<OfferCard> {
  Timer? _tick;

  @override
  void initState() {
    super.initState();
    _tick = Timer.periodic(const Duration(seconds: 1), (_) {
      if (mounted) setState(() {});
    });
  }

  @override
  void dispose() {
    _tick?.cancel();
    super.dispose();
  }

  double get _remaining {
    final elapsed = DateTime.now().difference(widget.offer.receivedAt);
    return (1 - elapsed.inMilliseconds / offerTtl.inMilliseconds).clamp(0.0, 1.0).toDouble();
  }

  @override
  Widget build(BuildContext context) {
    final o = widget.offer;
    return TweenAnimationBuilder<double>(
      tween: Tween(begin: 0, end: 1),
      duration: const Duration(milliseconds: 380),
      curve: Curves.easeOutBack,
      builder: (context, t, child) => Opacity(
        opacity: t.clamp(0.0, 1.0).toDouble(),
        child: Transform.translate(offset: Offset(0, (1 - t) * 40), child: child),
      ),
      child: Padding(
        padding: const EdgeInsets.only(bottom: 18, right: 4),
        child: BrutalCard(
          color: widget.isBest ? const Color(0xFFE8FFEF) : Colors.white,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              if (widget.isBest)
                Container(
                  margin: const EdgeInsets.only(bottom: 10),
                  padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 3),
                  decoration: BoxDecoration(
                    color: BarboyaColors.leaf,
                    borderRadius: BorderRadius.circular(8),
                    border: Border.all(color: BarboyaColors.ink, width: 2),
                  ),
                  child: const Text('MEJOR PRECIO',
                      style: TextStyle(fontWeight: FontWeight.w900, fontSize: 11, letterSpacing: 0.8)),
                ),
              Row(
                children: [
                  _Avatar(url: o.avatarUrl, name: o.name),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(o.name,
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                            style: const TextStyle(fontWeight: FontWeight.w800, fontSize: 16)),
                        const SizedBox(height: 2),
                        Row(children: [
                          const Icon(Icons.star_rounded, size: 18, color: Color(0xFFE6A700)),
                          Text(' ${o.rating.toStringAsFixed(1)} (${o.ratingCount})',
                              style: const TextStyle(fontWeight: FontWeight.w600)),
                        ]),
                        Text('${o.vehicle} · ${o.plate}',
                            maxLines: 1, overflow: TextOverflow.ellipsis, style: const TextStyle(fontSize: 13)),
                      ],
                    ),
                  ),
                  Column(
                    crossAxisAlignment: CrossAxisAlignment.end,
                    children: [
                      Text(formatCop(o.amountCop),
                          style: const TextStyle(fontWeight: FontWeight.w900, fontSize: 24)),
                      if (o.etaMinutes != null)
                        Text('llega en ${o.etaMinutes} min', style: const TextStyle(fontSize: 12)),
                    ],
                  ),
                ],
              ),
              const SizedBox(height: 12),
              ClipRRect(
                borderRadius: BorderRadius.circular(6),
                child: LinearProgressIndicator(
                  value: _remaining,
                  minHeight: 6,
                  backgroundColor: Colors.black12,
                  color: _remaining > 0.3 ? BarboyaColors.sky : BarboyaColors.coral,
                ),
              ),
              const SizedBox(height: 14),
              Row(
                children: [
                  Expanded(
                    flex: 3,
                    child: BrutalButton(
                      label: 'Aceptar',
                      icon: Icons.check_rounded,
                      color: BarboyaColors.leaf,
                      compact: true,
                      onPressed: widget.onAccept,
                    ),
                  ),
                  const SizedBox(width: 10),
                  Expanded(
                    flex: 3,
                    child: BrutalButton(
                      label: '-${formatCop(OfferCard.counterStep)}',
                      icon: Icons.swap_vert_rounded,
                      compact: true,
                      onPressed: o.amountCop > OfferCard.counterStep
                          ? () => widget.onCounter(o.amountCop - OfferCard.counterStep)
                          : null,
                    ),
                  ),
                  const SizedBox(width: 10),
                  BrutalButton(
                    label: '✕',
                    color: Colors.white,
                    compact: true,
                    onPressed: widget.onDismiss,
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _Avatar extends StatelessWidget {
  const _Avatar({required this.name, this.url});

  final String name;
  final String? url;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: 54,
      height: 54,
      decoration: BoxDecoration(
        shape: BoxShape.circle,
        color: BarboyaColors.sun,
        border: Border.all(color: BarboyaColors.ink, width: 2.5),
        image: url != null ? DecorationImage(image: NetworkImage(url!), fit: BoxFit.cover) : null,
      ),
      alignment: Alignment.center,
      child: url == null
          ? Text(name.isEmpty ? '?' : name[0].toUpperCase(),
              style: const TextStyle(fontWeight: FontWeight.w900, fontSize: 22))
          : null,
    );
  }
}
