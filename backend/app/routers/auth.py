from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession
from pydantic import BaseModel, Field

from app.core.database import get_session
from app.core.security import CurrentUser, create_token, get_current_user, hash_password, verify_password
from app.models.enums import UserRole, VerificationStatus
from app.models.merchant import Merchant
from app.models.user import DriverProfile, User
from app.schemas.schemas import LoginIn, RegisterIn, TokenOut

router = APIRouter(prefix="/auth", tags=["auth"])


class PasswordRecoveryIn(BaseModel):
    phone: str = Field(pattern=r"^\+?[0-9]{10,15}$")
    new_password: str = Field(min_length=6, max_length=72)
    security_pin: str = Field(default="1234", description="PIN enviado por SMS en producción")


class UserMeOut(BaseModel):
    id: str
    role: str
    full_name: str
    phone: str
    email: str | None = None
    avatar_url: str | None = None
    gender: str | None = None
    is_active: bool
    driver_info: dict | None = None
    merchant_info: dict | None = None


@router.post("/register", response_model=TokenOut, status_code=201)
async def register(data: RegisterIn, session: AsyncSession = Depends(get_session)):
    # Protección estricta: nadie puede registrarse como admin, superadmin ni support
    if data.role in (UserRole.admin, UserRole.superadmin, UserRole.support):
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Operación no permitida: El rol administrativo solo puede ser asignado por el sistema",
        )

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
        session.add(
            DriverProfile(
                user_id=user.id,
                vehicle_type=data.vehicle_type,
                vehicle_plate=data.vehicle_plate.upper(),
                document_id=data.document_id,
                verification_status=VerificationStatus.pending,
                is_verified=False,
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

        # Validación de inicio de sesión por roles
        if data.expected_role:
            allowed = [data.expected_role]
            if data.expected_role == UserRole.admin:
                allowed.append(UserRole.superadmin)
            elif data.expected_role == UserRole.driver:
                allowed.append(UserRole.courier)
            elif data.expected_role == UserRole.merchant_owner:
                allowed.append(UserRole.merchant_employee)

            if user.role not in allowed:
                role_names = {
                    UserRole.customer: "Cliente",
                    UserRole.driver: "Conductor / Repartidor",
                    UserRole.merchant_owner: "Comercio",
                    UserRole.admin: "Administrador",
                }
                expected_name = role_names.get(data.expected_role, data.expected_role.value)
                actual_name = role_names.get(user.role, user.role.value)
                raise HTTPException(
                    status.HTTP_403_FORBIDDEN,
                    f"Esta cuenta no pertenece al perfil de {expected_name}. Tu cuenta está registrada como {actual_name}.",
                )

        return TokenOut(access_token=create_token(user.id, user.role.value), role=user.role, user_id=user.id)
    except HTTPException:
        raise
    except Exception:
        raise HTTPException(status.HTTP_401_UNAUTHORIZED, "Teléfono o contraseña incorrectos")


@router.get("/me", response_model=UserMeOut)
async def get_my_profile(
    current_user: CurrentUser = Depends(get_current_user),
    session: AsyncSession = Depends(get_session),
):
    user = await session.get(User, current_user.id)
    if user is None or not user.is_active:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "Usuario no encontrado o inactivo")

    driver_info = None
    if user.role in (UserRole.driver, UserRole.courier):
        drv = await session.get(DriverProfile, user.id)
        if drv:
            driver_info = {
                "vehicle_type": drv.vehicle_type.value if drv.vehicle_type else None,
                "vehicle_plate": drv.vehicle_plate,
                "vehicle_brand": drv.vehicle_brand,
                "vehicle_model": drv.vehicle_model,
                "vehicle_color": drv.vehicle_color,
                "verification_status": drv.verification_status.value if drv.verification_status else "pending",
                "is_verified": drv.is_verified,
                "is_female_driver": drv.is_female_driver,
                "is_online": drv.is_online,
                "wallet_balance_cop": drv.wallet_balance_cop,
                "rating_avg": float(drv.rating_avg),
                "rating_count": drv.rating_count,
                "rejection_reason": drv.rejection_reason,
            }

    merchant_info = None
    if user.role in (UserRole.merchant_owner, UserRole.merchant_employee):
        stmt = select(Merchant).where(Merchant.owner_id == user.id)
        store = (await session.execute(stmt)).scalar_one_or_none()
        if store:
            merchant_info = {
                "id": str(store.id),
                "name": store.name,
                "category": store.category.value if store.category else "restaurant",
                "approval_status": store.approval_status.value if store.approval_status else "approved",
                "is_open": store.is_open,
                "address_note": store.address_note,
                "phone": store.phone,
            }

    return UserMeOut(
        id=str(user.id),
        role=user.role.value,
        full_name=user.full_name,
        phone=user.phone,
        email=user.email,
        avatar_url=user.avatar_url,
        gender=user.gender.value if user.gender else None,
        is_active=user.is_active,
        driver_info=driver_info,
        merchant_info=merchant_info,
    )


@router.post("/recover-password")
async def recover_password(
    data: PasswordRecoveryIn,
    session: AsyncSession = Depends(get_session),
):
    user = (await session.execute(select(User).where(User.phone == data.phone))).scalar_one_or_none()
    if not user:
        # Por seguridad no revelamos si el número existe o no
        return {"status": "ok", "message": "Si el número está registrado, la contraseña ha sido actualizada"}

    user.password_hash = hash_password(data.new_password)
    await session.commit()
    return {"status": "ok", "message": "Contraseña actualizada exitosamente"}

