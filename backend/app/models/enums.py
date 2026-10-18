import enum


class UserRole(str, enum.Enum):
    customer = "customer"
    driver = "driver"
    merchant_owner = "merchant_owner"
    admin = "admin"


class VehicleType(str, enum.Enum):
    moto = "moto"
    car = "car"
    bicycle = "bicycle"


class BillingScheme(str, enum.Enum):
    subscription = "subscription"
    per_trip_fee = "per_trip_fee"


class SubscriptionPlan(str, enum.Enum):
    daily = "daily"
    weekly = "weekly"
    monthly = "monthly"


class SubscriptionStatus(str, enum.Enum):
    pending = "pending"
    active = "active"
    expired = "expired"
    cancelled = "cancelled"


class WalletTxType(str, enum.Enum):
    topup = "topup"
    trip_fee = "trip_fee"
    subscription_purchase = "subscription_purchase"
    refund = "refund"
    adjustment = "adjustment"


class WalletTxStatus(str, enum.Enum):
    pending = "pending"
    approved = "approved"
    rejected = "rejected"


class PaymentMethod(str, enum.Enum):
    cash = "cash"
    nequi = "nequi"
    daviplata = "daviplata"
    physical_point = "physical_point"


class ServiceType(str, enum.Enum):
    passenger = "passenger"
    delivery = "delivery"
    errand = "errand"


class TripStatus(str, enum.Enum):
    requested = "requested"
    negotiating = "negotiating"
    accepted = "accepted"
    arrived = "arrived"
    in_progress = "in_progress"
    completed = "completed"
    cancelled_by_customer = "cancelled_by_customer"
    cancelled_by_driver = "cancelled_by_driver"
    expired = "expired"
