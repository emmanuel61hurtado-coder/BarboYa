import logging

from fastapi import APIRouter, WebSocket, WebSocketDisconnect
from sqlalchemy import func

from app.core.database import SessionLocal
from app.core.security import decode_token
from app.models.user import DriverProfile
from app.websockets.manager import manager
from uuid import UUID

log = logging.getLogger("barboya.ws")
router = APIRouter()


@router.websocket("/ws")
async def websocket_endpoint(ws: WebSocket, token: str):
    """Canal único en tiempo real. Autenticación por query param ?token=JWT."""
    try:
        payload = decode_token(token)
        user_id, role = UUID(payload["sub"]), payload["role"]
    except Exception:
        await ws.close(code=4401)
        return

    await manager.connect(user_id, ws)
    try:
        while True:
            msg = await ws.receive_json()
            kind, data = msg.get("type"), msg.get("payload", {})

            if kind == "ping":
                await manager.send_to(user_id, "pong", {})

            elif kind == "driver.location" and role == "driver":
                lat, lng = float(data["lat"]), float(data["lng"])
                await manager.set_driver_online(user_id, lat, lng)
                async with SessionLocal() as s:
                    d = await s.get(DriverProfile, user_id)
                    if d:
                        d.current_location = func.ST_SetSRID(func.ST_MakePoint(lng, lat), 4326)
                        d.is_online = True
                        await s.commit()

            elif kind == "driver.offline" and role == "driver":
                await manager.set_driver_offline(user_id)
                async with SessionLocal() as s:
                    d = await s.get(DriverProfile, user_id)
                    if d:
                        d.is_online = False
                        await s.commit()
    except WebSocketDisconnect:
        pass
    except Exception:
        log.exception("error en websocket de %s", user_id)
    finally:
        await manager.disconnect(user_id, ws)
