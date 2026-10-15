from pydantic import BaseModel, EmailStr
from app.models.user import UserRole
from uuid import UUID


class LoginRequest(BaseModel):
    email: EmailStr
    password: str


class RegisterRequest(BaseModel):
    email: EmailStr
    password: str
    nombre: str
    telefono: str
    rol: UserRole


class GoogleLoginRequest(BaseModel):
    id_token: str


class OTPRequest(BaseModel):
    telefono: str


class OTPVerifyRequest(BaseModel):
    telefono: str
    codigo: str


class RefreshTokenRequest(BaseModel):
    refresh_token: str


class TokenResponse(BaseModel):
    access_token: str
    refresh_token: str
    token_type: str = "bearer"


Token = TokenResponse


class TokenData(BaseModel):
    user_id: UUID | None = None


class ForgotPasswordRequest(BaseModel):
    email: EmailStr


class ResetPasswordRequest(BaseModel):
    token: str
    new_password: str
