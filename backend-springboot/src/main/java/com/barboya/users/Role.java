package com.barboya.users;

public enum Role {
    CUSTOMER,
    COURIER,
    MOTO_DRIVER,
    CAR_DRIVER,
    RESTAURANT,
    STORE,
    ADMIN,
    SUPERADMIN;

    /** Roles internos: nunca se obtienen por registro publico. */
    public boolean isStaff() {
        return this == ADMIN || this == SUPERADMIN;
    }

    /** Quien presta un servicio no opera hasta que una persona revise su cuenta. */
    public UserStatus initialStatus() {
        return this == CUSTOMER || isStaff() ? UserStatus.ACTIVE : UserStatus.PENDING_VERIFICATION;
    }
}
