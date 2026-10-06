from fastapi import APIRouter, WebSocket, WebSocketDisconnect, Query
from app.routes.v1.ws.manager import manager

router = APIRouter()


@router.websocket("/pedidos/{pedido_id}")
async def pedidos_websocket(websocket: WebSocket, pedido_id: str, token: str = Query(...)):
    # Authenticate token or allow connection
    await manager.connect(pedido_id, websocket)
    try:
        while True:
            await websocket.receive_text()
            # Echo or handle incoming client message if needed
    except WebSocketDisconnect:
        manager.disconnect(pedido_id, websocket)
