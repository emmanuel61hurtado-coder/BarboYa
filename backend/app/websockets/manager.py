"""Manager de WebSockets asíncrono para la negociación "Pon Tu Precio".

Topología
---------
* Cada usuario conectado tiene un socket (``user_id -> WebSocket``).
* Cada viaje tiene una "sala" (``trip_id -> {user_id}``) con el cliente y los
  conductores que han ofertado.
* Los conductores online se indexan por viaje cercano: al crear un viaje, el
  servicio llama a ``broadcast_new_trip`` con los ``driver_ids`` cercanos
  (calculados con PostGIS ``ST_DWithin``).
* Con varias instancias del backend se sincroniza vía Redis Pub/Sub
  (``RedisBridge``); en una sola instancia funciona sin Redis.

Mensajes (JSON) -> siempre ``{"type": "...", "payload": {...}}``
"""
from __future__ import annotations

import asyncio
import json
import logging
from collections import defaultdict
from typing import Any
from uuid import UUID

from fastapi import WebSocket

log = logging.getLogger("barboya.ws")

# Tipos de evento
TRIP_NEW = "trip.new"                  # servidor -> conductores cercanos
OFFER_NEW = "offer.new"                # conductor -> servidor -> cliente
OFFER_COUNTER = "offer.counter"        # cliente -> servidor -> conductor
OFFER_ACCEPT = "offer.accept"          # cliente -> servidor
OFFER_ACCEPTED = "offer.accepted"      # servidor -> ambos
OFFER_REJECTED = "offer.rejected"
OFFER_EXPIRED = "offer.expired"
TRIP_STATUS = "trip.status"
DRIVER_LOCATION = "driver.location"
ERROR = "error"


class ConnectionManager:
    def __init__(self) -> None:
        self._sockets: dict[UUID, WebSocket] = {}
        self._rooms: dict[UUID, set[UUID]] = defaultdict(set)
        self._lock = asyncio.Lock()
        self._redis: Any | None = None  # redis.asyncio.Redis opcional

    # ------------------------------------------------------------------ ciclo de vida
    async def connect(self, user_id: UUID, ws: WebSocket) -> None:
        await ws.accept()
        async with self._lock:
            old = self._sockets.get(user_id)
            self._sockets[user_id] = ws
        if old is not None:  # una sola sesión por usuario: cerramos la anterior
            try:
                await old.close(code=4000)
            except RuntimeError:
                pass

    async def disconnect(self, user_id: UUID, ws: WebSocket | None = None) -> None:
        async with self._lock:
            current = self._sockets.get(user_id)
            if ws is not None and current is not ws:
                return  # ya fue reemplazado por una sesión más nueva
            self._sockets.pop(user_id, None)
            for members in self._rooms.values():
                members.discard(user_id)

    def is_online(self, user_id: UUID) -> bool:
        return user_id in self._sockets

    # ------------------------------------------------------------------ salas por viaje
    async def join_trip(self, trip_id: UUID, user_id: UUID) -> None:
        async with self._lock:
            self._rooms[trip_id].add(user_id)

    async def leave_trip(self, trip_id: UUID, user_id: UUID) -> None:
        async with self._lock:
            self._rooms[trip_id].discard(user_id)

    async def close_room(self, trip_id: UUID) -> None:
        async with self._lock:
            self._rooms.pop(trip_id, None)

    # ------------------------------------------------------------------ envío
    async def send_to(self, user_id: UUID, event_type: str, payload: dict[str, Any]) -> bool:
        ws = self._sockets.get(user_id)
        if ws is None:
            return False
        try:
            await ws.send_text(json.dumps({"type": event_type, "payload": payload}, default=str))
            return True
        except Exception:  # socket roto: lo limpiamos
            log.warning("socket roto para %s, desconectando", user_id)
            await self.disconnect(user_id, ws)
            return False

    async def send_many(self, user_ids: list[UUID], event_type: str, payload: dict[str, Any]) -> int:
        results = await asyncio.gather(*(self.send_to(u, event_type, payload) for u in user_ids))
        return sum(1 for ok in results if ok)

    async def broadcast_room(
        self, trip_id: UUID, event_type: str, payload: dict[str, Any], exclude: UUID | None = None
    ) -> int:
        members = [u for u in self._rooms.get(trip_id, set()) if u != exclude]
        return await self.send_many(members, event_type, payload)

    # ------------------------------------------------------------------ flujos de negocio
    async def broadcast_new_trip(self, trip_id: UUID, nearby_driver_ids: list[UUID], payload: dict[str, Any]) -> int:
        """Avisa del nuevo viaje solo a conductores cercanos y elegibles."""
        return await self.send_many(nearby_driver_ids, TRIP_NEW, {"trip_id": str(trip_id), **payload})

    async def driver_offer(
        self, trip_id: UUID, customer_id: UUID, driver_id: UUID, offer: dict[str, Any]
    ) -> bool:
        """Conductor ofrece/contraoferta -> llega al cliente como tarjeta flotante."""
        await self.join_trip(trip_id, driver_id)
        await self.join_trip(trip_id, customer_id)
        return await self.send_to(
            customer_id, OFFER_NEW, {"trip_id": str(trip_id), "driver_id": str(driver_id), **offer}
        )

    async def customer_counter(
        self, trip_id: UUID, driver_id: UUID, amount_cop: int, customer_id: UUID
    ) -> bool:
        """El cliente responde a un conductor con un nuevo monto."""
        return await self.send_to(
            driver_id,
            OFFER_COUNTER,
            {"trip_id": str(trip_id), "customer_id": str(customer_id), "amount_cop": amount_cop},
        )

    async def notify_accepted(
        self, trip_id: UUID, customer_id: UUID, winner_driver_id: UUID, payload: dict[str, Any]
    ) -> None:
        """Confirma al ganador y descarta al resto de conductores que ofertaron."""
        members = set(self._rooms.get(trip_id, set()))
        await self.send_to(customer_id, OFFER_ACCEPTED, {"trip_id": str(trip_id), **payload})
        await self.send_to(winner_driver_id, OFFER_ACCEPTED, {"trip_id": str(trip_id), **payload})
        losers = [u for u in members if u not in (customer_id, winner_driver_id)]
        await self.send_many(losers, OFFER_REJECTED, {"trip_id": str(trip_id), "reason": "trip_taken"})
        await self.close_room(trip_id)

    # ------------------------------------------------------------------ Redis (multi-instancia)
    async def attach_redis(self, redis_client: Any) -> None:
        self._redis = redis_client

    async def set_driver_online(self, driver_id: UUID, lat: float, lng: float) -> None:
        """Estado volátil del conductor en Redis (GEO + TTL)."""
        if self._redis is None:
            return
        await self._redis.geoadd("drivers:online", (lng, lat, str(driver_id)))
        await self._redis.set(f"driver:{driver_id}:alive", "1", ex=60)

    async def set_driver_offline(self, driver_id: UUID) -> None:
        if self._redis is None:
            return
        await self._redis.zrem("drivers:online", str(driver_id))
        await self._redis.delete(f"driver:{driver_id}:alive")

    async def nearby_drivers(self, lat: float, lng: float, radius_m: int = 3000, limit: int = 20) -> list[UUID]:
        if self._redis is None:
            return []
        raw = await self._redis.geosearch(
            "drivers:online", longitude=lng, latitude=lat, radius=radius_m, unit="m", count=limit, sort="ASC"
        )
        alive: list[UUID] = []
        for item in raw:
            driver_id = item.decode() if isinstance(item, bytes) else item
            if await self._redis.exists(f"driver:{driver_id}:alive"):
                alive.append(UUID(driver_id))
        return alive


manager = ConnectionManager()
