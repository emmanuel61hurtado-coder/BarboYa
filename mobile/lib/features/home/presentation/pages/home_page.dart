import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';

import '../../../../core/network/api_client.dart';
import '../../../../core/theme/app_theme.dart';
import '../../../../core/widgets/brutal.dart';
import '../../../negotiation/data/repositories/negotiation_repository_impl.dart';
import '../../../negotiation/domain/entities/negotiation_entities.dart';
import '../../../negotiation/presentation/bloc/negotiation_bloc.dart';
import '../../../negotiation/presentation/pages/pon_tu_precio_page.dart';

/// Referencia local con coordenadas.
/// ⚠️ Coordenadas APROXIMADAS de ejemplo alrededor del centro de Barbosa.
/// Reemplázalas con datos reales (panel admin / geocodificación) antes de producción.
class LocalPlace {
  const LocalPlace(this.label, this.lat, this.lng);
  final String label;
  final double lat;
  final double lng;
}

const _places = <LocalPlace>[
  LocalPlace('Frente al Parque Principal', 5.9333, -73.6208),
  LocalPlace('Al lado del D1', 5.9345, -73.6190),
  LocalPlace('Terminal de transporte', 5.9310, -73.6235),
  LocalPlace('Hospital', 5.9360, -73.6170),
];

enum _Mode { transport, delivery }

/// Home unificado. Flujo de 3 toques: modo -> destino -> "Pedir".
class HomePage extends StatefulWidget {
  const HomePage({super.key, required this.api});

  final ApiClient api;

  @override
  State<HomePage> createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> {
  _Mode _mode = _Mode.transport;
  LocalPlace _pickup = _places.first; // GPS: sustituir por geolocator cuando esté integrado
  LocalPlace? _dropoff;
  final _dropCtrl = TextEditingController();

  @override
  void dispose() {
    _dropCtrl.dispose();
    super.dispose();
  }

  void _request() {
    final drop = _dropoff;
    if (drop == null) return;
    final draft = TripDraft(
      serviceType: _mode == _Mode.transport ? 'passenger' : 'delivery',
      pickupLat: _pickup.lat,
      pickupLng: _pickup.lng,
      pickupNote: _pickup.label,
      dropoffLat: drop.lat,
      dropoffLng: drop.lng,
      dropoffNote: _dropCtrl.text.isNotEmpty ? _dropCtrl.text : drop.label,
    );
    Navigator.of(context).push(MaterialPageRoute<void>(
      builder: (_) => BlocProvider(
        create: (_) => NegotiationBloc(NegotiationRepositoryImpl(widget.api))..add(NegotiationOpened(draft)),
        child: const PonTuPrecioPage(),
      ),
    ));
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.all(20),
          children: [
            const Text('BARBOYA', style: TextStyle(fontSize: 34, fontWeight: FontWeight.w900, letterSpacing: -0.5)),
            const Text('Tu pueblo, a un toque.', style: TextStyle(fontWeight: FontWeight.w600)),
            const SizedBox(height: 20),
            Row(
              children: [
                Expanded(
                  child: _ModeTile(
                    label: 'Transporte',
                    icon: Icons.two_wheeler_rounded,
                    selected: _mode == _Mode.transport,
                    onTap: () => setState(() => _mode = _Mode.transport),
                  ),
                ),
                const SizedBox(width: 14),
                Expanded(
                  child: _ModeTile(
                    label: 'Domicilios',
                    icon: Icons.shopping_bag_rounded,
                    selected: _mode == _Mode.delivery,
                    onTap: () => setState(() => _mode = _Mode.delivery),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 22),
            BrutalCard(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(children: [
                    const Icon(Icons.my_location_rounded, color: BarboyaColors.sky),
                    const SizedBox(width: 10),
                    Expanded(
                      child: Text(_pickup.label, style: const TextStyle(fontWeight: FontWeight.w800, fontSize: 16)),
                    ),
                  ]),
                  const SizedBox(height: 14),
                  TextField(
                    controller: _dropCtrl,
                    decoration: InputDecoration(
                      hintText: _mode == _Mode.transport ? '¿A dónde vas?' : '¿Dónde entregamos?',
                      prefixIcon: const Icon(Icons.place_rounded, color: BarboyaColors.coral),
                    ),
                  ),
                  const SizedBox(height: 12),
                  const Text('Referencias rápidas', style: TextStyle(fontWeight: FontWeight.w700, fontSize: 12)),
                  const SizedBox(height: 8),
                  Wrap(
                    spacing: 8,
                    runSpacing: 8,
                    children: [
                      for (final p in _places)
                        ChoiceChip(
                          label: Text(p.label, style: const TextStyle(fontWeight: FontWeight.w700)),
                          selected: _dropoff == p,
                          selectedColor: BarboyaColors.sun,
                          side: const BorderSide(color: BarboyaColors.ink, width: 2),
                          onSelected: (_) => setState(() {
                            _dropoff = p;
                            _dropCtrl.text = p.label;
                          }),
                        ),
                    ],
                  ),
                ],
              ),
            ),
            const SizedBox(height: 24),
            BrutalButton(
              label: _mode == _Mode.transport ? 'Pedir transporte' : 'Pedir domicilio',
              icon: Icons.arrow_forward_rounded,
              onPressed: _dropoff == null ? null : _request,
            ),
          ],
        ),
      ),
    );
  }
}

class _ModeTile extends StatelessWidget {
  const _ModeTile({required this.label, required this.icon, required this.selected, required this.onTap});

  final String label;
  final IconData icon;
  final bool selected;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 180),
        padding: const EdgeInsets.symmetric(vertical: 18),
        decoration: BoxDecoration(
          color: selected ? BarboyaColors.sun : Colors.white,
          borderRadius: BorderRadius.circular(BarboyaTheme.radius),
          border: Border.all(color: BarboyaColors.ink, width: BarboyaTheme.border),
          boxShadow: selected
              ? const [BoxShadow(color: BarboyaColors.ink, offset: BarboyaTheme.shadowOffset)]
              : const [],
        ),
        child: Column(
          children: [
            Icon(icon, size: 34),
            const SizedBox(height: 6),
            Text(label, style: const TextStyle(fontWeight: FontWeight.w900, fontSize: 16)),
          ],
        ),
      ),
    );
  }
}
