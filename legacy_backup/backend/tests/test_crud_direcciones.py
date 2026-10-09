"""
CRUD Tests: Direcciones
Tests para:
  GET  /direcciones        - listar direcciones del usuario (CLIENTE)
  POST /direcciones        - crear dirección
  POST /direcciones        - sin token → 403
"""
import pytest


async def register_and_login_cliente(client, email: str) -> str:
    await client.post(
        "/api/v1/auth/register",
        json={
            "email": email,
            "password": "Password123*",
            "nombre": "Cliente Dirección",
            "telefono": "3001110000",
            "rol": "CLIENTE",
        },
    )
    res = await client.post(
        "/api/v1/auth/login",
        json={"email": email, "password": "Password123*"},
    )
    return res.json()["access_token"]


DIRECCION_PAYLOAD = {
    "nombre": "Casa",
    "direccion": "Carrera 5 # 10-20",
    "detalles": "Apto 301",
    "lat": "6.2400",
    "lng": "-75.5900",
}


# ─────────────────────────────────────────────────────────
# POST /direcciones
# ─────────────────────────────────────────────────────────

@pytest.mark.asyncio
async def test_create_direccion_success(client):
    token = await register_and_login_cliente(client, "direccion1@barboya.com")
    res = await client.post(
        "/api/v1/direcciones",
        json=DIRECCION_PAYLOAD,
        headers={"Authorization": f"Bearer {token}"},
    )
    assert res.status_code == 201
    data = res.json()
    assert data["nombre"] == "Casa"
    assert data["direccion"] == "Carrera 5 # 10-20"
    assert "id" in data
    assert "user_id" in data


@pytest.mark.asyncio
async def test_create_direccion_unauthorized(client):
    res = await client.post("/api/v1/direcciones", json=DIRECCION_PAYLOAD)
    assert res.status_code in (401, 403)  # FastAPI >=0.122 devuelve 401 sin credenciales


@pytest.mark.asyncio
async def test_create_direccion_repartidor_forbidden(client):
    """Repartidor no puede crear direcciones (requiere rol CLIENTE)."""
    await client.post(
        "/api/v1/auth/register",
        json={
            "email": "repdir@barboya.com",
            "password": "Password123*",
            "nombre": "Repartidor Dir",
            "telefono": "3001111111",
            "rol": "REPARTIDOR",
        },
    )
    # Repartidor queda PENDIENTE → login fallará con 403 PENDING_APPROVAL
    res_login = await client.post(
        "/api/v1/auth/login",
        json={"email": "repdir@barboya.com", "password": "Password123*"},
    )
    assert res_login.status_code == 200
    token = res_login.json()["access_token"]
    res = await client.post(
        "/api/v1/direcciones",
        json=DIRECCION_PAYLOAD,
        headers={"Authorization": f"Bearer {token}"},
    )
    # Repartidor no tiene rol CLIENTE → 403
    assert res.status_code == 403


# ─────────────────────────────────────────────────────────
# GET /direcciones
# ─────────────────────────────────────────────────────────

@pytest.mark.asyncio
async def test_list_direcciones_empty(client):
    """Usuario sin direcciones → lista vacía."""
    token = await register_and_login_cliente(client, "dir_empty@barboya.com")
    res = await client.get(
        "/api/v1/direcciones",
        headers={"Authorization": f"Bearer {token}"},
    )
    assert res.status_code == 200
    assert res.json() == []


@pytest.mark.asyncio
async def test_list_direcciones_after_create(client):
    """Crear una dirección y listar devuelve exactamente esa dirección."""
    token = await register_and_login_cliente(client, "dir_list@barboya.com")
    headers = {"Authorization": f"Bearer {token}"}

    # Crear
    await client.post("/api/v1/direcciones", json=DIRECCION_PAYLOAD, headers=headers)

    # Listar
    res = await client.get("/api/v1/direcciones", headers=headers)
    assert res.status_code == 200
    data = res.json()
    assert len(data) == 1
    assert data[0]["nombre"] == "Casa"


@pytest.mark.asyncio
async def test_list_direcciones_isolated_per_user(client):
    """Las direcciones de un usuario no son visibles para otro."""
    token_a = await register_and_login_cliente(client, "dir_a@barboya.com")
    token_b = await register_and_login_cliente(client, "dir_b@barboya.com")

    # Usuario A crea una dirección
    await client.post(
        "/api/v1/direcciones",
        json={**DIRECCION_PAYLOAD, "nombre": "Casa A"},
        headers={"Authorization": f"Bearer {token_a}"},
    )

    # Usuario B no debe verla
    res_b = await client.get(
        "/api/v1/direcciones",
        headers={"Authorization": f"Bearer {token_b}"},
    )
    assert res_b.status_code == 200
    assert res_b.json() == []
