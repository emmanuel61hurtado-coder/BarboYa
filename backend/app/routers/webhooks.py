import hashlib
import hmac
import json

from fastapi import APIRouter, Header, HTTPException, Request, status

from app.core.config import get_settings

router = APIRouter(prefix="/webhooks", tags=["webhooks"])
settings = get_settings()


@router.post("/payments")
async def payment_webhook(request: Request, x_signature: str | None = Header(default=None)):
    """Solo acepta avisos firmados con HMAC-SHA256 usando WEBHOOK_SECRET."""
    if not settings.webhook_secret:
        raise HTTPException(status.HTTP_503_SERVICE_UNAVAILABLE, "Webhook de pagos sin configurar")

    body = await request.body()
    expected = hmac.new(settings.webhook_secret.encode(), body, hashlib.sha256).hexdigest()
    if not x_signature or not hmac.compare_digest(x_signature.encode(), expected.encode()):
        raise HTTPException(status.HTTP_401_UNAUTHORIZED, "Firma de webhook inválida")

    try:
        event = json.loads(body).get("event", "unknown")
    except (ValueError, AttributeError) as exc:
        raise HTTPException(status.HTTP_400_BAD_REQUEST, "Cuerpo del webhook inválido") from exc
    return {"received": True, "event": event}
