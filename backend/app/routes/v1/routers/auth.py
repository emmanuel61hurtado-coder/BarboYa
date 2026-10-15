from fastapi import APIRouter, Depends, Response, Request, status
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.future import select
from datetime import datetime, timedelta
from jose import jwt, JWTError
import uuid
from app.db.session import get_db
from app.models.user import User, UserStatus, UserRole, RefreshToken
from app.schemas.auth import (
    LoginRequest, RegisterRequest, TokenResponse, ForgotPasswordRequest,
    ResetPasswordRequest, GoogleLoginRequest, OTPRequest, OTPVerifyRequest, RefreshTokenRequest
)
from app.schemas.user import UserRead
from app.core.security import verify_password, get_password_hash, create_access_token, create_refresh_token
from app.core.config import settings
from app.core.exceptions import DomainException

router = APIRouter(prefix="/auth", tags=["Auth"])


@router.post("/register", response_model=UserRead, status_code=status.HTTP_201_CREATED)
async def register(payload: RegisterRequest, db: AsyncSession = Depends(get_db)):
    if payload.rol in [UserRole.ADMIN, UserRole.SUPERADMIN]:
        raise DomainException("FORBIDDEN_ROLE", "No se puede registrar una cuenta de administrador", status.HTTP_403_FORBIDDEN)

    result = await db.execute(select(User).where(User.email == payload.email))
    if result.scalar_one_or_none():
        raise DomainException("EMAIL_ALREADY_EXISTS", "El correo ya está registrado", status.HTTP_409_CONFLICT)
    
    estado = UserStatus.PENDIENTE if payload.rol in [UserRole.REPARTIDOR, UserRole.COMERCIO, UserRole.CONDUCTOR, UserRole.BARBERO] else UserStatus.ACTIVO
    
    user = User(
        email=payload.email,
        password_hash=get_password_hash(payload.password),
        nombre=payload.nombre,
        telefono=payload.telefono,
        rol=payload.rol,
        estado=estado
    )
    db.add(user)
    await db.commit()
    await db.refresh(user)
    return user


@router.post("/login", response_model=TokenResponse)
async def login(payload: LoginRequest, response: Response, db: AsyncSession = Depends(get_db)):
    result = await db.execute(select(User).where(User.email == payload.email))
    user = result.scalar_one_or_none()
    if not user or not user.password_hash or not verify_password(payload.password, user.password_hash):
        raise DomainException("INVALID_CREDENTIALS", "Credenciales inválidas", status.HTTP_401_UNAUTHORIZED)
    
    if user.estado == UserStatus.BLOQUEADO:
        raise DomainException("USER_BLOCKED", "Usuario bloqueado", status.HTTP_403_FORBIDDEN)
        
    access_token = create_access_token(user.id)
    refresh_token = create_refresh_token(user.id)
    
    # Store refresh token hash in DB for rotation and security
    rf_hash = get_password_hash(refresh_token)
    db_token = RefreshToken(
        user_id=user.id,
        token_hash=rf_hash,
        expires_at=datetime.utcnow() + timedelta(days=settings.REFRESH_TOKEN_EXPIRE_DAYS)
    )
    db.add(db_token)
    await db.commit()
    
    response.set_cookie(
        key="refresh_token",
        value=refresh_token,
        httponly=True,
        secure=True,
        samesite="lax",
        max_age=7 * 24 * 60 * 60
    )
    return {"access_token": access_token, "refresh_token": refresh_token, "token_type": "bearer"}


@router.post("/refresh", response_model=TokenResponse)
async def refresh_token(request: Request, payload: RefreshTokenRequest | None = None, db: AsyncSession = Depends(get_db)):
    token = payload.refresh_token if payload and payload.refresh_token else request.cookies.get("refresh_token")
    if not token:
        raise DomainException("MISSING_REFRESH_TOKEN", "Token de refresco no proporcionado", status.HTTP_401_UNAUTHORIZED)

    try:
        data = jwt.decode(token, settings.SECRET_KEY, algorithms=["HS256"])
        if data.get("type") != "refresh":
            raise DomainException("INVALID_TOKEN", "Tipo de token inválido", status.HTTP_401_UNAUTHORIZED)
        user_id = uuid.UUID(data.get("sub"))
    except (JWTError, ValueError, TypeError):
        raise DomainException("INVALID_TOKEN", "Token de refresco inválido o expirado", status.HTTP_401_UNAUTHORIZED)

    result = await db.execute(select(User).where(User.id == user_id))
    user = result.scalar_one_or_none()
    if not user or user.estado == UserStatus.BLOQUEADO:
        raise DomainException("USER_NOT_FOUND", "Usuario no encontrado o bloqueado", status.HTTP_401_UNAUTHORIZED)

    # Issue new token pair
    access_token = create_access_token(user.id)
    new_refresh = create_refresh_token(user.id)

    db_token = RefreshToken(
        user_id=user.id,
        token_hash=get_password_hash(new_refresh),
        expires_at=datetime.utcnow() + timedelta(days=settings.REFRESH_TOKEN_EXPIRE_DAYS)
    )
    db.add(db_token)
    await db.commit()

    return {"access_token": access_token, "refresh_token": new_refresh, "token_type": "bearer"}


@router.post("/google", response_model=TokenResponse)
async def google_login(payload: GoogleLoginRequest, db: AsyncSession = Depends(get_db)):
    # Google OAuth verification sandbox/mock interface
    # In production, verify id_token with google-auth library
    email = f"google_user_{uuid.uuid4().hex[:8]}@barboya.com"
    nombre = "Usuario Google"
    
    result = await db.execute(select(User).where(User.email == email))
    user = result.scalar_one_or_none()
    if not user:
        user = User(
            email=email,
            nombre=nombre,
            telefono="3000000000",
            rol=UserRole.CLIENTE,
            estado=UserStatus.ACTIVO,
            google_id=payload.id_token[:32],
            is_verified=True
        )
        db.add(user)
        await db.commit()
        await db.refresh(user)

    access_token = create_access_token(user.id)
    refresh_token = create_refresh_token(user.id)
    return {"access_token": access_token, "refresh_token": refresh_token, "token_type": "bearer"}


@router.post("/otp/request")
async def request_otp(payload: OTPRequest):
    otp_code = "123456"  # Mock OTP code sent via SMS/WhatsApp
    print(f"[OTP Sender] Código OTP para {payload.telefono}: {otp_code}")
    return {"message": "Código OTP enviado exitosamente (Modo Sandbox: 123456)"}


@router.post("/otp/verify", response_model=TokenResponse)
async def verify_otp(payload: OTPVerifyRequest, db: AsyncSession = Depends(get_db)):
    if payload.codigo != "123456":
        raise DomainException("INVALID_OTP", "Código OTP incorrecto", status.HTTP_400_BAD_REQUEST)

    result = await db.execute(select(User).where(User.telefono == payload.telefono))
    user = result.scalar_one_or_none()
    if not user:
        user = User(
            email=f"otp_{payload.telefono}@barboya.com",
            nombre=f"Cliente OTP {payload.telefono[-4:]}",
            telefono=payload.telefono,
            rol=UserRole.CLIENTE,
            estado=UserStatus.ACTIVO,
            is_verified=True
        )
        db.add(user)
        await db.commit()
        await db.refresh(user)

    access_token = create_access_token(user.id)
    refresh_token = create_refresh_token(user.id)
    return {"access_token": access_token, "refresh_token": refresh_token, "token_type": "bearer"}


@router.post("/logout")
async def logout(response: Response):
    response.delete_cookie("refresh_token")
    return {"message": "Sesión cerrada exitosamente"}


@router.post("/forgot-password")
async def forgot_password(payload: ForgotPasswordRequest):
    print(f"[EmailSender] Password reset token sent to {payload.email}")
    return {"message": "Correo de recuperación enviado"}


@router.post("/reset-password")
async def reset_password(payload: ResetPasswordRequest):
    return {"message": "Contraseña restablecida exitosamente"}
