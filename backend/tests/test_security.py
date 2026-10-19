import pytest
from fastapi.testclient import TestClient
from app.main import app
from app.core.security import hash_password, verify_password, create_token, decode_token
from uuid import uuid4

client = TestClient(app)


def test_password_hashing_argon2():
    raw = "PasswordSecure123!"
    hashed = hash_password(raw)
    assert hashed != raw
    assert verify_password(raw, hashed)
    assert not verify_password("WrongPassword", hashed)


def test_jwt_token_creation_and_validation():
    user_id = uuid4()
    role = "customer"
    token = create_token(user_id, role)
    payload = decode_token(token)
    assert payload["sub"] == str(user_id)
    assert payload["role"] == role
    assert payload["iss"] == "barboya-auth"
    assert payload["aud"] == "barboya-api"


def test_security_headers():
    response = client.get("/health")
    assert response.status_code == 200
    assert response.headers.get("x-content-type-options") == "nosniff"
    assert response.headers.get("x-frame-options") == "DENY"
    assert response.headers.get("x-xss-protection") == "1; mode=block"
    assert "strict-transport-security" in response.headers
    assert "content-security-policy" in response.headers


def test_sos_requires_auth():
    response = client.post("/security/sos", json={"lat": 5.9333, "lng": -73.6208})
    assert response.status_code == 401


def test_webhook_signature_verification():
    import hmac
    import hashlib
    from app.core.config import get_settings
    settings = get_settings()
    body = b'{"event": "transaction.updated"}'
    sig = hmac.new(settings.jwt_secret.encode(), body, hashlib.sha256).hexdigest()
    response = client.post("/webhooks/payments", content=body, headers={"x-signature": sig})
    assert response.status_code == 200
    assert response.json() == {"received": True, "event": "transaction.updated"}

    response_invalid = client.post("/webhooks/payments", content=body, headers={"x-signature": "invalid_sig"})
    assert response_invalid.status_code == 401
