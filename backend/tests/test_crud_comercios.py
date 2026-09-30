"""
CRUD Tests: Comercios
Tests para:
  GET  /comercios          - listar comercios abiertos
  GET  /comercios/{id}     - obtener comercio por id
  POST /comercios          - crear comercio (rol COMERCIO)
  POST /comercios          - duplicado = 409
"""
import pytest


async def register_and_login(client, email: str, rol: str = "CLIENTE") -> str:
    await client.post(
        "/api/v1/auth/register",
        json={
            "email": email,
            "password": "Password123*",
            "nombre": "Test User",
            "telefono": "3000000099",
            "rol": rol,
        },
    )
    # COMERCIO y REPARTIDOR quedan PENDIENTE; para login deben aprobarse.
    # En tests con SQLite no hay admin real, usamos CLIENTE para el flow completo.
    res = await client.post(
        "/api/v1/auth/login",
        json={"email": email, "password": "Password123*"},
    )
    return res.json()["access_token"]


COMERCIO_PAYLOAD = {
    "nombre": "Burguer Planet",
    "descripcion": "Las mejores hamburguesas",
    "direccion": "Calle 50 # 30-10",
    "lat": "6.2442",
    "lng": "-75.5812",
    "abierto": True,
}


# ─────────────────────────────────────────────────────────
# GET /comercios — público
# ─────────────────────────────────────────────────────────

@pytest.mark.asyncio
async def test_list_comercios_empty(client):
    res = await client.get("/api/v1/comercios")
    assert res.status_code == 200
    assert isinstance(res.json(), list)


# ─────────────────────────────────────────────────────────
# GET /comercios/{id} — comercio no existente
# ─────────────────────────────────────────────────────────

@pytest.mark.asyncio
async def test_get_comercio_not_found(client):
    fake_id = "00000000-0000-0000-0000-000000000000"
    res = await client.get(f"/api/v1/comercios/{fake_id}")
    assert res.status_code == 404
    assert res.json()["error"]["code"] == "NOT_FOUND"


# ─────────────────────────────────────────────────────────
# POST /comercios — requiere rol COMERCIO + ACTIVO
# Para forzarlo en SQLite, modificamos el estado directamente en DB.
# ─────────────────────────────────────────────────────────

@pytest.mark.asyncio
async def test_create_comercio_requires_auth(client):
    """Sin token → 403."""
    res = await client.post("/api/v1/comercios", json=COMERCIO_PAYLOAD)
    assert res.status_code == 403


@pytest.mark.asyncio
async def test_create_comercio_requires_comercio_role(client):
    """Token de CLIENTE → 403 FORBIDDEN."""
    token = await register_and_login(client, "clientenocomerc@barboya.com", "CLIENTE")
    res = await client.post(
        "/api/v1/comercios",
        json=COMERCIO_PAYLOAD,
        headers={"Authorization": f"Bearer {token}"},
    )
    assert res.status_code == 403
