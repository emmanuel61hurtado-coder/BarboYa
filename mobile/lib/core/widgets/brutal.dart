import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../theme/app_theme.dart';

/// Tarjeta neo-brutalista: borde grueso + sombra dura.
class BrutalCard extends StatelessWidget {
  const BrutalCard({
    super.key,
    required this.child,
    this.color = Colors.white,
    this.padding = const EdgeInsets.all(16),
  });

  final Widget child;
  final Color color;
  final EdgeInsets padding;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: padding,
      decoration: BoxDecoration(
        color: color,
        borderRadius: BorderRadius.circular(BarboyaTheme.radius),
        border: Border.all(color: BarboyaColors.ink, width: BarboyaTheme.border),
        boxShadow: const [
          BoxShadow(color: BarboyaColors.ink, offset: BarboyaTheme.shadowOffset),
        ],
      ),
      child: child,
    );
  }
}

/// Botón con "pulsación" física: se hunde sobre su sombra y vibra al tocar.
class BrutalButton extends StatefulWidget {
  const BrutalButton({
    super.key,
    required this.label,
    required this.onPressed,
    this.color = BarboyaColors.sun,
    this.icon,
    this.compact = false,
  });

  final String label;
  final VoidCallback? onPressed;
  final Color color;
  final IconData? icon;
  final bool compact;

  @override
  State<BrutalButton> createState() => _BrutalButtonState();
}

class _BrutalButtonState extends State<BrutalButton> {
  bool _down = false;

  @override
  Widget build(BuildContext context) {
    final enabled = widget.onPressed != null;
    final shift = _down ? BarboyaTheme.shadowOffset : Offset.zero;
    return Semantics(
      button: true,
      enabled: enabled,
      label: widget.label,
      child: GestureDetector(
        onTapDown: enabled ? (_) => setState(() => _down = true) : null,
        onTapCancel: () => setState(() => _down = false),
        onTapUp: enabled
            ? (_) {
                setState(() => _down = false);
                HapticFeedback.mediumImpact();
                widget.onPressed!();
              }
            : null,
        child: AnimatedContainer(
          duration: const Duration(milliseconds: 90),
          transform: Matrix4.translationValues(shift.dx, shift.dy, 0),
          padding: EdgeInsets.symmetric(
            horizontal: widget.compact ? 12 : 20,
            vertical: widget.compact ? 10 : 16,
          ),
          decoration: BoxDecoration(
            color: enabled ? widget.color : Colors.grey.shade400,
            borderRadius: BorderRadius.circular(14),
            border: Border.all(color: BarboyaColors.ink, width: BarboyaTheme.border),
            boxShadow: _down
                ? const []
                : const [BoxShadow(color: BarboyaColors.ink, offset: BarboyaTheme.shadowOffset)],
          ),
          child: Row(
            mainAxisSize: MainAxisSize.min,
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              if (widget.icon != null) ...[
                Icon(widget.icon, color: BarboyaColors.ink, size: widget.compact ? 18 : 22),
                const SizedBox(width: 8),
              ],
              Flexible(
                child: Text(
                  widget.label,
                  textAlign: TextAlign.center,
                  style: TextStyle(
                    fontWeight: FontWeight.w800,
                    fontSize: widget.compact ? 14 : 17,
                    color: BarboyaColors.ink,
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

String formatCop(int value) {
  final s = value.abs().toString();
  final buf = StringBuffer();
  for (var i = 0; i < s.length; i++) {
    if (i > 0 && (s.length - i) % 3 == 0) buf.write('.');
    buf.write(s[i]);
  }
  return '\$${value < 0 ? '-' : ''}$buf';
}
