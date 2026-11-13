"""Pruebas del método de pago cripto usando la cadena simulada.

Recorren el ciclo completo de la arquitectura:
billetera firma -> mempool (pending) -> bloque/EVM -> recibo y logs ->
indexador verifica -> PostgreSQL acredita el wallet.
"""
import uuid

import pytest
from sqlalchemy.ext.asyncio import async_sessionmaker, create_async_engine

from app.core.config import get_settings
from app.core.database import Base
from app.models import enums
from app.models.billing import WalletTransaction
from app.models.blockchain import BlockchainPayment
from app.models.user import DriverProfile, User
from app.services import blockchain

settings = get_settings()

PAYER = "0x" + "aa" * 20
TREASURY = "0x" + "bb" * 20
TOKEN = "0x" + "cc" * 20

MIN_UNITS = 1_000_000  # 1 USDC con 6 decimales -> 4.000 COP


@pytest.fixture(autouse=True)
def chain_config(monkeypatch):
    """Configura el método cripto y una cadena simulada para cada prueba."""
    monkeypatch.setattr(settings, "payment_token_address", TOKEN)
    monkeypatch.setattr(settings, "treasury_address", TREASURY)
    monkeypatch.setattr(settings, "payment_token_symbol", "USDC")
    monkeypatch.setattr(settings, "payment_token_decimals", 6)
    monkeypatch.setattr(settings, "cop_per_token", 4_000)
    monkeypatch.setattr(settings, "required_confirmations", 12)
    monkeypatch.setattr(settings, "topup_min_cop", 1_000)
    monkeypatch.setattr(settings, "topup_max_cop", 500_000)
    monkeypatch.setattr(settings, "chain_rpc_url", "")  # fuerza la cadena simulada
    blockchain.set_chain(blockchain.SimulatedChain(block_delay=2))
    yield
    blockchain.set_chain(None)


@pytest.fixture()
async def db():
    engine = create_async_engine("sqlite+aiosqlite:///:memory:")
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)
    maker = async_sessionmaker(engine, expire_on_commit=False)
    async with maker() as session:
        user = User(
            role=enums.UserRole.driver,
            full_name="Conductor Prueba",
            phone="+573001112233",
            password_hash="x",
        )
        session.add(user)
        await session.flush()
        driver = DriverProfile(
            user_id=user.id,
            vehicle_type=enums.VehicleType.moto,
            vehicle_plate="ABC123",
            document_id="CC-1",
            selfie_reference_url="",
        )
        session.add(driver)
        await session.commit()
        yield session, driver
    await engine.dispose()


async def _register(db, units=MIN_UNITS, tx_hash=None, payer=PAYER):
    """Paso 1: la billetera difunde el tx (JSON-RPC) y luego reporta el txHash."""
    session, driver = db
    if tx_hash is None:
        tx_hash = blockchain.get_chain().submit(payer, TOKEN, TREASURY, units)
    payment = await blockchain.register_payment(session, driver, tx_hash, payer, units)
    await session.commit()
    return payment


async def test_full_flow_pending_to_confirmed(db):
    """Mempool (pending) -> recibo -> confirmado -> saldo acreditado."""
    session, driver = db
    payment = await _register(db)
    assert payment.status == enums.BlockchainTxStatus.pending
    assert payment.amount_cop == 4_000  # 1 USDC a 4.000 COP

    # Primer barrido: el tx sigue en el mempool (aún no hay recibo)
    [p] = await blockchain.sync_pending_payments(session)
    assert p.status == enums.BlockchainTxStatus.pending
    assert driver.wallet_balance_cop == 0

    # Segundo barrido: ya está en un bloque; recibo + logs verificados
    [p] = await blockchain.sync_pending_payments(session)
    assert p.status == enums.BlockchainTxStatus.confirmed
    assert p.block_number is not None and p.gas_used is not None
    assert p.wallet_tx_id is not None
    assert p.confirmed_at is not None

    # El saldo del conductor quedó acreditado (paso 8, PostgreSQL)
    await session.refresh(driver)
    assert driver.wallet_balance_cop == 4_000
    tx = await session.get(WalletTransaction, p.wallet_tx_id)
    assert tx.type == enums.WalletTxType.topup
    assert tx.amount_cop == 4_000
    assert tx.balance_after_cop == 4_000
    assert tx.payment_method == enums.PaymentMethod.crypto


async def test_indexer_is_idempotent(db):
    """Un pago confirmado nunca se acredita dos veces."""
    session, driver = db
    payment = await _register(db)
    await blockchain.sync_pending_payments(session)   # mempool
    [p] = await blockchain.sync_pending_payments(session)  # bloque -> confirmado
    assert p.status == enums.BlockchainTxStatus.confirmed
    wallet_tx_id = p.wallet_tx_id

    # Barridos extra no vuelven a acreditar (el pago ya no está pendiente)
    assert await blockchain.sync_pending_payments(session) == []
    await session.refresh(driver)
    assert driver.wallet_balance_cop == 4_000
    assert p.wallet_tx_id == wallet_tx_id


async def test_reverted_tx_fails_without_credit(db):
    session, driver = db
    chain = blockchain.get_chain()
    tx_hash = chain.submit_reverting(PAYER, TOKEN, TREASURY, MIN_UNITS)
    payment = await blockchain.register_payment(session, db[1], tx_hash, PAYER, MIN_UNITS)
    await session.commit()

    await blockchain.sync_pending_payments(session)  # mempool
    [p] = await blockchain.sync_pending_payments(session)
    assert p.status == enums.BlockchainTxStatus.failed
    assert p.failure_reason
    await session.refresh(db[1])
    assert db[1].wallet_balance_cop == 0


async def test_wrong_payer_is_rejected(db):
    """El evento Transfer debe venir de la billetera reportada."""
    session, driver = db
    other = "0x" + "dd" * 20
    chain = blockchain.get_chain()
    tx_hash = chain.submit(other, TOKEN, TREASURY, MIN_UNITS)
    payment = await blockchain.register_payment(session, driver, tx_hash, PAYER, MIN_UNITS)
    await session.commit()

    await blockchain.sync_pending_payments(session)
    [p] = await blockchain.sync_pending_payments(session)
    assert p.status == enums.BlockchainTxStatus.failed
    assert "remitente" in p.failure_reason.lower()


async def test_invalid_inputs_are_rejected(db):
    session, driver = db
    with pytest.raises(blockchain.ChainError) as e:
        await blockchain.register_payment(session, driver, "0x123", PAYER, MIN_UNITS)
    assert e.value.code == "INVALID_TX_HASH"

    with pytest.raises(blockchain.ChainError) as e:
        await blockchain.register_payment(session, driver, "0x" + "1" * 64, "0xzz", MIN_UNITS)
    assert e.value.code == "INVALID_ADDRESS"

    # 0.5 USDC = 2.000 COP: por encima del mínimo, válido
    ok = await blockchain.register_payment(session, driver, "0x" + "2" * 64, PAYER, 500_000)
    assert ok.amount_cop == 2_000

    with pytest.raises(blockchain.ChainError) as e:
        await blockchain.register_payment(session, driver, "0x" + "3" * 64, PAYER, 10)
    assert e.value.code == "AMOUNT_TOO_LOW"


async def test_duplicate_tx_hash_rejected(db):
    session, driver = db
    tx_hash = "0x" + "f" * 64
    await blockchain.register_payment(session, driver, tx_hash, PAYER, MIN_UNITS)
    await session.commit()
    with pytest.raises(blockchain.ChainError) as e:
        await blockchain.register_payment(session, driver, tx_hash, PAYER, MIN_UNITS)
    assert e.value.code == "TX_ALREADY_REPORTED"


async def test_transfer_to_wrong_recipient_fails(db):
    """El pago debe llegar a la tesorería configurada."""
    session, driver = db
    stranger = "0x" + "ee" * 20
    chain = blockchain.get_chain()
    tx_hash = chain.submit(PAYER, TOKEN, stranger, MIN_UNITS)
    payment = await blockchain.register_payment(session, driver, tx_hash, PAYER, MIN_UNITS)
    await session.commit()

    await blockchain.sync_pending_payments(session)
    [p] = await blockchain.sync_pending_payments(session)
    assert p.status == enums.BlockchainTxStatus.failed


async def test_partial_amount_confirmed_for_full_only_when_enough(db):
    """Si transfiere MENOS de lo reportado, falla; si transfiere igual o más, confirma."""
    session, driver = db
    chain = blockchain.get_chain()
    tx_hash = chain.submit(PAYER, TOKEN, TREASURY, MIN_UNITS - 1)
    payment = await blockchain.register_payment(session, driver, tx_hash, PAYER, MIN_UNITS)
    await session.commit()
    await blockchain.sync_pending_payments(session)
    [p] = await blockchain.sync_pending_payments(session)
    assert p.status == enums.BlockchainTxStatus.failed
    assert "menor" in p.failure_reason.lower()
