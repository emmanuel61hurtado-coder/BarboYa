from fastapi import APIRouter
from pydantic import BaseModel, EmailStr

router = APIRouter(prefix="/users", tags=["users"])

class UserProfile(BaseModel):
    id: str
    email: EmailStr
    full_name: str
    phone: str | None = None
    role: str
    is_active: bool

@router.get("/me", response_model=UserProfile)
async def get_my_profile():
    return {
        "id": "00000000-0000-0000-0000-000000000001",
        "email": "usuario@barboya.com",
        "full_name": "Usuario Demostración",
        "phone": "+573001234567",
        "role": "client",
        "is_active": True
    }

@router.put("/me", response_model=UserProfile)
async def update_my_profile(profile_in: UserProfile):
    return profile_in
