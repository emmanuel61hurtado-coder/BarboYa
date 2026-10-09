from fastapi import APIRouter, Depends, Query, WebSocket, WebSocketDisconnect, status
from sqlalchemy.ext.asyncio import AsyncSession

from app.db.session import get_db
from app.models.user import UserRole
from app.routes.v1.ws.auth import authenticate_ws, can_access_pedido
from app.routes.v1.ws.manager import manager

router = APIRouter()


@router.websocket("/tracking/{pedido_id}")
async def tracking_websocket(
    websocket: WebSocket,
    pedido_id: str,
    token: str = Query(...),
    db: AsyncSession = Depends(get_db),
):
    user = await authenticate_ws(token, db)
    if user is None:
        await websocket.close(code=status.WS_1008_POLICY_VIOLATION)
        return
    allowed, pedido = await can_access_pedido(user, pedido_id, db)
    if not allowed:
        await websocket.close(code=status.WS_1008_POLICY_VIOLATION)
        return
    # Solo el repartidor asignado (o un admin) puede publicar ubicacion; el resto solo escucha.
    can_publish = user.rol == UserRole.ADMIN or (pedido is not None and pedido.repartidor_id == user.id)

    room = f"tracking_{pedido_id}"
    await manager.connect(room, websocket)
    try:
        while True:
            data = await websocket.receive_json()
            if can_publish:
                await manager.broadcast(room, data)
    except WebSocketDisconnect:
        manager.disconnect(room, websocket)
