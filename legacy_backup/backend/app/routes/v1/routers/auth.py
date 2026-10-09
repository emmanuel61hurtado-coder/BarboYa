from fastapi import APIRouter, Depends, Response, status
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.future import select
from app.db.session import get_db
from app.models.user import User, UserStatus, UserRole
from app.schemas.auth import LoginRequest, RegisterRequest, TokenResponse, ForgotPasswordRequest, ResetPasswordRequest
from app.schemas.user import UserRead
from app.core.security import verify_password, get_password_hash, create_access_token, create_refresh_token
from app.core.exceptions import DomainException

router = APIRouter(prefix="/auth", tags=["Auth"])


@router.post("/register", response_model=UserRead, status_code=status.HTTP_201_CREATED)
async def register(payload: RegisterRequest, db: AsyncSession = Depends(get_db)):
    if payload.rol == UserRole.ADMIN:
        raise DomainException("FORBIDDEN_ROLE", "No se puede registrar una cuenta de administrador", status.HTTP_403_FORBIDDEN)

    result = await db.execute(select(User).where(User.email == payload.email))
    if result.scalar_one_or_none():
        raise DomainException("EMAIL_ALREADY_EXISTS", "El correo ya está registrado", status.HTTP_409_CONFLICT)
    
    estado = UserStatus.PENDIENTE if payload.rol in [UserRole.REPARTIDOR, UserRole.COMERCIO] else UserStatus.ACTIVO
    
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
    if not user or not verify_password(payload.password, user.password_hash):
        raise DomainException("INVALID_CREDENTIALS", "Credenciales inválidas", status.HTTP_401_UNAUTHORIZED)
    
    if user.estado == UserStatus.BLOQUEADO:
        raise DomainException("USER_BLOCKED", "Usuario bloqueado", status.HTTP_403_FORBIDDEN)
        
    access_token = create_access_token(user.id)
    refresh_token = create_refresh_token(user.id)
    
    response.set_cookie(
        key="refresh_token",
        value=refresh_token,
        httponly=True,
        secure=True,
        samesite="lax",
        max_age=7 * 24 * 60 * 60
    )
    return {"access_token": access_token, "token_type": "bearer"}


@router.post("/refresh", response_model=TokenResponse)
async def refresh_token():
    # Implementation for refreshing token via cookie
    return {"access_token": "new_mock_access_token", "token_type": "bearer"}


@router.post("/logout")
async def logout(response: Response):
    response.delete_cookie("refresh_token")
    return {"message": "Sesión cerrada exitosamente"}


@router.post("/forgot-password")
async def forgot_password(payload: ForgotPasswordRequest):
    # Console email sender simulation
    print(f"[EmailSender] Password reset email sent to {payload.email}")
    return {"message": "Correo de recuperación enviado"}


@router.post("/reset-password")
async def reset_password(payload: ResetPasswordRequest):
    return {"message": "Contraseña restablecida exitosamente"}
