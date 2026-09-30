"""
CRUD Tests: Vehiculos
Tests para:
  POST /vehiculos      - crear vehículo (rol REPARTIDOR + ACTIVO)
  GET  /vehiculos/me   - obtener mi vehículo
  POST /vehiculos      - duplicado → 409
  POST /vehiculos      - sin token → 403
"""
import pytest
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.future import select
from tests.conftest import TestingSessionLocal
from app.models.user import User, UserStatus


async def _activate_user(email: str) -> None:
    """Activa manualmente un usuario PENDIENTE en la DB de pruebas."""
    async with TestingSessionLocal() as session:
        result = await session.execute(select(User).where(User.email == email))
        user = result.scalar_one_or_none()
        if user:
            user.estado = UserStatus.ACTIVO
            await session.commit()


async def register_activate_login(client, email: str, rol: str) -> str:
    await client.post(
        "/api/v1/auth/register",
        json={
            "email": email,
            "password": "Password123*",
            "nombre": "Test Veh",
            "telefono": "3005550000",
            "rol": rol,
        },
    )
    if rol in ("REPARTIDOR", "COMERCIO"):
        await _activate_user(email)

    res = await client.post(
        "/api/v1/auth/login",
        json={"email": email, "password": "Password123*"},
    )
    return res.json()["access_token"]


VEHICULO_PAYLOAD = {"tipo": "MOTO", "placa": "ABC123", "modelo": "Honda CB190"}


# ─────────────────────────────────────────────────────────
# POST /vehiculos
# ─────────────────────────────────────────────────────────

@pytest.mark.asyncio
async def test_create_vehiculo_unauthorized(client):
    res = await client.post("/api/v1/vehiculos", json=VEHICULO_PAYLOAD)
    assert res.status_code == 403


@pytest.mark.asyncio
async def test_create_vehiculo_cliente_forbidden(client):
    token = await register_activate_login(client, "clienteveh@barboya.com", "CLIENTE")
    res = await client.post(
        "/api/v1/vehiculos",
        json=VEHICULO_PAYLOAD,
        headers={"Authorization": f"Bearer {token}"},
    )
    assert res.status_code == 403


@pytest.mark.asyncio
async def test_create_vehiculo_success(client):
    token = await register_activate_login(client, "repveh1@barboya.com", "REPARTIDOR")
    res = await client.post(
        "/api/v1/vehiculos",
        json=VEHICULO_PAYLOAD,
        headers={"Authorization": f"Bearer {token}"},
    )
    assert res.status_code == 201
    data = res.json()
    assert data["tipo"] == "MOTO"
    assert data["placa"] == "ABC123"
    assert data["modelo"] == "Honda CB190"
    assert "id" in data
    assert "repartidor_id" in data


@pytest.mark.asyncio
async def test_create_vehiculo_duplicate(client):
    """Un repartidor no puede registrar dos vehículos."""
    token = await register_activate_login(client, "repveh2@barboya.com", "REPARTIDOR")
    headers = {"Authorization": f"Bearer {token}"}

    res1 = await client.post("/api/v1/vehiculos", json=VEHICULO_PAYLOAD, headers=headers)
    assert res1.status_code == 201

    res2 = await client.post("/api/v1/vehiculos", json=VEHICULO_PAYLOAD, headers=headers)
    assert res2.status_code == 409
    assert res2.json()["error"]["code"] == "ALREADY_EXISTS"


# ─────────────────────────────────────────────────────────
# GET /vehiculos/me
# ─────────────────────────────────────────────────────────

@pytest.mark.asyncio
async def test_get_vehiculo_me_after_create(client):
    token = await register_activate_login(client, "repveh3@barboya.com", "REPARTIDOR")
    headers = {"Authorization": f"Bearer {token}"}

    await client.post("/api/v1/vehiculos", json=VEHICULO_PAYLOAD, headers=headers)

    res = await client.get("/api/v1/vehiculos/me", headers=headers)
    assert res.status_code == 200
    data = res.json()
    assert data["tipo"] == "MOTO"
    assert data["placa"] == "ABC123"


@pytest.mark.asyncio
async def test_get_vehiculo_me_not_found(client):
    token = await register_activate_login(client, "repveh4@barboya.com", "REPARTIDOR")
    res = await client.get(
        "/api/v1/vehiculos/me",
        headers={"Authorization": f"Bearer {token}"},
    )
    assert res.status_code == 404
    assert res.json()["error"]["code"] == "NOT_FOUND"
