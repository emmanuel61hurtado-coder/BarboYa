import pytest
from fastapi.testclient import TestClient
from app.main import app
from app.core.security import create_token
from uuid import uuid4

client = TestClient(app)


def test_sql_injection_attempt_in_login():
    # Attempt SQL injection string in phone/password fields
    response = client.post("/auth/login", json={"phone": "' OR '1'='1", "password": "' OR '1'='1"})
    # Should be unauthorized, not 500 error or SQL injection success
    assert response.status_code == 401


def test_jwt_tampered_token_rejection():
    # Create valid token then tamper with it
    token = create_token(uuid4(), "customer")
    parts = token.split(".")
    tampered_token = parts[0] + ".eyJzdWIiOiJmYWtlIn0." + parts[2]
    
    response = client.get("/wallet/me", headers={"Authorization": f"Bearer {tampered_token}"})
    assert response.status_code == 401


def test_rbac_customer_accessing_admin_endpoint():
    customer_token = create_token(uuid4(), "customer")
    response = client.get("/admin/dashboard/stats", headers={"Authorization": f"Bearer {customer_token}"})
    assert response.status_code == 403


def test_security_headers_present_on_all_responses():
    response = client.get("/health")
    assert response.headers.get("x-content-type-options") == "nosniff"
    assert response.headers.get("x-frame-options") == "DENY"
    assert response.headers.get("x-xss-protection") == "1; mode=block"
    assert "strict-transport-security" in response.headers
    assert "content-security-policy" in response.headers


def test_unauthorized_access_to_protected_trips():
    response = client.post("/trips", json={
        "service_type": "passenger",
        "pickup": {"lat": 5.9333, "lng": -73.6208},
        "dropoff": {"lat": 5.9400, "lng": -73.6150},
        "offered_fare_cop": 6000
    })
    assert response.status_code == 401
