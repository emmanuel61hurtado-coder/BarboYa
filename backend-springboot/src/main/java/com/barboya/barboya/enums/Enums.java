package com.barboya.barboya.enums;

public final class Enums {
    private Enums() {}

    public enum UserRole { customer, driver, courier, merchant_owner, merchant_employee, support, admin, superadmin }
    public enum VehicleType { moto, car, bicycle }
    public enum BillingScheme { subscription, per_trip_fee }
    public enum SubscriptionPlan { daily, weekly, monthly }
    public enum SubscriptionStatus { pending, active, expired, cancelled }
    public enum WalletTxType { topup, trip_fee, subscription_purchase, refund, adjustment }
    public enum WalletTxStatus { pending, approved, rejected }
    public enum PaymentMethod { cash, nequi, daviplata, physical_point, crypto }
    public enum BlockchainTxStatus { pending, confirming, confirmed, failed }
    public enum ServiceType { passenger, delivery, errand }
    public enum TripStatus { requested, negotiating, accepted, arrived, in_progress, completed, cancelled_by_customer, cancelled_by_driver, expired }
    public enum MerchantCategory { restaurant, pharmacy, grocery, bakery, hardware, other }
    public enum Gender { female, male, other }
    public enum VerificationStatus { pending, under_review, verified, needs_update, rejected, suspended }
    public enum MerchantStatus { pending_approval, approved, rejected, suspended }
    public enum OrderStatus { placed, pending_confirmation, accepted, rejected, preparing, ready, courier_search, courier_assigned, courier_heading_to_merchant, picked_up, in_transit, delivered, cancelled }
    public enum SecurityEventType { sos, stopped_too_long, are_you_ok_sent, are_you_ok_ack, face_check_requested, face_check_passed, face_check_failed, share_link_created }
}