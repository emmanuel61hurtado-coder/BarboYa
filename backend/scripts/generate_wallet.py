"""Generador de wallets Ethereum (para la tesorería de Barboya).

Implementación en Python puro: keccak-256 + curva secp256k1. Sin dependencias
externas, ideal para entornos sin web3/eth-account instalados.

Uso:
    python scripts/generate_wallet.py            # genera una wallet nueva
    python scripts/generate_wallet.py --selftest # verifica contra vectores conocidos

La wallet generada es la tesorería que recibe los pagos USDC (ver
docs/blockchain-payments.md). La clave privada SOLO se usa para:
    * importarla en MetaMask y enviar/recibir test USDC
    * firmar como admin si algún día se necesitan retiros automáticos
NO la subas al repositorio: va en variables de entorno del despliegue.
"""
from __future__ import annotations

import secrets
import sys

# ─────────────────────────────── KECCAK-256 ───────────────────────────────

_ROUND_CONSTANTS = [
    0x0000000000000001, 0x0000000000008082, 0x800000000000808A, 0x8000000080008000,
    0x000000000000808B, 0x0000000080000001, 0x8000000080008081, 0x8000000000008009,
    0x000000000000008A, 0x0000000000000088, 0x0000000080008009, 0x000000008000000A,
    0x000000008000808B, 0x800000000000008B, 0x8000000000008089, 0x8000000000008003,
    0x8000000000008002, 0x8000000000000080, 0x000000000000800A, 0x800000008000000A,
    0x8000000080008081, 0x8000000000008080, 0x0000000080000001, 0x8000000080008008,
]

# Offsets de rotación ρ por (x, y)
_ROT = [
    [0, 36, 3, 41, 18],   # x=0
    [1, 44, 10, 45, 2],   # x=1
    [62, 6, 43, 15, 61],  # x=2
    [28, 55, 25, 21, 56], # x=3
    [27, 20, 39, 8, 14],  # x=4
]

_MASK = (1 << 64) - 1


def _rotl64(v: int, n: int) -> int:
    n %= 64
    return ((v << n) | (v >> (64 - n))) & _MASK if n else v


def _keccak_f(state: list[list[int]]) -> None:
    for rc in _ROUND_CONSTANTS:
        # θ
        c = [state[x][0] ^ state[x][1] ^ state[x][2] ^ state[x][3] ^ state[x][4] for x in range(5)]
        d = [c[(x - 1) % 5] ^ _rotl64(c[(x + 1) % 5], 1) for x in range(5)]
        for x in range(5):
            for y in range(5):
                state[x][y] ^= d[x]
        # ρ y π
        b = [[0] * 5 for _ in range(5)]
        for x in range(5):
            for y in range(5):
                b[y][(2 * x + 3 * y) % 5] = _rotl64(state[x][y], _ROT[x][y])
        # χ
        for x in range(5):
            for y in range(5):
                state[x][y] = b[x][y] ^ ((b[(x + 1) % 5][y] ^ _MASK) & b[(x + 2) % 5][y])
        # ι
        state[0][0] ^= rc


def keccak256(data: bytes) -> bytes:
    """Keccak-256 (el de Ethereum, con padding 0x01 — no es SHA3-256)."""
    rate = 136  # bytes (1088 bits)
    state = [[0] * 5 for _ in range(5)]
    padded = bytearray(data)
    padded.append(0x01)  # padding keccak
    while len(padded) % rate != 0:
        padded.append(0x00)
    padded[-1] |= 0x80

    for block_start in range(0, len(padded), rate):
        block = padded[block_start:block_start + rate]
        for i in range(rate // 8):
            lane = int.from_bytes(block[i * 8:(i + 1) * 8], "little")
            x, y = i % 5, i // 5
            state[x][y] ^= lane
        _keccak_f(state)

    out = bytearray()
    for y in range(5):
        for x in range(5):
            out += state[x][y].to_bytes(8, "little")
    return bytes(out[:32])


# ─────────────────────────────── SECP256K1 ───────────────────────────────

_P = 2**256 - 2**32 - 977
_N = 0xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEBAAEDCE6AF48A03BBFD25E8CD0364141
_GX = 0x79BE667EF9DCBBAC55A06295CE870B07029BFCDB2DCE28D959F2815B16F81798
_GY = 0x483ADA7726A3C4655DA4FBFC0E1108A8FD934E9FDBDF7A293C9D645C0D4CB257


def _point_add(p1: tuple[int, int] | None, p2: tuple[int, int] | None) -> tuple[int, int] | None:
    if p1 is None:
        return p2
    if p2 is None:
        return p1
    x1, y1 = p1
    x2, y2 = p2
    if x1 == x2 and (y1 + y2) % _P == 0:
        return None
    if p1 == p2:
        lam = (3 * x1 * x1) * pow(2 * y1, _P - 2, _P) % _P
    else:
        lam = (y2 - y1) * pow(x2 - x1, _P - 2, _P) % _P
    x3 = (lam * lam - x1 - x2) % _P
    y3 = (lam * (x1 - x3) - y1) % _P
    return (x3, y3)


def _point_mul(k: int, point: tuple[int, int]) -> tuple[int, int]:
    result = None
    addend = point
    while k:
        if k & 1:
            result = _point_add(result, addend)
        addend = _point_add(addend, addend)
        k >>= 1
    return result  # type: ignore[return-value]


# ─────────────────────────────── WALLET ───────────────────────────────

def eip55_address(address_20b: bytes) -> str:
    """Formatea la dirección con checksum EIP-55 (mayúsculas válidas)."""
    hex_addr = address_20b.hex()
    digest = keccak256(hex_addr.encode("ascii")).hex()
    return "0x" + "".join(
        ch.upper() if int(digest[i], 16) >= 8 else ch for i, ch in enumerate(hex_addr)
    )


def generate_wallet() -> dict:
    """Genera una wallet Ethereum nueva: clave privada aleatoria de 32 bytes."""
    while True:
        priv = secrets.token_bytes(32)
        if 0 < int.from_bytes(priv, "big") < _N:
            break
    pub_x, pub_y = _point_mul(int.from_bytes(priv, "big"), (_GX, _GY))
    pub_bytes = pub_x.to_bytes(32, "big") + pub_y.to_bytes(32, "big")
    address = eip55_address(keccak256(pub_bytes)[-20:])
    return {
        "address": address,
        "private_key": "0x" + priv.hex(),
        "pub_x": hex(pub_x),
        "pub_y": hex(pub_y),
    }


def _selftest() -> bool:
    ok = True
    vectors = [
        (b"", "c5d2460186f7233c927e7db2dcc703c0e500b653ca82273b7bfad8045d85a470"),
        (b"Transfer(address,address,uint256)",
         "ddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef"),
        (b"abc", "4e03657aea45a94fc7d47ba826c8d667c0d1e6e33a64a036ec44f58fa12d6c45"),
    ]
    for data, expected in vectors:
        got = keccak256(data).hex()
        status = "OK" if got == expected else "FAIL"
        if got != expected:
            ok = False
        print(f"  [{status}] keccak256({data[:40]!r}...) = {got}")
    return ok


if __name__ == "__main__":
    if "--selftest" in sys.argv:
        print("Verificación de keccak256:")
        sys.exit(0 if _selftest() else 1)
    w = generate_wallet()
    print("Wallet de tesorería generada (usar SOLO en testnet hasta pasar a producción):")
    print(f"  address      : {w['address']}")
    print(f"  private_key  : {w['private_key']}")
    print("Guárdala en las variables de entorno del despliegue, nunca en el repositorio.")
