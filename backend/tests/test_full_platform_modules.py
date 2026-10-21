import pytest
from unittest.mock import AsyncMock, MagicMock
from uuid import uuid4
from fastapi.testclient import TestClient
from app.main import app
from app.core.security import create_token
from app.core.database import get_session
from app.models.enums import VerificationStatus
from app.models.user import DriverProfile

client = TestClient(app)


def test_public_registration_blocks_admin_roles():
    """Regla Crítica: Nadie puede registrarse como admin ni superadmin directamente."""
    res_admin = client.post("/auth/register", json={
        "full_name": "Atacante Admin",
        "phone": "+573001234567",
        "password": "Password123!",
        "role": "admin"
    })
    assert res_admin.status_code == 403

    res_super = client.post("/auth/register", json={
        "full_name": "Atacante Superadmin",
        "phone": "+573001234568",
        "password": "Password123!",
        "role": "superadmin"
    })
    assert res_super.status_code == 403


def test_customer_cannot_access_merchant_store():
    """Un cliente no puede acceder a gestionar tiendas de comercios sin el rol."""
    customer_token = create_token(uuid4(), "customer")
    res = client.get("/merchants/my-store/profile", headers={"Authorization": f"Bearer {customer_token}"})
    assert res.status_code == 403


def test_customer_cannot_access_admin_verification_pipeline():
    """El panel de verificación de conductores solo es accesible para administradores."""
    customer_token = create_token(uuid4(), "customer")
    res_forbidden = client.get("/admin/drivers/verification", headers={"Authorization": f"Bearer {customer_token}"})
    assert res_forbidden.status_code == 403


def test_driver_cannot_access_admin_dashboard():
    """Un conductor no puede acceder a las estadísticas del panel de administración."""
    driver_token = create_token(uuid4(), "driver")
    res = client.get("/admin/dashboard/stats", headers={"Authorization": f"Bearer {driver_token}"})
    assert res.status_code == 403


def test_merchant_cannot_access_driver_routes():
    """Un comercio no puede manipular las rutas del conductor."""
    merchant_token = create_token(uuid4(), "merchant_owner")
    res = client.get("/drivers/me", headers={"Authorization": f"Bearer {merchant_token}"})
    assert res.status_code == 403


def test_unverified_driver_cannot_go_online_mocked():
    """Un conductor no verificado recibe 403 Forbidden al intentar ponerse en línea."""
    mock_session = AsyncMock()
    mock_driver = MagicMock(spec=DriverProfile)
    mock_driver.is_verified = False
    mock_session.get.return_value = mock_driver

    async def override_get_session():
        yield mock_session

    app.dependency_overrides[get_session] = override_get_session
    try:
        driver_id = uuid4()
        driver_token = create_token(driver_id, "driver")
        res = client.post("/drivers/location", json={
            "lat": 5.9328,
            "lng": -73.6204,
            "is_online": True
        }, headers={"Authorization": f"Bearer {driver_token}"})
        assert res.status_code == 403
        assert "no está verificada" in res.json()["detail"]
    finally:
        app.dependency_overrides.pop(get_session, None)


def test_admin_can_access_verification_pipeline():
    """Un administrador autorizado puede listar conductores para verificar."""
    mock_session = AsyncMock()
    mock_session.execute.return_value = MagicMock(all=MagicMock(return_value=[]))

    async def override_get_session():
        yield mock_session

    app.dependency_overrides[get_session] = override_get_session
    try:
        admin_token = create_token(uuid4(), "admin")
        res = client.get("/admin/drivers/verification", headers={"Authorization": f"Bearer {admin_token}"})
        assert res.status_code == 200
        assert isinstance(res.json(), list)
    finally:
        app.dependency_overrides.pop(get_session, None)


def test_role_based_login_rejection():
    """Si un usuario cliente intenta iniciar sesión seleccionando rol Conductor o Admin, se rechaza con 403."""
    from app.models.enums import UserRole
    from app.core.security import hash_password

    mock_session = AsyncMock()
    mock_user = MagicMock()
    mock_user.role = UserRole.customer
    mock_user.is_active = True
    mock_user.password_hash = hash_password("Password123!")

    exec_result = MagicMock()
    exec_result.scalar_one_or_none.return_value = mock_user
    mock_session.execute = AsyncMock(return_value=exec_result)

    async def override_get_session():
        yield mock_session

    app.dependency_overrides[get_session] = override_get_session
    try:
        res = client.post("/auth/login", json={
            "phone": "+573009999999",
            "password": "Password123!",
            "expected_role": "admin"
        })
        assert res.status_code == 403
        assert "no pertenece al perfil de Administrador" in res.json()["detail"]
    finally:
        app.dependency_overrides.pop(get_session, None)


def test_password_recovery_request_code_and_reset():
    """Prueba la solicitud de código y la recuperación por correo o teléfono."""
    # 1. Solicitar código por correo
    res_code = client.post("/auth/request-recovery-code", json={
        "identifier": "conductor@barboya.co",
        "method": "email"
    })
    assert res_code.status_code == 200
    data = res_code.json()
    assert data["status"] == "ok"
    assert data["security_pin"] == "8492"
    assert "conductor@barboya.co" in data["identifier"]

    # 2. Mock de recuperación de contraseña
    mock_session = AsyncMock()
    mock_user = MagicMock()
    mock_user.password_hash = "old_hash"
    exec_result = MagicMock()
    exec_result.scalar_one_or_none.return_value = mock_user
    mock_session.execute = AsyncMock(return_value=exec_result)

    async def override_get_session():
        yield mock_session

    app.dependency_overrides[get_session] = override_get_session
    try:
        # Recuperación por correo
        res_email = client.post("/auth/recover-password", json={
            "identifier": "usuario@barboya.co",
            "recovery_method": "email",
            "security_pin": "8492",
            "new_password": "NuevoPassword2026!"
        })
        assert res_email.status_code == 200
        assert "exitosamente" in res_email.json()["message"]

        # Recuperación por teléfono (compatibilidad)
        res_phone = client.post("/auth/recover-password", json={
            "phone": "+573105551234",
            "recovery_method": "phone",
            "security_pin": "8492",
            "new_password": "OtroPassword2026!"
        })
        assert res_phone.status_code == 200
        assert "exitosamente" in res_phone.json()["message"]
    finally:
        app.dependency_overrides.pop(get_session, None)


