from app.core.database import get_db
from app.models.user import UserRole
from fastapi import APIRouter, Depends, status
from pydantic import BaseModel, EmailStr
from sqlalchemy.ext.asyncio import AsyncSession

router = APIRouter(prefix="/auth", tags=["auth"])

class UserRegister(BaseModel):
    email: EmailStr
    password: str
    full_name: str
    phone: str | None = None
    role: UserRole = UserRole.CLIENT

class TokenResponse(BaseModel):
    access_token: str
    refresh_token: str
    token_type: str = "bearer"

# ruff: noqa: B008
@router.post("/register", status_code=status.HTTP_201_CREATED)
async def register(user_in: UserRegister, db: AsyncSession = Depends(get_db)):
    _ = (user_in, db)
    return {"msg": "Usuario registrado exitosamente (Mock)"}

@router.post("/login", response_model=TokenResponse)
async def login():
    return {
        "access_token": "mock_access_token_jwt",
        "refresh_token": "mock_refresh_token_jwt",
        "token_type": "bearer"
    }
