from fastapi import APIRouter, Depends, Query, WebSocket, WebSocketDisconnect, status
from sqlalchemy.ext.asyncio import AsyncSession

from app.db.session import get_db
from app.routes.v1.ws.auth import authenticate_ws, can_access_pedido
from app.routes.v1.ws.manager import manager

router = APIRouter()


@router.websocket("/pedidos/{pedido_id}")
async def pedidos_websocket(
    websocket: WebSocket,
    pedido_id: str,
    token: str = Query(...),
    db: AsyncSession = Depends(get_db),
):
    user = await authenticate_ws(token, db)
    if user is None:
        await websocket.close(code=status.WS_1008_POLICY_VIOLATION)
        return
    allowed, _ = await can_access_pedido(user, pedido_id, db)
    if not allowed:
        await websocket.close(code=status.WS_1008_POLICY_VIOLATION)
        return

    await manager.connect(pedido_id, websocket)
    try:
        while True:
            await websocket.receive_text()
    except WebSocketDisconnect:
        manager.disconnect(pedido_id, websocket)
