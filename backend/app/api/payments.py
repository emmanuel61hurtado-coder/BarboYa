from fastapi import APIRouter, status
from pydantic import BaseModel

router = APIRouter(prefix="/payments", tags=["payments"])

class WalletBalance(BaseModel):
    balance: float
    currency: str = "COP"

class TopUpRequest(BaseModel):
    amount: float
    payment_method_id: str

@router.get("/wallet", response_model=WalletBalance)
async def get_wallet_balance():
    return {
        "balance": 85000.0,
        "currency": "COP"
    }

@router.post("/wallet/topup", status_code=status.HTTP_200_OK)
async def topup_wallet(topup: TopUpRequest):
    return {
        "status": "success",
        "new_balance": 85000.0 + topup.amount,
        "transaction_id": "tx-123456"
    }
