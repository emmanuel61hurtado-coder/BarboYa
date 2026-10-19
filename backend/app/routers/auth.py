from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.database import get_session
from app.core.security import create_token, hash_password, verify_password
from app.models.enums import UserRole
from app.models.user import DriverProfile, User
from app.schemas.schemas import LoginIn, RegisterIn, TokenOut

router = APIRouter(prefix="/auth", tags=["auth"])


@router.post("/register", response_model=TokenOut, status_code=201)
async def register(data: RegisterIn, session: AsyncSession = Depends(get_session)):
    if data.role in (
        UserRole.admin,
        UserRole.superadmin,
        UserRole.support,
        UserRole.merchant_owner,
        UserRole.merchant_employee,
    ):
        raise HTTPException(status.HTTP_403_FORBIDDEN, "Ese rol lo asigna un administrador")
    exists = (await session.execute(select(User.id).where(User.phone == data.phone))).first()
    if exists:
        raise HTTPException(status.HTTP_409_CONFLICT, "Ese teléfono ya está registrado")

    user = User(
        full_name=data.full_name,
        phone=data.phone,
        password_hash=hash_password(data.password),
        role=data.role,
    )
    session.add(user)
    await session.flush()

    if data.role == UserRole.driver:
        if not (data.vehicle_type and data.vehicle_plate and data.document_id):
            raise HTTPException(422, "Un conductor requiere vehículo, placa y documento")
        # is_verified=False hasta que el admin valide documentos y selfie base
        session.add(
            DriverProfile(
                user_id=user.id,
                vehicle_type=data.vehicle_type,
                vehicle_plate=data.vehicle_plate.upper(),
                document_id=data.document_id,
            )
        )
    await session.commit()
    return TokenOut(access_token=create_token(user.id, user.role.value), role=user.role, user_id=user.id)


@router.post("/login", response_model=TokenOut)
async def login(data: LoginIn, session: AsyncSession = Depends(get_session)):
    try:
        user = (await session.execute(select(User).where(User.phone == data.phone))).scalar_one_or_none()
        if not user or not verify_password(data.password, user.password_hash) or not user.is_active:
            raise HTTPException(status.HTTP_401_UNAUTHORIZED, "Teléfono o contraseña incorrectos")
        return TokenOut(access_token=create_token(user.id, user.role.value), role=user.role, user_id=user.id)
    except HTTPException:
        raise
    except Exception:
        raise HTTPException(status.HTTP_401_UNAUTHORIZED, "Teléfono o contraseña incorrectos")
