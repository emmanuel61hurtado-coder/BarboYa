"""
CRUD Tests: Users
Tests para GET /users/me y PATCH /users/me
"""
import pytest


# ─────────────────────────────────────────────────────────
# Helpers
# ─────────────────────────────────────────────────────────

async def register_and_login(client, email: str, rol: str = "CLIENTE") -> str:
    """Registra un usuario y devuelve su access_token."""
    await client.post(
        "/api/v1/auth/register",
        json={
            "email": email,
            "password": "Password123*",
            "nombre": "Usuario CRUD",
            "telefono": "3000000001",
            "rol": rol,
        },
    )
    res = await client.post(
        "/api/v1/auth/login",
        json={"email": email, "password": "Password123*"},
    )
    return res.json()["access_token"]


# ─────────────────────────────────────────────────────────
# GET /users/me
# ─────────────────────────────────────────────────────────

@pytest.mark.asyncio
async def test_get_me_authenticated(client):
    token = await register_and_login(client, "getme@barboya.com")
    res = await client.get(
        "/api/v1/users/me",
        headers={"Authorization": f"Bearer {token}"},
    )
    assert res.status_code == 200
    data = res.json()
    assert data["email"] == "getme@barboya.com"
    assert data["rol"] == "CLIENTE"


@pytest.mark.asyncio
async def test_get_me_unauthorized(client):
    res = await client.get("/api/v1/users/me")
    assert res.status_code == 403


# ─────────────────────────────────────────────────────────
# PATCH /users/me
# ─────────────────────────────────────────────────────────

@pytest.mark.asyncio
async def test_update_me(client):
    token = await register_and_login(client, "updateme@barboya.com")
    res = await client.patch(
        "/api/v1/users/me",
        headers={"Authorization": f"Bearer {token}"},
        json={"nombre": "Nombre Actualizado", "telefono": "3009999999"},
    )
    assert res.status_code == 200
    data = res.json()
    assert data["nombre"] == "Nombre Actualizado"
    assert data["telefono"] == "3009999999"


@pytest.mark.asyncio
async def test_update_me_partial(client):
    """Actualizar sólo el nombre sin romper el teléfono."""
    token = await register_and_login(client, "partialupdate@barboya.com")
    res = await client.patch(
        "/api/v1/users/me",
        headers={"Authorization": f"Bearer {token}"},
        json={"nombre": "Solo Nombre"},
    )
    assert res.status_code == 200
    assert res.json()["nombre"] == "Solo Nombre"
