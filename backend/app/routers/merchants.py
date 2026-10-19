from uuid import UUID
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.database import get_session
from app.core.security import CurrentUser, require_role
from app.models.merchant import Merchant, Order, Product
from app.models.enums import MerchantCategory, OrderStatus, PaymentMethod
from pydantic import BaseModel, Field, ConfigDict

router = APIRouter(prefix="/merchants", tags=["merchants"])


class ProductOut(BaseModel):
    model_config = ConfigDict(from_attributes=True)
    id: UUID
    name: str
    description: str | None
    price_cop: int
    image_url: str | None
    is_available: bool


class MerchantOut(BaseModel):
    model_config = ConfigDict(from_attributes=True)
    id: UUID
    name: str
    category: MerchantCategory
    description: str | None
    logo_url: str | None
    address_note: str
    phone: str | None
    whatsapp: str | None
    is_open: bool


class OrderItemIn(BaseModel):
    product_id: UUID
    qty: int = Field(gt=0, le=50)


class OrderCreateIn(BaseModel):
    merchant_id: UUID
    items: list[OrderItemIn]
    payment_method: PaymentMethod = PaymentMethod.cash
    delivery_address_note: str | None = Field(default=None, max_length=200)


@router.get("", response_model=list[MerchantOut])
async def list_merchants(category: MerchantCategory | None = None, session: AsyncSession = Depends(get_session)):
    try:
        stmt = select(Merchant).where(Merchant.is_active.is_(True))
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


orders_router = APIRouter(prefix="/orders", tags=["orders"])


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

    delivery_fee = 3_000  # Tarifa base de domicilio local
    commission = int(subtotal * float(merchant.commission_pct) / 100)
    total = subtotal + delivery_fee

    order = Order(
        customer_id=user.id,
        merchant_id=merchant.id,
        status=OrderStatus.placed,
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
    return {"order_id": str(order.id), "status": order.status.value, "total_cop": total}
