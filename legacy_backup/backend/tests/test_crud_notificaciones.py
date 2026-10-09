"""
CRUD Tests: Notificaciones
Tests para:
  GET /notificaciones  - listar notificaciones del usuario autenticado
"""
import pytest


async def register_and_login(client, email: str, rol: str = "CLIENTE") -> str:
    await client.post(
        "/api/v1/auth/register",
        json={
            "email": email,
            "password": "Password123*",
            "nombre": "Notif User",
            "telefono": "3002220000",
            "rol": rol,
        },
    )
    res = await client.post(
        "/api/v1/auth/login",
        json={"email": email, "password": "Password123*"},
    )
    return res.json()["access_token"]


@pytest.mark.asyncio
async def test_list_notificaciones_empty(client):
    """Usuario nuevo sin notificaciones → lista vacía."""
    token = await register_and_login(client, "notif_empty@barboya.com")
    res = await client.get(
        "/api/v1/notificaciones",
        headers={"Authorization": f"Bearer {token}"},
    )
    assert res.status_code == 200
    assert res.json() == []


@pytest.mark.asyncio
async def test_list_notificaciones_unauthorized(client):
    """Sin token → 403."""
    res = await client.get("/api/v1/notificaciones")
    assert res.status_code in (401, 403)  # FastAPI >=0.122 devuelve 401 sin credenciales
