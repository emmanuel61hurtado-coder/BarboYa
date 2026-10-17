import pytest
from httpx import ASGITransport, AsyncClient

from app.main import app


@pytest.mark.asyncio
async def test_auth_register():
    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as ac:
        payload = {
            "email": "test@barboya.com",
            "password": "SecurePassword123!",
            "full_name": "Test User",
            "phone": "+573001234567",
            "role": "client",
        }
        res = await ac.post("/api/v1/auth/register", json=payload)
    assert res.status_code == 201
    assert "msg" in res.json()


@pytest.mark.asyncio
async def test_auth_login():
    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as ac:
        res = await ac.post("/api/v1/auth/login")
    assert res.status_code == 200
    data = res.json()
    assert "access_token" in data
    assert data["token_type"] == "bearer"


@pytest.mark.asyncio
async def test_users_me():
    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as ac:
        res = await ac.get("/api/v1/users/me")
    assert res.status_code == 200
    data = res.json()
    assert data["email"] == "usuario@barboya.com"

    # Test PUT /me
    profile_update = {
        "id": data["id"],
        "email": "nuevo@barboya.com",
        "full_name": "Usuario Actualizado",
        "phone": "+573111111111",
        "role": "client",
        "is_active": True,
    }
    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as ac:
        put_res = await ac.put("/api/v1/users/me", json=profile_update)
    assert put_res.status_code == 200
    assert put_res.json()["full_name"] == "Usuario Actualizado"


@pytest.mark.asyncio
async def test_services_list():
    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as ac:
        res = await ac.get("/api/v1/services/")
    assert res.status_code == 200
    data = res.json()
    assert isinstance(data, list)
    assert len(data) >= 1
    assert "name" in data[0]


@pytest.mark.asyncio
async def test_transport_quote_and_request():
    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as ac:
        quote_payload = {
            "pickup_lat": 4.6097,
            "pickup_lng": -74.0817,
            "dropoff_lat": 4.6533,
            "dropoff_lng": -74.0583,
            "service_type": "moto",
        }
        quote_res = await ac.post("/api/v1/transport/quote", json=quote_payload)
        assert quote_res.status_code == 200
        quote_data = quote_res.json()
        assert "estimated_fare" in quote_data
        assert quote_data["estimated_fare"] > 0

        req_res = await ac.post("/api/v1/transport/request", json=quote_payload)
        assert req_res.status_code == 201
        req_data = req_res.json()
        assert "ride_id" in req_data


@pytest.mark.asyncio
async def test_notifications():
    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as ac:
        res = await ac.get("/api/v1/notifications/")
        assert res.status_code == 200
        data = res.json()
        assert len(data) >= 1
        notif_id = data[0]["id"]

        read_res = await ac.post(f"/api/v1/notifications/{notif_id}/read")
        assert read_res.status_code == 200
        assert read_res.json()["read"] is True


@pytest.mark.asyncio
async def test_payments_wallet():
    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as ac:
        res = await ac.get("/api/v1/payments/wallet")
        assert res.status_code == 200
        data = res.json()
        assert "balance" in data

        topup_payload = {"amount": 20000.0, "payment_method_id": "pm_card_visa"}
        topup_res = await ac.post("/api/v1/payments/wallet/topup", json=topup_payload)
        assert topup_res.status_code == 200
        assert topup_res.json()["status"] == "success"
