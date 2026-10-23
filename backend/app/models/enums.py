import enum


class UserRole(str, enum.Enum):
    customer = "customer"
    driver = "driver"
    courier = "courier"
    merchant_owner = "merchant_owner"
    merchant_employee = "merchant_employee"
    support = "support"
    admin = "admin"
    superadmin = "superadmin"


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


class MerchantCategory(str, enum.Enum):
    restaurant = "restaurant"
    pharmacy = "pharmacy"
    grocery = "grocery"
    bakery = "bakery"
    hardware = "hardware"
    other = "other"


class Gender(str, enum.Enum):
    female = "female"
    male = "male"
    other = "other"


class VerificationStatus(str, enum.Enum):
    pending = "pending"
    under_review = "under_review"
    verified = "verified"
    needs_update = "needs_update"
    rejected = "rejected"
    suspended = "suspended"


class MerchantStatus(str, enum.Enum):
    pending_approval = "pending_approval"
    approved = "approved"
    rejected = "rejected"
    suspended = "suspended"


class OrderStatus(str, enum.Enum):
    placed = "placed"
    pending_confirmation = "pending_confirmation"
    accepted = "accepted"
    rejected = "rejected"
    preparing = "preparing"
    ready = "ready"
    courier_search = "courier_search"
    courier_assigned = "courier_assigned"
    courier_heading_to_merchant = "courier_heading_to_merchant"
    picked_up = "picked_up"
    in_transit = "in_transit"
    delivered = "delivered"
    cancelled = "cancelled"


class SecurityEventType(str, enum.Enum):
    sos = "sos"
    stopped_too_long = "stopped_too_long"
    are_you_ok_sent = "are_you_ok_sent"
    are_you_ok_ack = "are_you_ok_ack"
    face_check_requested = "face_check_requested"
    face_check_passed = "face_check_passed"
    face_check_failed = "face_check_failed"
    share_link_created = "share_link_created"
