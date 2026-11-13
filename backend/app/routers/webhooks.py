import hmac
import hashlib
from fastapi import APIRouter, Depends, HTTPException, Header, Request, status
from sqlalchemy.ext.asyncio import AsyncSession
from app.core.database import get_session
from app.core.config import get_settings

router = APIRouter(prefix="/webhooks", tags=["webhooks"])
settings = get_settings()


@router.post("/payments")
async def payment_webhook(
    request: Request,
    x_signature: str | None = Header(default=None),
    session: AsyncSession = Depends(get_session),
):
    body_bytes = await request.body()
    # Verificación criptográfica HMAC-SHA256 de la firma del webhook de pago
    secret = getattr(settings, "webhook_secret", settings.jwt_secret).encode()
    expected = hmac.new(secret, body_bytes, hashlib.sha256).hexdigest()

    if x_signature and not hmac.compare_digest(x_signature, expected):
        raise HTTPException(status.HTTP_401_UNAUTHORIZED, "Firma de webhook inválida")

    payload = await request.json()
    event = payload.get("event", "unknown")
    return {"received": True, "event": event}
