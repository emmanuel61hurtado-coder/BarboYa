from app.models.pedido import EstadoPedido
from app.core.exceptions import DomainException
from fastapi import status

VALID_TRANSITIONS = {
    EstadoPedido.CREADO: [EstadoPedido.ACEPTADO, EstadoPedido.CANCELADO],
    EstadoPedido.ACEPTADO: [EstadoPedido.PREPARANDO, EstadoPedido.CANCELADO],
    EstadoPedido.PREPARANDO: [EstadoPedido.LISTO, EstadoPedido.CANCELADO],
    EstadoPedido.LISTO: [EstadoPedido.EN_CAMINO],
    EstadoPedido.EN_CAMINO: [EstadoPedido.ENTREGADO],
    EstadoPedido.ENTREGADO: [],
    EstadoPedido.CANCELADO: [],
}


def validate_transition(current: EstadoPedido, target: EstadoPedido) -> None:
    if target not in VALID_TRANSITIONS.get(current, []):
        raise DomainException(
            code="INVALID_STATE_TRANSITION",
            message=f"No se puede cambiar el estado del pedido de {current} a {target}",
            status_code=status.HTTP_409_CONFLICT,
        )
