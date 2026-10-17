from fastapi import APIRouter, status
from pydantic import BaseModel

router = APIRouter(prefix="/transport", tags=["transport"])

class RideRequest(BaseModel):
    pickup_lat: float
    pickup_lng: float
    dropoff_lat: float
    dropoff_lng: float
    service_type: str = "moto" # moto, car, delivery

class RideQuote(BaseModel):
    estimated_fare: float
    estimated_time_mins: int
    distance_km: float

@router.post("/quote", response_model=RideQuote)
async def quote_ride(ride: RideRequest):
    return {
        "estimated_fare": 12500.0,
        "estimated_time_mins": 14,
        "distance_km": 4.2
    }

@router.post("/request", status_code=status.HTTP_201_CREATED)
async def request_ride(ride: RideRequest):
    return {
        "ride_id": "ride-998877",
        "status": "searching_driver",
        "message": "Buscando conductor cercano..."
    }
