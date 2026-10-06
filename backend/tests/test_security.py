"""Pruebas de seguridad: hashing, JWT, autenticacion, autorizacion por rol y configuracion."""
import base64
import json
from datetime import timedelta

import pytest
from jose import jwt

from app.core.config import Settings, settings
from app.core.security import (
    create_access_token,
    create_refresh_token,
    get_password_hash,
    verify_password,
)
from app.models.user import UserRole, UserStatus

NO_AUTH = (401, 403)


# ---------- hashing ----------
def test_hash_es_argon2_y_no_guarda_texto_plano():
    h = get_password_hash("Secreta123*")
    assert h.startswith("$argon2")
    assert "Secreta123*" not in h


def test_hash_con_salt_distinto_cada_vez():
    assert get_password_hash("misma") != get_password_hash("misma")


def test_verify_password_correcta_e_incorrecta():
    h = get_password_hash("Correcta1*")
    assert verify_password("Correcta1*", h) is True
    assert verify_password("incorrecta", h) is False


# ---------- JWT ----------
def test_access_token_claims():
    p = jwt.decode(create_access_token("abc"), settings.SECRET_KEY, algorithms=["HS256"])
    assert p["sub"] == "abc" and p["type"] == "access" and "exp" in p


def test_refresh_token_claims():
    p = jwt.decode(create_refresh_token("abc"), settings.SECRET_KEY, algorithms=["HS256"])
    assert p["type"] == "refresh"


def test_token_firmado_con_otra_clave_es_rechazado():
    token = jwt.encode({"sub": "abc"}, "otra-clave", algorithm="HS256")
    with pytest.raises(jwt.JWTError):
        jwt.decode(token, settings.SECRET_KEY, algorithms=["HS256"])


def test_token_expirado_es_rechazado():
    token = create_access_token("abc", expires_delta=timedelta(seconds=-1))
    with pytest.raises(jwt.JWTError):
        jwt.decode(token, settings.SECRET_KEY, algorithms=["HS256"])


# ---------- autenticacion de endpoints ----------
@pytest.mark.parametrize("path", ["/api/v1/users/me", "/api/v1/admin/usuarios", "/api/v1/reportes/ventas"])
async def test_endpoints_protegidos_sin_token(client, path):
    r = await client.get(path)
    assert r.status_code in NO_AUTH


async def test_token_basura_da_401(client):
    r = await client.get("/api/v1/users/me", headers={"Authorization": "Bearer no.es.un.jwt"})
    assert r.status_code == 401
    assert r.json()["error"]["code"] == "INVALID_TOKEN"


async def test_token_expirado_da_401(client, make_user):
    user, _ = await make_user()
    token = create_access_token(user.id, expires_delta=timedelta(seconds=-5))
    r = await client.get("/api/v1/users/me", headers={"Authorization": f"Bearer {token}"})
    assert r.status_code == 401


async def test_token_firmado_con_otra_clave_da_401(client, make_user):
    user, _ = await make_user()
    token = jwt.encode({"sub": str(user.id), "type": "access"}, "otra-clave", algorithm="HS256")
    r = await client.get("/api/v1/users/me", headers={"Authorization": f"Bearer {token}"})
    assert r.status_code == 401


async def test_token_alg_none_es_rechazado(client, make_user):
    user, _ = await make_user()

    def b64(d):
        return base64.urlsafe_b64encode(json.dumps(d).encode()).rstrip(b"=").decode()

    token = f"{b64({'alg': 'none', 'typ': 'JWT'})}.{b64({'sub': str(user.id)})}."
    r = await client.get("/api/v1/users/me", headers={"Authorization": f"Bearer {token}"})
    assert r.status_code == 401


async def test_token_valido_accede_a_me(client, make_user):
    user, headers = await make_user()
    r = await client.get("/api/v1/users/me", headers=headers)
    assert r.status_code == 200
    assert r.json()["email"] == user.email


async def test_respuestas_nunca_exponen_password_hash(client, make_user):
    _, headers = await make_user()
    r = await client.get("/api/v1/users/me", headers=headers)
    assert "password" not in r.text.lower()


async def test_usuario_bloqueado_no_accede(client, make_user):
    _, headers = await make_user(estado=UserStatus.BLOQUEADO)
    r = await client.get("/api/v1/users/me", headers=headers)
    assert r.status_code == 403
    assert r.json()["error"]["code"] == "USER_BLOCKED"


async def test_login_usuario_bloqueado_rechazado(client, make_user):
    user, _ = await make_user(estado=UserStatus.BLOQUEADO)
    r = await client.post("/api/v1/auth/login", json={"email": user.email, "password": "Password123*"})
    assert r.status_code == 403


async def test_login_mensaje_igual_para_usuario_inexistente_y_clave_mala(client, make_user):
    user, _ = await make_user()
    mala = await client.post("/api/v1/auth/login", json={"email": user.email, "password": "mala"})
    inexistente = await client.post("/api/v1/auth/login", json={"email": "no@existe.com", "password": "mala"})
    assert mala.status_code == inexistente.status_code == 401
    assert mala.json() == inexistente.json()


async def test_login_cookie_refresh_httponly_y_secure(client, make_user):
    user, _ = await make_user()
    r = await client.post("/api/v1/auth/login", json={"email": user.email, "password": "Password123*"})
    cookie = r.headers["set-cookie"].lower()
    assert "refresh_token=" in cookie and "httponly" in cookie and "secure" in cookie


@pytest.mark.parametrize("email", ["' OR '1'='1", "admin@barboya.com' --", "no-es-email"])
async def test_login_con_payload_de_inyeccion_da_422(client, email):
    r = await client.post("/api/v1/auth/login", json={"email": email, "password": "x"})
    assert r.status_code == 422


# ---------- autorizacion por rol ----------
@pytest.mark.parametrize("rol", [UserRole.CLIENTE, UserRole.REPARTIDOR, UserRole.COMERCIO])
async def test_admin_endpoint_prohibido_para_no_admin(client, make_user, rol):
    _, headers = await make_user(rol=rol)
    r = await client.get("/api/v1/admin/usuarios", headers=headers)
    assert r.status_code == 403
    assert r.json()["error"]["code"] == "FORBIDDEN"


async def test_admin_puede_listar_usuarios(client, make_user):
    _, headers = await make_user(rol=UserRole.ADMIN)
    r = await client.get("/api/v1/admin/usuarios", headers=headers)
    assert r.status_code == 200
    assert isinstance(r.json(), list)


async def test_admin_aprueba_y_bloquea_usuarios(client, make_user):
    _, admin_h = await make_user(rol=UserRole.ADMIN)
    pendiente, _ = await make_user(rol=UserRole.REPARTIDOR, estado=UserStatus.PENDIENTE)
    r = await client.patch(f"/api/v1/admin/usuarios/{pendiente.id}/aprobar", headers=admin_h)
    assert r.status_code == 200 and r.json()["estado"] == "ACTIVO"
    r = await client.patch(f"/api/v1/admin/usuarios/{pendiente.id}/bloquear", headers=admin_h)
    assert r.status_code == 200 and r.json()["estado"] == "BLOQUEADO"


async def test_cliente_no_puede_aprobar_usuarios(client, make_user):
    _, headers = await make_user(rol=UserRole.CLIENTE)
    victima, _ = await make_user(rol=UserRole.REPARTIDOR, estado=UserStatus.PENDIENTE)
    r = await client.patch(f"/api/v1/admin/usuarios/{victima.id}/aprobar", headers=headers)
    assert r.status_code == 403


async def test_comercio_pendiente_no_accede_a_reportes(client, make_user):
    _, h = await make_user(rol=UserRole.COMERCIO, estado=UserStatus.PENDIENTE)
    r = await client.get("/api/v1/reportes/ventas", headers=h)
    assert r.status_code == 403
    assert r.json()["error"]["code"] == "PENDING_APPROVAL"


async def test_comercio_activo_y_admin_acceden_a_reportes(client, make_user):
    for rol in (UserRole.COMERCIO, UserRole.ADMIN):
        _, h = await make_user(rol=rol)
        assert (await client.get("/api/v1/reportes/ventas", headers=h)).status_code == 200


async def test_cliente_no_accede_a_reportes(client, make_user):
    _, headers = await make_user(rol=UserRole.CLIENTE)
    assert (await client.get("/api/v1/reportes/ventas", headers=headers)).status_code == 403


async def test_patch_me_ignora_estado_y_rol_enviados_por_el_cliente(client, make_user):
    user, headers = await make_user(rol=UserRole.CLIENTE)
    r = await client.patch("/api/v1/users/me", headers=headers,
                           json={"nombre": "Nuevo", "estado": "BLOQUEADO", "rol": "ADMIN"})
    assert r.status_code == 200
    body = r.json()
    assert body["nombre"] == "Nuevo" and body["estado"] == "ACTIVO" and body["rol"] == "CLIENTE"


# ---------- registro ----------
async def test_registro_repartidor_y_comercio_quedan_pendientes(client):
    for i, rol in enumerate(["REPARTIDOR", "COMERCIO"]):
        r = await client.post("/api/v1/auth/register", json={
            "email": f"pend{i}@barboya.com", "password": "Password123*",
            "nombre": "P", "telefono": "3000000000", "rol": rol,
        })
        assert r.status_code == 201 and r.json()["estado"] == "PENDIENTE"


async def test_registro_no_devuelve_password(client):
    r = await client.post("/api/v1/auth/register", json={
        "email": "nopass@barboya.com", "password": "Password123*",
        "nombre": "P", "telefono": "3000000000", "rol": "CLIENTE",
    })
    assert r.status_code == 201
    assert "password" not in r.text.lower()


# ---------- vulnerabilidades conocidas (xfail strict: fallaran en cuanto se corrijan) ----------
@pytest.mark.xfail(strict=True, reason="VULN: /auth/register permite autoasignarse rol ADMIN")
async def test_registro_publico_no_debe_permitir_rol_admin(client):
    r = await client.post("/api/v1/auth/register", json={
        "email": "evil-admin@barboya.com", "password": "Password123*",
        "nombre": "Evil", "telefono": "3000000000", "rol": "ADMIN",
    })
    assert r.status_code in (400, 403, 422)


@pytest.mark.xfail(strict=True, reason="VULN: get_current_user no valida el claim type=access")
async def test_refresh_token_no_debe_servir_como_access_token(client, make_user):
    user, _ = await make_user()
    r = await client.get("/api/v1/users/me", headers={"Authorization": f"Bearer {create_refresh_token(user.id)}"})
    assert r.status_code == 401


# ---------- configuracion ----------
def _prod(**kw):
    base = dict(_env_file=None, ENVIRONMENT="production", SECRET_KEY="a" * 40, ADMIN_PASSWORD="UnaClaveFuerte987!")
    base.update(kw)
    return Settings(**base)


def test_produccion_valida_con_secretos_fuertes():
    assert _prod().ENVIRONMENT == "production"


@pytest.mark.parametrize("key", ["corta", "supersecretkeychangemeinproduction", "x" * 31, "changeme" + "z" * 40])
def test_produccion_rechaza_secret_key_debil(key):
    with pytest.raises(ValueError):
        _prod(SECRET_KEY=key)


@pytest.mark.parametrize("pw", ["Admin123*", "password", "admin", "123456", "root"])
def test_produccion_rechaza_admin_password_por_defecto(pw):
    with pytest.raises(ValueError):
        _prod(ADMIN_PASSWORD=pw)


def test_desarrollo_permite_claves_por_defecto():
    s = Settings(_env_file=None, ENVIRONMENT="development")
    assert s.ENVIRONMENT == "development"


def test_allowed_origins_acepta_json_y_csv():
    assert Settings(_env_file=None, ALLOWED_ORIGINS='["https://a.com","https://b.com"]').ALLOWED_ORIGINS == ["https://a.com", "https://b.com"]
    assert Settings(_env_file=None, ALLOWED_ORIGINS="https://a.com, https://b.com").ALLOWED_ORIGINS == ["https://a.com", "https://b.com"]
