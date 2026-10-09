import pytest


@pytest.mark.asyncio
async def test_register_client_success(client):
    response = await client.post(
        "/api/v1/auth/register",
        json={
            "email": "cliente1@barboya.com",
            "password": "Password123*",
            "nombre": "Cliente Ejemplo",
            "telefono": "3001112233",
            "rol": "CLIENTE"
        }
    )
    assert response.status_code == 201
    data = response.json()
    assert data["email"] == "cliente1@barboya.com"
    assert data["rol"] == "CLIENTE"
    assert data["estado"] == "ACTIVO"


@pytest.mark.asyncio
async def test_register_repartidor_pending(client):
    response = await client.post(
        "/api/v1/auth/register",
        json={
            "email": "repartidor1@barboya.com",
            "password": "Password123*",
            "nombre": "Repartidor Ejemplo",
            "telefono": "3004445566",
            "rol": "REPARTIDOR"
        }
    )
    assert response.status_code == 201
    data = response.json()
    assert data["email"] == "repartidor1@barboya.com"
    assert data["rol"] == "REPARTIDOR"
    assert data["estado"] == "PENDIENTE"


@pytest.mark.asyncio
async def test_register_duplicate_email(client):
    payload = {
        "email": "duplicado@barboya.com",
        "password": "Password123*",
        "nombre": "User Dup",
        "telefono": "3009998877",
        "rol": "CLIENTE"
    }
    res1 = await client.post("/api/v1/auth/register", json=payload)
    assert res1.status_code == 201

    res2 = await client.post("/api/v1/auth/register", json=payload)
    assert res2.status_code == 409
    assert res2.json()["error"]["code"] == "EMAIL_ALREADY_EXISTS"


@pytest.mark.asyncio
async def test_login_success(client):
    # Register
    await client.post(
        "/api/v1/auth/register",
        json={
            "email": "logintest@barboya.com",
            "password": "Password123*",
            "nombre": "Login User",
            "telefono": "3001234567",
            "rol": "CLIENTE"
        }
    )

    # Login
    response = await client.post(
        "/api/v1/auth/login",
        json={
            "email": "logintest@barboya.com",
            "password": "Password123*"
        }
    )
    assert response.status_code == 200
    token_data = response.json()
    assert "access_token" in token_data
    assert token_data["token_type"] == "bearer"


@pytest.mark.asyncio
async def test_login_invalid_credentials(client):
    response = await client.post(
        "/api/v1/auth/login",
        json={
            "email": "nonexistent@barboya.com",
            "password": "WrongPassword"
        }
    )
    assert response.status_code == 401
    assert response.json()["error"]["code"] == "INVALID_CREDENTIALS"


@pytest.mark.asyncio
async def test_forgot_password(client):
    response = await client.post(
        "/api/v1/auth/forgot-password",
        json={"email": "logintest@barboya.com"}
    )
    assert response.status_code == 200
    assert response.json()["message"] == "Correo de recuperación enviado"
