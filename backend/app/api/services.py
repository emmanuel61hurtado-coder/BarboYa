
from fastapi import APIRouter
from pydantic import BaseModel

router = APIRouter(prefix="/services", tags=["services"])

class ServiceItem(BaseModel):
    id: str
    name: str
    category: str
    price: float
    provider_name: str
    rating: float

@router.get("/", response_model=list[ServiceItem])
async def list_services(category: str | None = None):
    return [
        {
            "id": "srv-01",
            "name": "Plomería Residencial Express",
            "category": "Hogar",
            "price": 45000.0,
            "provider_name": "Carlos El Plomero",
            "rating": 4.8
        },
        {
            "id": "srv-02",
            "name": "Limpieza Profunda Apartamento",
            "category": "Limpieza",
            "price": 80000.0,
            "provider_name": "CleanPro S.A.S.",
            "rating": 4.9
        }
    ]
