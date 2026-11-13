"""Pagos con criptomonedas para Barboya (cadenas compatibles EVM).

Implementa el flujo de la arquitectura:

    1. Billetera ──JSON-RPC──> 2. Nodo RPC ──difunde──> 3. Mempool
                                                        │ selecciona
    8. PostgreSQL <──guarda── 7. Indexador <──emite── 6. Recibo y logs
                                                        │ produce
    5. EVM <──ejecuta── 4. Bloque

Roles de este módulo:
* `RpcChainClient`  : habla JSON-RPC con el nodo (pasos 2 y 6).
* `SimulatedChain`  : sustituto local del nodo para desarrollo/pruebas;
                      recorre mempool -> bloque -> recibo de forma determinista.
* `register_payment`: el conductor reporta el txHash firmado por su billetera.
* `sync_payment` / `sync_pending_payments`: el indexador (paso 7). Lee el recibo,
  verifica el evento `Transfer` del token contra el intento y, al alcanzar las
  confirmaciones exigidas, acredita la recarga en el wallet del conductor
  (idempotente: una sola `WalletTransaction` por pago).

No se usa `web3.py`: con httpx y JSON-RPC crudo basta y evita una dependencia
pesada. Solo se necesita el topic0 de `Transfer(address,address,uint256)`,
que es una constante bien conocida del estándar ERC-20.
"""
from __future__ import annotations

import asyncio
import secrets
import uuid
from dataclasses import dataclass
from datetime import datetime, timezone

import httpx
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.config import get_settings
from app.models.billing import WalletTransaction
from app.models.blockchain import BlockchainPayment
from app.models.enums import BlockchainTxStatus, WalletTxType
from app.models.user import DriverProfile

settings = get_settings()

# keccak256("Transfer(address,address,uint256)") — constante del estándar ERC-20
TRANSFER_TOPIC = "0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef"

_HEX32 = 64


class ChainError(Exception):
    """Error de negocio con código estable para que la app pueda reaccionar."""

    def __init__(self, code: str, message: str):
        super().__init__(message)
        self.code = code
        self.message = message


def _is_address(value: str) -> bool:
    return (
        isinstance(value, str)
        and value.startswith("0x")
        and len(value) == 42
        and all(c in "0123456789abcdefABCDEF" for c in value[2:])
    )


def _is_tx_hash(value: str) -> bool:
    return (
        isinstance(value, str)
        and value.startswith("0x")
        and len(value) == 66
        and all(c in "0123456789abcdefABCDEF" for c in value[2:])
    )


def _topic_to_address(topic: str) -> str:
    """Toma los últimos 20 bytes de un topic de 32 bytes como dirección."""
    return "0x" + topic[-40:]


def _hex_to_int(value: str) -> int:
    return int(value, 16)


def token_units_to_cop(units: int, decimals: int, cop_per_token: int) -> int:
    """Convierte unidades mínimas del token a COP (redondeo hacia abajo)."""
    return (units * cop_per_token) // (10**decimals)


@dataclass(frozen=True)
class Receipt:
    """Recibo decodificado (paso 6): status, bloque, gas y logs relevantes."""

    tx_hash: str
    status: int                 # 1 = éxito, 0 = revertido
    block_number: int | None
    gas_used: int | None
    logs: list[dict]


# ───────────────────────── CADENA SIMULADA (dev / pruebas) ─────────────────────────

class SimulatedChain:
    """Sustituto del nodo RPC para desarrollo local y pruebas.

    Imita el ciclo del nodo real sin red:
      * `submit`  -> devuelve un txHash y el tx queda "en mempool".
      * `get_receipt` -> None mientras esté en mempool; tras `block_delay`
        consultas devuelve un recibo con logs `Transfer` válidos.
    """

    def __init__(self, block_delay: int = 2):
        self._block_delay = block_delay
        self._counter = 0
        self._txs: dict[str, dict] = {}

    def submit(self, payer: str, token: str, to: str, units: int) -> str:
        self._counter += 1
        tx_hash = "0x" + secrets.token_hex(32)
        self._txs[tx_hash] = {
            "payer": payer.lower(),
            "token": token.lower(),
            "to": to.lower(),
            "units": units,
            "polls": 0,
            "status": 1,
        }
        return tx_hash

    def submit_reverting(self, payer: str, token: str, to: str, units: int) -> str:
        """Programa un tx que se revertirá en el bloque (status 0x0)."""
        tx_hash = self.submit(payer, token, to, units)
        self._txs[tx_hash]["status"] = 0
        return tx_hash

    async def get_receipt(self, tx_hash: str) -> Receipt | None:
        tx = self._txs.get(tx_hash)
        if tx is None:
            return None  # sigue en el mempool (o hash desconocido)
        tx["polls"] += 1
        if tx["polls"] < self._block_delay:
            return None  # aún esperando su turno (mempool)
        logs: list[dict] = []
        if tx["status"] == 1:
            logs.append(
                {
                    "address": tx["token"],
                    "topics": [
                        TRANSFER_TOPIC,
                        "0x" + "0" * 24 + tx["payer"][2:],   # from (payer)
                        "0x" + "0" * 24 + tx["to"][2:],      # to (tesorería)
                    ],
                    "data": hex(tx["units"]),
                }
            )
        return Receipt(
            tx_hash=tx_hash,
            status=tx["status"],
            block_number=1_000_000 + self._counter,
            gas_used=21_000 + self._counter,
            logs=logs,
        )

    @property
    def pending_count(self) -> int:
        """Mempool simulado: txs que aún no tienen recibo."""
        return sum(1 for t in self._txs.values() if t["polls"] < self._block_delay)

    async def confirmations_for(self, receipt: Receipt) -> int:
        """En la simulación un bloque incluido ya es definitivo."""
        return settings.required_confirmations


# ───────────────────────── NODO RPC REAL (paso 2) ─────────────────────────

class RpcChainClient:
    """Cliente JSON-RPC mínimo contra un nodo EVM (eth_getTransactionReceipt)."""

    def __init__(self, rpc_url: str, timeout: float = 10.0):
        self._rpc_url = rpc_url
        self._timeout = timeout

    async def _call(self, method: str, params: list) -> dict | None:
        payload = {"jsonrpc": "2.0", "id": 1, "method": method, "params": params}
        async with httpx.AsyncClient(timeout=self._timeout) as client:
            resp = await client.post(self._rpc_url, json=payload)
            resp.raise_for_status()
            body = resp.json()
        if body.get("error"):
            raise ChainError("RPC_ERROR", f"El nodo RPC rechazó la consulta: {body['error'].get('message')}")
        return body.get("result")

    async def get_receipt(self, tx_hash: str) -> Receipt | None:
        """None = aún no minado (mempool). status 0x0 = revertido."""
        raw = await self._call("eth_getTransactionReceipt", [tx_hash])
        if raw is None:
            return None
        status = _hex_to_int(raw.get("status", "0x0"))
        logs = raw.get("logs") or []
        return Receipt(
            tx_hash=tx_hash,
            status=status,
            block_number=_hex_to_int(raw["blockNumber"]) if raw.get("blockNumber") else None,
            gas_used=_hex_to_int(raw["gasUsed"]) if raw.get("gasUsed") else None,
            logs=logs,
        )

    async def block_number(self) -> int | None:
        raw = await self._call("eth_blockNumber", [])
        return _hex_to_int(raw) if raw is not None else None

    async def confirmations_for(self, receipt: Receipt) -> int | None:
        head = await self.block_number()
        if head is None or receipt.block_number is None:
            return None
        return head - receipt.block_number + 1


_chain: SimulatedChain | RpcChainClient | None = None


def get_chain() -> SimulatedChain | RpcChainClient:
    """Devuelve el cliente de cadena según la configuración (singleton)."""
    global _chain
    if _chain is None:
        if settings.chain_rpc_url:
            _chain = RpcChainClient(settings.chain_rpc_url)
        else:
            _chain = SimulatedChain()
    return _chain


def set_chain(client) -> None:
    """Permite a las pruebas inyectar una cadena simulada."""
    global _chain
    _chain = client


# ───────────────────────── 1. BILLETERA: registrar el intento ─────────────────────────

async def register_payment(
    session: AsyncSession,
    driver: DriverProfile,
    tx_hash: str,
    payer_address: str,
    token_amount_units: int,
) -> BlockchainPayment:
    """El conductor (su billetera) ya firmó y envió el tx; reporta el txHash.

    Se guarda el intento con los datos esperados; el indexador los contrastará
    contra el recibo real. Idempotente por txHash (índice único).
    """
    if not _is_tx_hash(tx_hash):
        raise ChainError("INVALID_TX_HASH", "El hash de la transacción no es válido")
    if not _is_address(payer_address):
        raise ChainError("INVALID_ADDRESS", "La dirección de la billetera no es válida")
    if not settings.payment_token_address or not _is_address(settings.payment_token_address):
        raise ChainError("CHAIN_NOT_CONFIGURED", "El método de pago cripto no está configurado")
    if token_amount_units <= 0:
        raise ChainError("INVALID_AMOUNT", "El monto debe ser positivo")

    amount_cop = token_units_to_cop(
        token_amount_units, settings.payment_token_decimals, settings.cop_per_token
    )
    if amount_cop < settings.topup_min_cop:
        raise ChainError("AMOUNT_TOO_LOW", f"El mínimo de recarga es ${settings.topup_min_cop:,} COP")
    if amount_cop > settings.topup_max_cop:
        raise ChainError("AMOUNT_TOO_HIGH", f"El máximo de recarga es ${settings.topup_max_cop:,} COP")

    existing = (
        await session.execute(
            select(BlockchainPayment).where(BlockchainPayment.tx_hash == tx_hash)
        )
    ).scalar_one_or_none()
    if existing is not None:
        raise ChainError("TX_ALREADY_REPORTED", "Esta transacción ya fue reportada")

    payment = BlockchainPayment(
        driver_id=driver.user_id,
        tx_hash=tx_hash.lower(),
        payer_address=payer_address.lower(),
        chain_id=settings.chain_id,
        status=BlockchainTxStatus.pending,
        token_address=settings.payment_token_address.lower(),
        token_symbol=settings.payment_token_symbol,
        token_amount_units=token_amount_units,
        decimals=settings.payment_token_decimals,
        amount_cop=amount_cop,
        cop_per_token=settings.cop_per_token,
    )
    session.add(payment)
    await session.flush()
    return payment


# ───────────────────────── 7. INDEXADOR ─────────────────────────

async def _verify_and_credit(session: AsyncSession, payment: BlockchainPayment, receipt: Receipt) -> None:
    """Verifica el evento Transfer contra el intento y acredita el saldo."""
    transfer = next(
        (
            log
            for log in receipt.logs
            if (log.get("address") or "").lower() == payment.token_address
            and len(log.get("topics") or []) >= 3
            and (log["topics"][0] or "").lower() == TRANSFER_TOPIC
            and _topic_to_address(log["topics"][2]).lower() == settings.treasury_address.lower()
        ),
        None,
    )
    if transfer is None or receipt.status != 1:
        payment.status = BlockchainTxStatus.failed
        payment.failure_reason = payment.failure_reason or "El tx no pagó a la tesorería con el token esperado"
        return

    # from debe ser la billetera del conductor y el monto el reportado
    if _topic_to_address(transfer["topics"][1]).lower() != payment.payer_address:
        payment.status = BlockchainTxStatus.failed
        payment.failure_reason = "El remitente no coincide con la billetera reportada"
        return

    data = transfer.get("data") or "0x0"
    units = _hex_to_int(data) if isinstance(data, str) and data.startswith("0x") else int(data, 16)
    if units < payment.token_amount_units:
        payment.status = BlockchainTxStatus.failed
        payment.failure_reason = "El monto transferido es menor al reportado"
        return

    payment.block_number = receipt.block_number
    payment.gas_used = receipt.gas_used
    payment.receipt = {"status": hex(receipt.status), "blockNumber": receipt.block_number, "gasUsed": receipt.gas_used}

    if payment.confirmations is None or payment.confirmations < settings.required_confirmations:
        payment.confirmations = await get_chain().confirmations_for(receipt)
        if payment.confirmations < settings.required_confirmations:
            payment.status = BlockchainTxStatus.confirming
            return

    await _credit_wallet(session, payment)


async def _credit_wallet(session: AsyncSession, payment: BlockchainPayment) -> None:
    """Acredita la recarga (idempotente: índice único en wallet_tx_id)."""
    if payment.status == BlockchainTxStatus.confirmed and payment.wallet_tx_id is not None:
        return
    driver = (
        await session.execute(
            select(DriverProfile).where(DriverProfile.user_id == payment.driver_id).with_for_update()
        )
    ).scalar_one_or_none()
    if driver is None:
        payment.status = BlockchainTxStatus.failed
        payment.failure_reason = "Conductor no encontrado"
        return

    tx = WalletTransaction(
        driver_id=payment.driver_id,
        type=WalletTxType.topup,
        amount_cop=payment.amount_cop,
        payment_method=payment.payment_method,
        payment_reference=payment.tx_hash,
        note=f"Recarga cripto ({payment.token_symbol}) confirmada on-chain",
    )
    session.add(tx)
    await session.flush()
    payment.wallet_tx_id = tx.id

    driver.wallet_balance_cop += payment.amount_cop
    tx.balance_after_cop = driver.wallet_balance_cop

    payment.status = BlockchainTxStatus.confirmed
    payment.confirmed_at = datetime.now(timezone.utc)


async def sync_payment(session: AsyncSession, payment: BlockchainPayment) -> BlockchainPayment:
    """Un paso del indexador para un pago: mempool -> recibo -> verificación."""
    if payment.status in (BlockchainTxStatus.confirmed, BlockchainTxStatus.failed):
        payment.indexed_at = payment.indexed_at or datetime.now(timezone.utc)
        return payment

    receipt = await get_chain().get_receipt(payment.tx_hash)
    if receipt is None:
        # Paso 3: sigue esperando su turno en el mempool
        payment.indexed_at = datetime.now(timezone.utc)
        return payment

    payment.indexed_at = datetime.now(timezone.utc)
    if receipt.status != 1:
        payment.status = BlockchainTxStatus.failed
        payment.failure_reason = payment.failure_reason or "La transacción fue revertida por la EVM"
        payment.receipt = {"status": hex(receipt.status), "blockNumber": receipt.block_number}
        return payment

    await _verify_and_credit(session, payment, receipt)
    await session.flush()
    return payment


async def sync_pending_payments(session: AsyncSession) -> list[BlockchainPayment]:
    """Barrido del indexador sobre todos los pagos que no están resueltos."""
    rows = (
        await session.execute(
            select(BlockchainPayment).where(
                BlockchainPayment.status.in_([BlockchainTxStatus.pending, BlockchainTxStatus.confirming])
            )
        )
    ).scalars()
    results = []
    for payment in rows:
        results.append(await sync_payment(session, payment))
    await session.flush()
    return results
