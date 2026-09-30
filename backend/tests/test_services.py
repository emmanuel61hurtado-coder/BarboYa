import pytest
from app.models.pedido import EstadoPedido
from app.services.state_machine import validate_transition
from app.core.exceptions import DomainException


def test_state_machine_valid_transitions():
    validate_transition(EstadoPedido.CREADO, EstadoPedido.ACEPTADO)
    validate_transition(EstadoPedido.ACEPTADO, EstadoPedido.PREPARANDO)
    validate_transition(EstadoPedido.PREPARANDO, EstadoPedido.LISTO)
    validate_transition(EstadoPedido.LISTO, EstadoPedido.EN_CAMINO)
    validate_transition(EstadoPedido.EN_CAMINO, EstadoPedido.ENTREGADO)


def test_state_machine_cancellation():
    validate_transition(EstadoPedido.CREADO, EstadoPedido.CANCELADO)
    validate_transition(EstadoPedido.ACEPTADO, EstadoPedido.CANCELADO)


def test_state_machine_invalid_transitions():
    with pytest.raises(DomainException) as exc_info:
        validate_transition(EstadoPedido.CREADO, EstadoPedido.ENTREGADO)
    assert exc_info.value.code == "INVALID_STATE_TRANSITION"

    with pytest.raises(DomainException) as exc_info:
        validate_transition(EstadoPedido.ENTREGADO, EstadoPedido.CREADO)
    assert exc_info.value.code == "INVALID_STATE_TRANSITION"
