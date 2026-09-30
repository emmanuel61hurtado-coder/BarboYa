import pytest


@pytest.mark.asyncio
async def test_auth_flow(client):
    # Register client
    response = await client.post(
        "/api/v1/auth/register",
        json={
            "email": "testclient@barboya.com",
            "password": "Password123*",
            "nombre": "Test Client",
            "telefono": "3001112233",
            "rol": "CLIENTE"
        }
    )
    assert response.status_code == 201
    data = response.json()
    assert data["email"] == "testclient@barboya.com"

    # Login
    response = await client.post(
        "/api/v1/auth/login",
        json={
            "email": "testclient@barboya.com",
            "password": "Password123*"
        }
    )
    assert response.status_code == 200
    token_data = response.json()
    assert "access_token" in token_data
