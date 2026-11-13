# Pagos con blockchain (EVM)

Método de pago `crypto` para recargas del wallet del conductor, siguiendo la
arquitectura de referencia (números = pasos del diagrama):

```
1. Billetera ──JSON-RPC──> 2. Nodo RPC ──difunde──> 3. Mempool
                                                    │ selecciona
8. PostgreSQL <──guarda── 7. Indexador <──emite── 6. Recibo y logs
                                                    │ produce
5. EVM <──ejecuta── 4. Bloque
```

| Paso | Qué ocurre | Dónde vive en el código |
|---|---|---|
| 1. Billetera | El conductor firma el tx en su wallet (MetaMask u otra) y reporta el `txHash` + monto esperado | `POST /payments/crypto` → `services/blockchain.register_payment` |
| 2. Nodo RPC | El backend consulta `eth_getTransactionReceipt` vía JSON-RPC | `services/blockchain.RpcChainClient` |
| 3. Mempool | Sin recibo aún ⇒ pago en `pending` | `BlockchainTxStatus.pending` |
| 4. Bloque | El productor incluye el tx en un bloque | (fuera del backend) |
| 5. EVM | Se ejecuta el `Transfer` del contrato del token | (fuera del backend) |
| 6. Recibo y logs | `status` (0x1/0x0), `blockNumber`, `gasUsed` y logs del evento | `services.blockchain.Receipt` |
| 7. Indexador | Lee los logs, verifica el evento `Transfer` y las confirmaciones | `services/blockchain.sync_pending_payments` (tarea de fondo en `main.py`) |
| 8. PostgreSQL | Al confirmar se acredita la recarga en el wallet (consultas en ms) | tabla `blockchain_payments` + `wallet_transactions` |

## Reglas de negocio

- Token esperado: stablecoin ERC-20 (por defecto **USDC**, 6 decimales) configurable.
- El evento `Transfer` del recibo debe: ir al contrato del token configurado,
  tener como destino la **tesorería** de Barboya, venir **de** la billetera
  reportada y transferir **igual o más** de lo declarado. Si no, el pago pasa a
  `failed` y no se acredita nada.
- Confirmaciones mínimas exigidas antes de acreditar (12 por defecto).
- Conversión a COP fijada al registrar el intento (`COP_PER_TOKEN`), así la
  volatilidad no cambia el valor de un pago ya reportado.
- Idempotente: un solo `txHash` (índice único) y una sola `WalletTransaction`
  por pago confirmado, aunque el indexador corra muchas veces.
- Límites de recarga: los mismos del wallet (`$1.000 – $500.000` COP).

## Estados (`BlockchainTxStatus`)

```
pending ──(recibo existe, < confirmaciones)──> confirming ──> confirmed
   │                                              │
   └──(recibo 0x0 o datos no coinciden)───────────┴──────────> failed
```

## API

**Conductor** (requiere JWT de driver):

| Ruta | Descripción |
|---|---|
| `POST /payments/crypto` | Registra el intento: `{tx_hash, payer_address, token_amount_units}` (unidades mínimas del token: 1 USDC = `1_000_000`) |
| `GET /payments/crypto` | Historial de sus pagos on-chain |
| `GET /payments/crypto/{id}` | Estado actual; sincroniza contra el nodo al consultar |

**Admin**:

| Ruta | Descripción |
|---|---|
| `POST /admin/payments/crypto/sync` | Fuerza un barrido del indexador |

## Modo simulado (desarrollo y pruebas)

Sin `CHAIN_RPC_URL` ni `PAYMENT_TOKEN_ADDRESS` configurados el método queda
inactivo (HTTP 503). Para desarrollo local sin nodo se puede configurar el
token y omitir el RPC: `services.blockchain.SimulatedChain` reemplaza al nodo y
recorre mempool → bloque → recibo de forma determinista (2 consultas: primera
`pending`, segunda confirma). Las pruebas de
`backend/tests/test_blockchain_payments.py` cubren el ciclo completo.

## Producción

1. Configura en el entorno: `CHAIN_RPC_URL`, `CHAIN_ID`, `PAYMENT_TOKEN_ADDRESS`,
   `TREASURY_ADDRESS`, `COP_PER_TOKEN`, `REQUIRED_CONFIRMATIONS`.
2. La tarea de fondo (`_blockchain_indexer` en `app/main.py`) arranca sola con la
   API y sincroniza cada `INDEXER_INTERVAL_SECONDS`.
3. Recomendado: ejecutar el RPC contra un proveedor (Alchemy/Infura/Ankr) y usar
   una red barata (Polygon) por el costo del gas.

### Configuración actual (testnet, ya aplicada en `backend/.env`)

| Variable | Valor |
|---|---|
| Red | Polygon PoS Amoy (testnet), `CHAIN_ID=80002` |
| RPC | `https://polygon-amoy-bor-rpc.publicnode.com` |
| Token | USDC nativo de Circle: `0x41E94Eb019C0762f9Bfcf9Fb1E58725BfB0e7582` (6 decimales) |
| Tesorería | Generada con `backend/scripts/generate_wallet.py` (ver `backend/.env`) |
| Conversión | 1 USDC ≈ 4.000 COP |
| Confirmaciones | 12 (≈ 24 s: en Amoy los bloques tardan ~2 s) |

Fondos de prueba: USDC → `https://faucet.circle.com` · POL para gas → `https://faucet.polygon.technology`.

### Wallet de tesorería

```bash
cd backend
python scripts/generate_wallet.py            # genera address + private_key
python scripts/generate_wallet.py --selftest # valida keccak256 contra vectores conocidos
```

* Importa la clave privada en MetaMask para gestionar los fondos de prueba.
* La API **no** necesita la clave privada (solo recibe pagos); guárdala en tu
  gestor de secretos. `.gitignore` ya excluye `*.key` y `treasury*`.
* Para producción: genera una wallet nueva, cambia a Polygon mainnet
  (`CHAIN_ID=137`, RPC propio, USDC `0x3c499c542cEF5E3811e1192ce70d8cC03d5c3359`)
  y actualiza `COP_PER_TOKEN` con la tasa real del día.
