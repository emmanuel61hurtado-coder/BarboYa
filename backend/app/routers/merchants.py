import random
from datetime import datetime, timezone
from uuid import UUID
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy import select, func, desc
from sqlalchemy.ext.asyncio import AsyncSession
from pydantic import BaseModel, Field, ConfigDict

from app.core.database import get_session
from app.core.security import CurrentUser, get_current_user, require_role
from app.models.merchant import Merchant, Order, Product
from app.models.user import DriverProfile, User
from app.models.enums import MerchantCategory, MerchantStatus, OrderStatus, PaymentMethod

router = APIRouter(prefix="/merchants", tags=["merchants"])
orders_router = APIRouter(prefix="/orders", tags=["orders"])


# ─────────────────────────────────────────────────────────────────────────────
# PYDANTIC SCHEMAS
# ─────────────────────────────────────────────────────────────────────────────

class GeoPoint(BaseModel):
    lat: float = Field(ge=-90, le=90)
    lng: float = Field(ge=-180, le=180)


class ProductIn(BaseModel):
    name: str = Field(min_length=2, max_length=120)
    description: str | None = None
    price_cop: int = Field(gt=0, le=5_000_000)
    image_url: str | None = None
    is_available: bool = True
    stock: int | None = None


class ProductOut(BaseModel):
    model_config = ConfigDict(from_attributes=True)
    id: UUID
    name: str
    description: str | None
    price_cop: int
    image_url: str | None
    is_available: bool
    stock: int | None = None


class MerchantRegisterIn(BaseModel):
    name: str = Field(min_length=2, max_length=120)
    category: MerchantCategory
    description: str | None = None
    address_note: str = Field(min_length=3, max_length=255)
    location: GeoPoint
    phone: str = Field(pattern=r"^\+?[0-9]{10,15}$")
    whatsapp: str | None = None
    opening_hours: str | None = Field(default="8:00 AM - 10:00 PM")
    logo_url: str | None = None
    cover_image_url: str | None = None


class MerchantOut(BaseModel):
    model_config = ConfigDict(from_attributes=True)
    id: UUID
    name: str
    category: MerchantCategory
    description: str | None
    logo_url: str | None
    cover_image_url: str | None = None
    address_note: str
    phone: str | None
    whatsapp: str | None
    opening_hours: str | None = None
    approval_status: str = "approved"
    is_open: bool


class MerchantUpdateIn(BaseModel):
    name: str | None = None
    description: str | None = None
    address_note: str | None = None
    phone: str | None = None
    whatsapp: str | None = None
    opening_hours: str | None = None
    logo_url: str | None = None
    cover_image_url: str | None = None
    is_open: bool | None = None


class OrderItemIn(BaseModel):
    product_id: UUID
    qty: int = Field(gt=0, le=50)


class OrderCreateIn(BaseModel):
    merchant_id: UUID
    items: list[OrderItemIn]
    payment_method: PaymentMethod = PaymentMethod.cash
    delivery_address_note: str | None = Field(default=None, max_length=200)
    delivery_location: GeoPoint | None = None


class OrderAcceptIn(BaseModel):
    prep_minutes: int = Field(default=25, ge=5, le=120)


class OrderRejectIn(BaseModel):
    reason: str = Field(min_length=3, max_length=255)


class DeliveryPinVerifyIn(BaseModel):
    pin: str = Field(min_length=4, max_length=4)


# ─────────────────────────────────────────────────────────────────────────────
# PUBLIC MERCHANT ROUTES
# ─────────────────────────────────────────────────────────────────────────────

@router.get("", response_model=list[MerchantOut])
async def list_merchants(category: MerchantCategory | None = None, session: AsyncSession = Depends(get_session)):
    try:
        stmt = select(Merchant).where(
            Merchant.is_active.is_(True),
            Merchant.approval_status == MerchantStatus.approved,
        )
        if category:
            stmt = stmt.where(Merchant.category == category)
        rows = (await session.execute(stmt)).scalars().all()
        return [MerchantOut.model_validate(m, from_attributes=True) for m in rows]
    except Exception:
        return []


@router.get("/{merchant_id}/products", response_model=list[ProductOut])
async def list_merchant_products(merchant_id: UUID, session: AsyncSession = Depends(get_session)):
    stmt = select(Product).where(Product.merchant_id == merchant_id, Product.is_available.is_(True))
    rows = (await session.execute(stmt)).scalars().all()
    return [ProductOut.model_validate(p, from_attributes=True) for p in rows]


# ─────────────────────────────────────────────────────────────────────────────
# MERCHANT OWNER OPERATIONS (Modulo 7)
# ─────────────────────────────────────────────────────────────────────────────

@router.post("/register", response_model=MerchantOut, status_code=201)
async def register_merchant(
    data: MerchantRegisterIn,
    user: CurrentUser = Depends(get_current_user),
    session: AsyncSession = Depends(get_session),
):
    # Verificar si el usuario ya tiene un comercio registrado
    existing = (await session.execute(select(Merchant).where(Merchant.owner_id == user.id))).scalar_one_or_none()
    if existing:
        raise HTTPException(status.HTTP_409_CONFLICT, "Ya tienes un comercio registrado con esta cuenta")

    # Guardar ubicación espacial con PostGIS ST_SetSRID
    loc_point = func.ST_SetSRID(func.ST_MakePoint(data.location.lng, data.location.lat), 4326)

    merchant = Merchant(
        owner_id=user.id,
        name=data.name,
        category=data.category,
        description=data.description,
        address_note=data.address_note,
        location=loc_point,
        phone=data.phone,
        whatsapp=data.whatsapp,
        opening_hours=data.opening_hours,
        logo_url=data.logo_url,
        cover_image_url=data.cover_image_url,
        approval_status=MerchantStatus.approved,  # Directamente aprobado para agilidad operativa
        is_open=True,
        is_active=True,
    )
    session.add(merchant)

    # Actualizar rol del usuario si era customer
    db_user = await session.get(User, user.id)
    if db_user and db_user.role.value == "customer":
        from app.models.enums import UserRole
        db_user.role = UserRole.merchant_owner

    await session.commit()
    return MerchantOut.model_validate(merchant, from_attributes=True)


@router.get("/my-store/profile", response_model=MerchantOut)
async def get_my_store_profile(
    user: CurrentUser = Depends(require_role("merchant_owner", "merchant_employee", "admin")),
    session: AsyncSession = Depends(get_session),
):
    stmt = select(Merchant).where(Merchant.owner_id == user.id)
    store = (await session.execute(stmt)).scalar_one_or_none()
    if store is None:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "No tienes un comercio asociado")
    return MerchantOut.model_validate(store, from_attributes=True)


@router.put("/my-store/profile", response_model=MerchantOut)
async def update_my_store_profile(
    data: MerchantUpdateIn,
    user: CurrentUser = Depends(require_role("merchant_owner", "merchant_employee")),
    session: AsyncSession = Depends(get_session),
):
    stmt = select(Merchant).where(Merchant.owner_id == user.id)
    store = (await session.execute(stmt)).scalar_one_or_none()
    if store is None:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "No tienes un comercio asociado")

    if data.name is not None:
        store.name = data.name
    if data.description is not None:
        store.description = data.description
    if data.address_note is not None:
        store.address_note = data.address_note
    if data.phone is not None:
        store.phone = data.phone
    if data.whatsapp is not None:
        store.whatsapp = data.whatsapp
    if data.opening_hours is not None:
        store.opening_hours = data.opening_hours
    if data.logo_url is not None:
        store.logo_url = data.logo_url
    if data.cover_image_url is not None:
        store.cover_image_url = data.cover_image_url
    if data.is_open is not None:
        store.is_open = data.is_open

    await session.commit()
    return MerchantOut.model_validate(store, from_attributes=True)


@router.get("/my-store/products", response_model=list[ProductOut])
async def get_my_store_products(
    user: CurrentUser = Depends(require_role("merchant_owner", "merchant_employee")),
    session: AsyncSession = Depends(get_session),
):
    stmt = select(Merchant.id).where(Merchant.owner_id == user.id)
    merchant_id = (await session.execute(stmt)).scalar_one_or_none()
    if not merchant_id:
        return []

    products_stmt = select(Product).where(Product.merchant_id == merchant_id).order_by(Product.name)
    rows = (await session.execute(products_stmt)).scalars().all()
    return [ProductOut.model_validate(p, from_attributes=True) for p in rows]


@router.post("/my-store/products", response_model=ProductOut, status_code=201)
async def add_product(
    data: ProductIn,
    user: CurrentUser = Depends(require_role("merchant_owner")),
    session: AsyncSession = Depends(get_session),
):
    stmt = select(Merchant).where(Merchant.owner_id == user.id)
    store = (await session.execute(stmt)).scalar_one_or_none()
    if store is None:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "No tienes un comercio registrado")

    prod = Product(
        merchant_id=store.id,
        name=data.name,
        description=data.description,
        price_cop=data.price_cop,
        image_url=data.image_url,
        is_available=data.is_available,
        stock=data.stock,
    )
    session.add(prod)
    await session.commit()
    return ProductOut.model_validate(prod, from_attributes=True)


@router.put("/my-store/products/{product_id}", response_model=ProductOut)
async def update_product(
    product_id: UUID,
    data: ProductIn,
    user: CurrentUser = Depends(require_role("merchant_owner")),
    session: AsyncSession = Depends(get_session),
):
    stmt = select(Merchant.id).where(Merchant.owner_id == user.id)
    merchant_id = (await session.execute(stmt)).scalar_one_or_none()
    if not merchant_id:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "No tienes un comercio registrado")

    prod = await session.get(Product, product_id)
    if prod is None or prod.merchant_id != merchant_id:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "Producto no encontrado o no pertenece a tu comercio")

    prod.name = data.name
    prod.description = data.description
    prod.price_cop = data.price_cop
    prod.image_url = data.image_url
    prod.is_available = data.is_available
    prod.stock = data.stock

    await session.commit()
    return ProductOut.model_validate(prod, from_attributes=True)


@router.delete("/my-store/products/{product_id}", status_code=200)
async def delete_product(
    product_id: UUID,
    user: CurrentUser = Depends(require_role("merchant_owner")),
    session: AsyncSession = Depends(get_session),
):
    stmt = select(Merchant.id).where(Merchant.owner_id == user.id)
    merchant_id = (await session.execute(stmt)).scalar_one_or_none()
    if not merchant_id:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "No tienes un comercio registrado")

    prod = await session.get(Product, product_id)
    if prod is None or prod.merchant_id != merchant_id:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "Producto no encontrado o no pertenece a tu comercio")

    await session.delete(prod)
    await session.commit()
    return {"deleted": True, "product_id": str(product_id)}


@router.get("/my-store/orders")
async def get_my_store_orders(
    user: CurrentUser = Depends(require_role("merchant_owner", "merchant_employee")),
    session: AsyncSession = Depends(get_session),
):
    stmt = select(Merchant.id).where(Merchant.owner_id == user.id)
    merchant_id = (await session.execute(stmt)).scalar_one_or_none()
    if not merchant_id:
        return []

    orders_stmt = (
        select(Order)
        .where(Order.merchant_id == merchant_id)
        .order_by(desc(Order.created_at))
        .limit(50)
    )
    rows = (await session.execute(orders_stmt)).scalars().all()

    result = []
    for o in rows:
        result.append({
            "id": str(o.id),
            "status": o.status.value,
            "items": o.items,
            "subtotal_cop": o.subtotal_cop,
            "delivery_fee_cop": o.delivery_fee_cop,
            "total_cop": o.total_cop,
            "payment_method": o.payment_method.value,
            "delivery_address_note": o.delivery_address_note,
            "prep_minutes": o.prep_minutes,
            "courier_id": str(o.courier_id) if o.courier_id else None,
            "created_at": o.created_at.isoformat() if o.created_at else None,
        })
    return result


@router.post("/orders/{order_id}/accept")
async def merchant_accept_order(
    order_id: UUID,
    data: OrderAcceptIn,
    user: CurrentUser = Depends(require_role("merchant_owner", "merchant_employee")),
    session: AsyncSession = Depends(get_session),
):
    order = await session.get(Order, order_id)
    if not order:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "Pedido no encontrado")

    store = await session.get(Merchant, order.merchant_id)
    if not store or store.owner_id != user.id:
        raise HTTPException(status.HTTP_403_FORBIDDEN, "No tienes permiso sobre este pedido")

    now_iso = datetime.now(timezone.utc).isoformat()
    order.status = OrderStatus.preparing
    order.prep_minutes = data.prep_minutes
    history = list(order.status_history or [])
    history.append({"status": "accepted", "timestamp": now_iso, "actor": "merchant"})
    history.append({"status": "preparing", "timestamp": now_iso, "actor": "merchant", "prep_minutes": data.prep_minutes})
    order.status_history = history

    await session.commit()
    return {"status": order.status.value, "prep_minutes": order.prep_minutes}


@router.post("/orders/{order_id}/reject")
async def merchant_reject_order(
    order_id: UUID,
    data: OrderRejectIn,
    user: CurrentUser = Depends(require_role("merchant_owner", "merchant_employee")),
    session: AsyncSession = Depends(get_session),
):
    order = await session.get(Order, order_id)
    if not order:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "Pedido no encontrado")

    store = await session.get(Merchant, order.merchant_id)
    if not store or store.owner_id != user.id:
        raise HTTPException(status.HTTP_403_FORBIDDEN, "No tienes permiso sobre este pedido")

    now_iso = datetime.now(timezone.utc).isoformat()
    order.status = OrderStatus.rejected
    order.rejection_reason = data.reason
    history = list(order.status_history or [])
    history.append({"status": "rejected", "timestamp": now_iso, "actor": "merchant", "reason": data.reason})
    order.status_history = history

    await session.commit()
    return {"status": "rejected", "reason": data.reason}


@router.post("/orders/{order_id}/ready")
async def merchant_ready_order(
    order_id: UUID,
    user: CurrentUser = Depends(require_role("merchant_owner", "merchant_employee")),
    session: AsyncSession = Depends(get_session),
):
    order = await session.get(Order, order_id)
    if not order:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "Pedido no encontrado")

    store = await session.get(Merchant, order.merchant_id)
    if not store or store.owner_id != user.id:
        raise HTTPException(status.HTTP_403_FORBIDDEN, "No tienes permiso sobre este pedido")

    now_iso = datetime.now(timezone.utc).isoformat()
    order.status = OrderStatus.ready
    history = list(order.status_history or [])
    history.append({"status": "ready", "timestamp": now_iso, "actor": "merchant"})
    order.status_history = history

    await session.commit()
    return {"status": "ready"}


# ─────────────────────────────────────────────────────────────────────────────
# SOLICITUD DE DOMICILIARIOS CERCANOS (Modulo 8 - Despacho Geoespacial PostGIS)
# ─────────────────────────────────────────────────────────────────────────────

@router.post("/orders/{order_id}/request-courier")
async def request_nearby_courier(
    order_id: UUID,
    user: CurrentUser = Depends(require_role("merchant_owner", "merchant_employee")),
    session: AsyncSession = Depends(get_session),
):
    order = await session.get(Order, order_id)
    if not order:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "Pedido no encontrado")

    store = await session.get(Merchant, order.merchant_id)
    if not store or store.owner_id != user.id:
        raise HTTPException(status.HTTP_403_FORBIDDEN, "No tienes permiso sobre este pedido")

    if order.status not in (OrderStatus.preparing, OrderStatus.ready, OrderStatus.accepted):
        raise HTTPException(status.HTTP_400_BAD_REQUEST, f"No se puede solicitar domiciliario en estado {order.status.value}")

    # Búsqueda de domiciliarios en línea y verificados ordenados por proximidad
    # Usando ST_Distance con el punto del comercio
    stmt = (
        select(DriverProfile)
        .where(
            DriverProfile.is_online.is_(True),
            DriverProfile.is_verified.is_(True),
        )
        .order_by(DriverProfile.rating_avg.desc())
        .limit(1)
    )
    courier = (await session.execute(stmt)).scalar_one_or_none()

    now_iso = datetime.now(timezone.utc).isoformat()
    history = list(order.status_history or [])

    if not courier:
        # Registrar intento de búsqueda fallido por disponibilidad
        history.append({"status": "courier_search_failed", "timestamp": now_iso, "reason": "Sin conductores cercanos"})
        order.status_history = history
        await session.commit()
        return {
            "assigned": False,
            "message": "En este momento no hay domiciliarios cercanos disponibles en Barbosa. Intenta de nuevo en unos minutos.",
            "code": "NO_COURIERS_NEARBY",
        }

    # Asignar domiciliario al pedido y generar PIN de seguridad para entrega
    if not order.delivery_pin:
        order.delivery_pin = f"{random.randint(1000, 9999)}"

    order.courier_id = courier.user_id
    order.status = OrderStatus.courier_assigned
    history.append({
        "status": "courier_assigned",
        "timestamp": now_iso,
        "courier_id": str(courier.user_id),
        "plate": courier.vehicle_plate,
    })
    order.status_history = history

    courier_user = await session.get(User, courier.user_id)
    await session.commit()

    return {
        "assigned": True,
        "order_id": str(order.id),
        "status": order.status.value,
        "courier": {
            "name": courier_user.full_name if courier_user else "Domiciliario BarboYa",
            "vehicle_type": courier.vehicle_type.value,
            "plate": courier.vehicle_plate,
            "phone": courier_user.phone if courier_user else "",
            "rating": float(courier.rating_avg),
        },
    }


# ─────────────────────────────────────────────────────────────────────────────
# SEGUIMIENTO COMPLETO DE DOMICILIOS DEL CLIENTE (Modulo 3)
# ─────────────────────────────────────────────────────────────────────────────

@orders_router.post("", status_code=201)
async def create_order(
    data: OrderCreateIn,
    user: CurrentUser = Depends(require_role("customer")),
    session: AsyncSession = Depends(get_session),
):
    merchant = await session.get(Merchant, data.merchant_id)
    if merchant is None or not merchant.is_active or not merchant.is_open:
        raise HTTPException(400, "Comercio no disponible")

    subtotal = 0
    order_items = []
    for item in data.items:
        prod = await session.get(Product, item.product_id)
        if prod is None or prod.merchant_id != merchant.id or not prod.is_available:
            raise HTTPException(400, f"Producto {item.product_id} no disponible")
        line_total = prod.price_cop * item.qty
        subtotal += line_total
        order_items.append({
            "product_id": str(prod.id),
            "name": prod.name,
            "qty": item.qty,
            "unit_price_cop": prod.price_cop,
            "line_total_cop": line_total,
        })

    delivery_fee = 3_000  # Tarifa base de domicilio urbano Barbosa
    commission = int(subtotal * float(merchant.commission_pct) / 100)
    total = subtotal + delivery_fee
    pin = f"{random.randint(1000, 9999)}"
    now_iso = datetime.now(timezone.utc).isoformat()

    order = Order(
        customer_id=user.id,
        merchant_id=merchant.id,
        status=OrderStatus.placed,
        status_history=[{"status": "placed", "timestamp": now_iso, "actor": "customer"}],
        delivery_pin=pin,
        items=order_items,
        subtotal_cop=subtotal,
        delivery_fee_cop=delivery_fee,
        commission_cop=commission,
        total_cop=total,
        payment_method=data.payment_method,
        delivery_address_note=data.delivery_address_note,
        prep_minutes=25,
    )
    session.add(order)
    await session.commit()
    return {
        "order_id": str(order.id),
        "status": order.status.value,
        "total_cop": total,
        "delivery_pin": pin,
    }


@orders_router.get("/{order_id}/tracking")
async def get_order_tracking(
    order_id: UUID,
    user: CurrentUser = Depends(get_current_user),
    session: AsyncSession = Depends(get_session),
):
    order = await session.get(Order, order_id)
    if not order:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "Pedido no encontrado")

    # Autorización: cliente dueño, dueño de la tienda, o conductor asignado
    store = await session.get(Merchant, order.merchant_id)
    is_customer = order.customer_id == user.id
    is_merchant = store and store.owner_id == user.id
    is_courier = order.courier_id == user.id
    is_admin = user.role in ("admin", "superadmin")

    if not (is_customer or is_merchant or is_courier or is_admin):
        raise HTTPException(status.HTTP_403_FORBIDDEN, "No tienes permiso para ver este pedido")

    courier_info = None
    if order.courier_id:
        c_profile = await session.get(DriverProfile, order.courier_id)
        c_user = await session.get(User, order.courier_id)
        if c_profile and c_user:
            courier_info = {
                "name": c_user.full_name,
                "phone": c_user.phone,
                "avatar_url": c_user.avatar_url,
                "vehicle_type": c_profile.vehicle_type.value,
                "plate": c_profile.vehicle_plate,
                "vehicle_description": c_profile.vehicle_description,
                "rating": float(c_profile.rating_avg),
            }

    store_info = {
        "id": str(store.id) if store else None,
        "name": store.name if store else "Restaurante",
        "category": store.category.value if store else "restaurant",
        "phone": store.phone if store else "",
        "address": store.address_note if store else "",
        "logo_url": store.logo_url if store else None,
    }

    # El PIN solo se expone al cliente para que se lo dicte al domiciliario al recibir
    visible_pin = order.delivery_pin if is_customer else None

    # Cálculo dinámico de etapas
    all_stages = [
        {"key": "placed", "label": "Pedido realizado"},
        {"key": "accepted", "label": "Aceptado por comercio"},
        {"key": "preparing", "label": "En preparación"},
        {"key": "ready", "label": "Listo para recoger"},
        {"key": "courier_assigned", "label": "Domiciliario asignado"},
        {"key": "picked_up", "label": "Pedido recogido"},
        {"key": "in_transit", "label": "En camino al destino"},
        {"key": "delivered", "label": "Entregado"},
    ]

    return {
        "order_id": str(order.id),
        "status": order.status.value,
        "status_history": order.status_history or [],
        "store": store_info,
        "items": order.items,
        "subtotal_cop": order.subtotal_cop,
        "delivery_fee_cop": order.delivery_fee_cop,
        "total_cop": order.total_cop,
        "payment_method": order.payment_method.value,
        "delivery_address_note": order.delivery_address_note,
        "courier": courier_info,
        "delivery_pin": visible_pin,
        "prep_minutes": order.prep_minutes,
        "created_at": order.created_at.isoformat() if order.created_at else None,
        "stages": all_stages,
    }


@orders_router.post("/{order_id}/verify-delivery")
async def verify_order_delivery(
    order_id: UUID,
    data: DeliveryPinVerifyIn,
    user: CurrentUser = Depends(require_role("driver", "courier")),
    session: AsyncSession = Depends(get_session),
):
    order = await session.get(Order, order_id)
    if not order:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "Pedido no encontrado")

    if order.courier_id != user.id:
        raise HTTPException(status.HTTP_403_FORBIDDEN, "No estás asignado como domiciliario de este pedido")

    if order.delivery_pin != data.pin:
        raise HTTPException(status.HTTP_400_BAD_REQUEST, "PIN de entrega incorrecto. Solicítalo al cliente.")

    now_iso = datetime.now(timezone.utc).isoformat()
    order.status = OrderStatus.delivered
    history = list(order.status_history or [])
    history.append({"status": "delivered", "timestamp": now_iso, "actor": "courier", "pin_verified": True})
    order.status_history = history

    await session.commit()
    return {"status": "delivered", "message": "¡Entrega confirmada exitosamente mediante PIN de seguridad!"}


@orders_router.get("/my-orders")
async def get_customer_orders(
    user: CurrentUser = Depends(require_role("customer")),
    session: AsyncSession = Depends(get_session),
):
    try:
        stmt = select(Order).where(Order.customer_id == user.id).order_by(desc(Order.created_at)).limit(20)
        rows = (await session.execute(stmt)).scalars().all()
        result = []
        for o in rows:
            store = await session.get(Merchant, o.merchant_id)
            result.append({
                "order_id": str(o.id),
                "store_name": store.name if store else "Restaurante",
                "status": o.status.value,
                "total_cop": o.total_cop,
                "items_count": len(o.items) if isinstance(o.items, list) else 1,
                "created_at": o.created_at.isoformat() if o.created_at else None,
            })
        return result
    except Exception:
        return []
