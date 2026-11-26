"""Pruebas de integración de la API de pagos cripto (rutas completas).

Usa la app real de FastAPI con dependencias sobreescribidass:
    * get_session  -> SQLite en memoria con el esquema completo
    * get_current_user -> usuario simulado (conductor / admin)

Ejercita el ciclo HTTP: POST /payments/crypto (billetera) -> sincronía del
indexador -> GET estado -> acreditación en el wallet.
"""
import asyncio
import uuid

import pytest
from fastapi.testclient import TestClient
from sqlalchemy.ext.asyncio import async_sessionmaker, create_async_engine

from app.core import security
from app.core.config import get_settings
from app.core.database import get_session
from app.main import app
from app.models import enums
from app.models.user import DriverProfile, User
from app.services import blockchain

settings = get_settings()

PAYER = "0x" + "aa" * 20
TREASURY = "0x" + "bb" * 20
TOKEN = "0x" + "cc" * 20


@pytest.fixture(autouse=True)
def chain_config(monkeypatch):
    monkeypatch.setattr(settings, "payment_token_address", TOKEN)
    monkeypatch.setattr(settings, "treasury_address", TREASURY)
    monkeypatch.setattr(settings, "payment_token_symbol", "USDC")
    monkeypatch.setattr(settings, "payment_token_decimals", 6)
    monkeypatch.setattr(settings, "cop_per_token", 4_000)
    monkeypatch.setattr(settings, "required_confirmations", 12)
    monkeypatch.setattr(settings, "chain_rpc_url", "")
    blockchain.set_chain(blockchain.SimulatedChain(block_delay=2))
    yield
    blockchain.set_chain(None)


@pytest.fixture()
def client():
    """App real con sesión SQLite en memoria y usuario simulado."""
    engine = create_async_engine("sqlite+aiosqlite:///:memory:")
    maker = async_sessionmaker(engine, expire_on_commit=False)

    async def _seed():
        from app.core.database import Base

        import app.models  # noqa: F401  (registra todos los modelos en Base.metadata)

        async with engine.begin() as conn:
            await conn.run_sync(Base.metadata.create_all)
        async with maker() as session:
            user = User(
                role=enums.UserRole.driver,
                full_name="Conductor API",
                phone="+573001112233",
                password_hash="x",
            )
            session.add(user)
            await session.flush()
            session.add(
                DriverProfile(
                    user_id=user.id,
                    vehicle_type=enums.VehicleType.moto,
                    vehicle_plate="API123",
                    document_id="CC-API",
                    selfie_reference_url="",
                )
            )
            await session.commit()
            return user.id

    driver_id = asyncio.run(_seed())

    async def override_session():
        async with maker() as session:
            yield session

    app.dependency_overrides[get_session] = override_session
    app.dependency_overrides[security.get_current_user] = lambda: security.CurrentUser(driver_id, "driver")

    with TestClient(app, raise_server_exceptions=True) as c:
        yield c
    app.dependency_overrides.clear()


def test_config_and_register_flow(client):
    """GET config -> POST intento -> estado inicial pendiente (mempool)."""
    cfg = client.get("/payments/crypto/config").json()
    assert cfg["enabled"] is True
    assert cfg["chain_id"] == 80002
    assert cfg["treasury_address"].startswith("0x")
    assert cfg["token_symbol"] == "USDC"
    assert cfg["min_cop"] == 1_000 and cfg["max_cop"] == 500_000

    # registro inválido -> 422 (validación de formato Pydantic)
    bad = client.post(
        "/payments/crypto",
        json={"tx_hash": "0x123", "payer_address": PAYER, "token_amount_units": 1_000_000},
    )
    assert bad.status_code == 422
    assert isinstance(bad.json()["detail"], list)

    tx_hash = blockchain.get_chain().submit(PAYER, TOKEN, TREASURY, 1_000_000)
    created = client.post(
        "/payments/crypto",
        json={"tx_hash": tx_hash, "payer_address": PAYER, "token_amount_units": 1_000_000},
    )
    assert created.status_code == 201, created.text
    body = created.json()
    assert body["status"] == "pending"
    assert body["amount_cop"] == 4_000

    history = client.get("/payments/crypto").json()
    assert len(history) == 1 and history[0]["id"] == body["id"]

    duplicate = client.post(
        "/payments/crypto",
        json={"tx_hash": tx_hash, "payer_address": PAYER, "token_amount_units": 1_000_000},
    )
    assert duplicate.status_code == 422
    assert duplicate.json()["detail"]["code"] == "TX_ALREADY_REPORTED"


def test_indexer_confirms_via_status_endpoint(client):
    """El GET de estado sincroniza contra la cadena y confirma el pago."""
    chain = blockchain.get_chain()
    tx_hash = chain.submit(PAYER, TOKEN, TREASURY, 1_000_000)
    body = client.post(
        "/payments/crypto",
        json={"tx_hash": tx_hash, "payer_address": PAYER, "token_amount_units": 1_000_000},
    ).json()
    pid = body["id"]

    # 1a consulta: aún en mempool
    st = client.get(f"/payments/crypto/{pid}").json()
    assert st["status"] == "pending"

    # 2a consulta: recibo disponible -> confirmado
    st = client.get(f"/payments/crypto/{pid}").json()
    assert st["status"] == "confirmed"
    assert st["wallet_tx_id"] is not None
    assert st["amount_cop"] == 4_000

    # admin puede forzar el barrido del indexador (ya no queda nada pendiente)
    app.dependency_overrides[security.get_current_user] = lambda: security.CurrentUser(uuid.uuid4(), "admin")
    sync = client.post("/admin/payments/crypto/sync")
    assert sync.status_code == 200
    assert sync.json()["synced"] == 0


def test_disabled_method_returns_503(client, monkeypatch):
    monkeypatch.setattr(settings, "payment_token_address", "")
    r = client.get("/payments/crypto/config")
    assert r.status_code == 503
    assert r.json()["detail"]["code"] == "CHAIN_NOT_CONFIGURED"
