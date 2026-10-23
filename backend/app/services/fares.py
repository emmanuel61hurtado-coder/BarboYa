"""Tarifa sugerida: base + km * tarifa/km de la zona, con recargo nocturno."""
from __future__ import annotations

import math
from datetime import datetime, time
from zoneinfo import ZoneInfo

from sqlalchemy import func, select
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.zone import Zone

BOGOTA = ZoneInfo("America/Bogota")
FALLBACK = {"base": 3000, "per_km": 800, "night_pct": 20.0, "night_start": time(20, 0), "night_end": time(5, 0)}


def haversine_m(lat1: float, lng1: float, lat2: float, lng2: float) -> int:
    r = 6_371_000
    p1, p2 = math.radians(lat1), math.radians(lat2)
    dphi = p2 - p1
    dl = math.radians(lng2 - lng1)
    a = math.sin(dphi / 2) ** 2 + math.cos(p1) * math.cos(p2) * math.sin(dl / 2) ** 2
    return int(2 * r * math.asin(math.sqrt(a)))


def is_night(now: datetime, start: time, end: time) -> bool:
    t = now.astimezone(BOGOTA).time()
    return t >= start or t < end if start > end else start <= t < end


def round_to_100(value: float) -> int:
    return int(round(value / 100.0) * 100)


async def find_zone(session: AsyncSession, lat: float, lng: float) -> Zone | None:
    point = func.ST_SetSRID(func.ST_MakePoint(lng, lat), 4326)
    stmt = select(Zone).where(Zone.is_active.is_(True), func.ST_Contains(Zone.boundary, point)).limit(1)
    return (await session.execute(stmt)).scalar_one_or_none()


async def quote(
    session: AsyncSession, pickup: tuple[float, float], dropoff: tuple[float, float], now: datetime | None = None
) -> dict:
    now = now or datetime.now(BOGOTA)
    zone = await find_zone(session, *pickup)
    distance = haversine_m(*pickup, *dropoff)
    km = distance / 1000

    if zone:
        base, per_km = zone.base_fare_cop, zone.per_km_cop
        pct, ns, ne = float(zone.night_surcharge_pct), zone.night_start, zone.night_end
    else:
        base, per_km, pct, ns, ne = (FALLBACK[k] for k in ("base", "per_km", "night_pct", "night_start", "night_end"))

    fare = base + km * per_km
    night = is_night(now, ns, ne)
    if night:
        fare *= 1 + pct / 100
    suggested = round_to_100(fare)
    return {
        "distance_m": distance,
        "suggested_fare_cop": suggested,
        "min_fare_cop": round_to_100(suggested * 0.8),
        "max_fare_cop": round_to_100(suggested * 1.5),
        "night_surcharge_applied": night,
        "zone_name": zone.name if zone else None,
        "zone_id": zone.id if zone else None,
    }
