from fastapi import APIRouter, WebSocket, WebSocketDisconnect, Query
from app.api.v1.ws.manager import manager

router = APIRouter()


@router.websocket("/tracking/{pedido_id}")
async def tracking_websocket(websocket: WebSocket, pedido_id: str, token: str = Query(...)):
    await manager.connect(f"tracking_{pedido_id}", websocket)
    try:
        while True:
            data = await websocket.receive_json()
            # Broadcast location update to all listeners (clients)
            await manager.broadcast(f"tracking_{pedido_id}", data)
    except WebSocketDisconnect:
        manager.disconnect(f"tracking_{pedido_id}", websocket)
